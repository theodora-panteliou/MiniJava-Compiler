import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import syntaxtree.*;
import visitor.GJDepthFirst;

/* LLVMVisitors generates LLVM IR */

public class LLVMVisitor extends GJDepthFirst<String,String> {
    SymbolTable symbolTable = null;
    Offset offsets = null;

    String currClass = null; /* for scope */
    String currMethod = null; /* for scope */
    String currReg = null;
    String array_type = null; /* keeps int[] or boolean[] to cast types in ArrayLookup and ArrayAssignment */

    ArrayList<List<String>> expression_list = new ArrayList<List<String>>(); /* expression_list works as a stack. It keeps the lists of expression lists for ExpressionList to allow nested ExpressionLists */

    String currType = null;

    /* Counters for registers and labels */
    private int reg_counter = 0;
    private int if_label_counter = 0;
    private int loop_label_counter = 0;
    private int arr_label_counter = 0;
    private int oob_label_counter = 0;
    private int and_label_counter = 0;

    private String get_reg() {
        currReg = "%_"+reg_counter++;
        return currReg;
    }

    private void reset_reg() {
        reg_counter=0;
    }

    private String get_if_label() {
        return "if" + if_label_counter++ ;
    }
    
    private String get_loop_label() {
        return "loop" + loop_label_counter++ ;
    }

    private String get_arr_label() {
        return "arr_alloc" + arr_label_counter++ ;
    }

    private String get_oob_label() {
        return "oob" + oob_label_counter++ ;
    }

    private String get_and_label() {
        return "andclause" + and_label_counter++ ;
    }

    LLVMVisitor(SymbolTable st, Offset os){
        this.symbolTable = st;
        this.offsets = os;
        System.out.println(offsets.make_vtable(st)); /* print vtable */
    }

    private String get_ir_type(String arg){
        String type;
        if (arg.equals("int")){
            type = "i32";
        }
        else if (arg.equals("boolean")){
            type = "i1";
        }
        else if (arg.equals("int[]")){
            type = "i32*";
        }
        else if (arg.equals("boolean[]")){
            type = "i8*";
        }
        else { /* class object */
            type = "i8*";
        }
        return type;
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, String argu) throws Exception {
        System.out.println("""
        \n\n
        declare i8* @calloc(i32, i32)
        declare i32 @printf(i8*, ...)
        declare void @exit(i32)
        
        @_cint = constant [4 x i8] c\"%d\\0a\\00\"
        @_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"
        define void @print_int(i32 %i) {
            %_str = bitcast [4 x i8]* @_cint to i8*
            call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
            ret void
        }
        
        define void @throw_oob() {
            %_str = bitcast [15 x i8]* @_cOOB to i8*
            call i32 (i8*, ...) @printf(i8* %_str)
            call void @exit(i32 1)
            ret void
        }
        """);

        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String visit(MainClass n, String argu) throws Exception {

        System.out.println("\ndefine i32 @main() {\n");

        currClass = n.f1.accept(this, null);
        currMethod = n.f6.toString();
        n.f11.accept(this, argu);

        n.f14.accept(this, argu);
        n.f15.accept(this, argu);

        System.out.println("\n\tret i32 0\n}\n");
        currMethod = null;
        currClass = null;
        reset_reg();
        return null;
     }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String ClassName = n.f1.accept(this, argu);
        currClass = ClassName;

        n.f3.accept(this, argu);
        n.f4.accept(this, ClassName);

        currClass = null;

        return argu;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String ClassName = n.f1.accept(this, argu);
        currClass = ClassName;

        n.f3.accept(this, argu);

        n.f5.accept(this, argu);
        n.f6.accept(this, ClassName);
        
        currClass = null;
        return argu;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, String argu) throws Exception {
        if (currMethod!=null && currClass!=null) { /* if varDeclaration is inside method allocate space in stack */
            String type = n.f0.accept(this, argu);
            String name = n.f1.accept(this, argu);
            System.out.println("\t%" + name + " = alloca " + get_ir_type(type));
        }
        return null;
    }
    
    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String visit(MethodDeclaration n, String ClassName) throws Exception {

        n.f1.accept(this, null);
        String MethodName = n.f2.accept(this, null);
        currMethod = MethodName;
        
        MethodInfo arglist = symbolTable.return_method_info(MethodName, ClassName);
        String RetType = get_ir_type(arglist.getReturnType());
        
        String args = "";
        Iterator<String> arg_names = arglist.getArgNames().iterator();
        Iterator<String> arg_types = arglist.getArgTypes().iterator();
        while (arg_names.hasNext() && arg_types.hasNext()) {
            args +=  ", " + get_ir_type(arg_types.next()) + " %." + arg_names.next();
        }
        System.out.println("define " + RetType + " @" + ClassName + "." + MethodName + "(i8* %this" + args + ") {");

        n.f4.accept(this, null);

        n.f7.accept(this, null);
        n.f8.accept(this, null);

        String ret = n.f10.accept(this, null); /* ret expr: we need type and last register, type is function's return type so we need to return only the register */

        System.out.println("\tret " + RetType + " " + ret + "\n}\n");
        currMethod = null;
        reset_reg();
        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public String visit(FormalParameterList n, String argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return argu;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String argu) throws Exception {
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        System.out.println("\t%" + name + " = alloca " + get_ir_type(type));
        System.out.println("\tstore " + get_ir_type(type) + " %." + name + ", " + get_ir_type(type) + "* %" + name);
        return null;
    }

    /**
     * f0 -> ( FormalParameterTerm() )*
    */
    public String visit(FormalParameterTail n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, String argu) throws Exception {
        String calc_expr = get_loop_label();
        String loopstart = get_loop_label();
        String out = get_loop_label();

        System.out.println("\tbr label %" + calc_expr);
        System.out.println(calc_expr + ":");
        String expr_reg = n.f2.accept(this, argu);
        System.out.println("\tbr i1 " + expr_reg + ", label %" + loopstart + ", label %" + out);
        
        System.out.println(loopstart + ":");
        n.f4.accept(this, argu);
        System.out.println("\tbr label %" + calc_expr);

        System.out.println(out + ":");

        return null;
    }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String argu) throws Exception {
        String res = n.f2.accept(this, argu); /* the result of the expression should be the register to which the value is loaded */
        System.out.println("\tcall void (i32) @print_int(i32 " + res + ")\n"); /* always prints ints */
        return null;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n, String argu) throws Exception {
        String res_reg = n.f0.accept(this, "Expression");
        String type = currType;
        String method_name = n.f2.accept(this, null);

        int offset = offsets.find_method_offset(type, method_name)/8;
        System.out.println("\t; " + type + "." + method_name + " : " + offset);
        
        String prev_reg, new_reg;
        new_reg = get_reg();
        System.out.println("\t" + new_reg + " = bitcast i8* " + res_reg + " to i8***");
        System.out.println("\t" + get_reg() + " = load i8**, i8*** " + new_reg);

        String primary_res = currReg;
        String method_ptr = get_reg();
        System.out.println("\t" + method_ptr + " = getelementptr i8*, i8** " + primary_res + ", i32 " + offset);

        new_reg = get_reg();
        System.out.println("\t" + new_reg + " = load i8*, i8** " + method_ptr);

        MethodInfo methodInfo = symbolTable.return_method_info(method_name, type);

        prev_reg = new_reg;
        new_reg = get_reg();
        /* make method call */
        String ret_type = get_ir_type(methodInfo.getReturnType());
                    
        String arg_types = "";
        Iterator<String> args = methodInfo.getArgTypes().iterator();
        while (args.hasNext()) {
            arg_types += "," + get_ir_type(args.next());
        }
        
        System.out.println("\t" + new_reg + " = bitcast i8* " + prev_reg + " to " + ret_type + " (i8*" + arg_types + ")*");

        n.f4.accept(this, null);

        /* expression_list works as a stack */
        List<String> curr_parameters = null;
        if (expression_list.size()-1 <0){ /* if expression list is empty pass an empty list to find_method_type */
            curr_parameters = new LinkedList<String>();
        }
        else { /* else pop the last expression list */
            curr_parameters = expression_list.remove(expression_list.size()-1);
        }

        String reg = get_reg();
        String str = "\t" + reg + " = call " + ret_type + " " + new_reg + "(i8* " + res_reg;
        Iterator<String> it = curr_parameters.iterator();
        args = methodInfo.getArgTypes().iterator();
        while (it.hasNext() && args.hasNext()){
            str += "," + get_ir_type(args.next()) +  " " + it.next();
        }
        System.out.println(str + ")");
        currType = methodInfo.getReturnType();
        array_type = methodInfo.getReturnType();
        return reg;
    }

     /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n, String argu) throws Exception {
        List<String> new_list = new ArrayList<>();
    
        String res =n.f0.accept(this, null);

        /* initialize new level of expression list */
        new_list.add(res);
        expression_list.add(new_list);
        
        n.f1.accept(this, null);
        return null;
    }

    /**
     * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n, String argu) throws Exception {
         /* add to last level */
        List<String> insert_list = expression_list.get(expression_list.size()-1);
        
        String res =n.f1.accept(this, null);

        insert_list.add(res);

        return null;
    }

    /**
    * f0 -> AndExpression()
    *       | CompareExpression()
    *       | PlusExpression()
    *       | MinusExpression()
    *       | TimesExpression()
    *       | ArrayLookup()
    *       | ArrayLength()
    *       | MessageSend()
    *       | Clause()
    */
    public String visit(Expression n, String argu) throws Exception { 
        return n.f0.accept(this, "Expression");
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, String argu) throws Exception {
        String expr_reg = n.f2.accept(this, null);
        String reg = n.f0.accept(this, "lvalue"); /* get reg if id is field or name if id is variable */
        if (reg.startsWith("%")==false) {
            reg = "%" + reg;
        }
        String name = n.f0.accept(this, null); /* get name */

        String type = get_ir_type(symbolTable.find_type_in_scope(name, currMethod, currClass));

        System.out.println("\tstore " + type + " " + expr_reg + ", " + type + "* " + reg);
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n, String argu) throws Exception {
        String _ret=null;

        String array = n.f0.accept(this, "lvalue");
        String type = array_type;
        String index = n.f2.accept(this, argu);

        String labelok = get_oob_label();
        String labeloob = get_oob_label();
        String labelout = get_oob_label();
        String new_reg, prev_reg;
        new_reg = get_reg();

        if (type.equals("int[]")){
            /* compare size of array with index */
            System.out.println("\t" + new_reg + " = load i32*, i32** " + array);
            prev_reg = new_reg;
            array = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = load i32, i32 *" + prev_reg);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = icmp ult i32 " + index + ", " + prev_reg);
            System.out.println("\tbr i1 " + new_reg + ", label %" + labelok + ", label %" + labeloob );

            /* if size is in bounds */
            System.out.println(labelok+":");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = add i32 " + index + ", 1");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = getelementptr i32, i32* " + array + ", i32 " + prev_reg);
            
            /* assign */
            String value = n.f5.accept(this, argu);
            System.out.println("\tstore i32 " + value + ", i32* " + new_reg);
        }
        else if (type.equals("boolean[]")){
            /* compare size of array with index */
            System.out.println("\t" + new_reg + " = load i8*, i8** " + array);
            array = new_reg;

            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = bitcast i8* " + prev_reg + " to i32*");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = load i32, i32 *" + prev_reg);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = icmp ult i32 " + index + ", " + prev_reg);
            System.out.println("\tbr i1 " + new_reg + ", label %" + labelok + ", label %" + labeloob );

            /* if size is in bounds */
            System.out.println(labelok+":");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = add i32 " + index + ", 4");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = getelementptr i8, i8* " + array + ", i32 " + prev_reg);

            String value = n.f5.accept(this, argu);

            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = zext i1 " + value + " to i8");
            System.out.println("\tstore i8 " + new_reg + ", i8* " + prev_reg);
        }
        // /* assign */
        // String value = n.f5.accept(this, argu);
        // if (type.equals("boolean[]")) {
        //     prev_reg = new_reg;
        //     new_reg = get_reg();
        //     System.out.println("\t" + new_reg + " zext i1 " + value + " to i32");
        //     System.out.println("\tstore i32 " + new_reg + ", i32* " + prev_reg);
        // }
        // else {
        //     System.out.println("\tstore i32 " + value + ", i32* " + new_reg);
        // }

        System.out.println("\tbr label %" + labelout);

        /* if size is out of bounds */
        System.out.println(labeloob+":");
        System.out.println("\tcall void @throw_oob()\n\tbr label %" + labelout);

        /* out */
        System.out.println(labelout+":");

        return _ret;
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, String argu) throws Exception {
        String labelstart = get_and_label();
        String label1 = get_and_label();
        String label2 = get_and_label();
        String labelend = get_and_label();

        String clause1 = n.f0.accept(this, argu);
        System.out.println("\tbr label %" + labelstart);

        System.out.println(labelstart + ":");
        System.out.println("\tbr i1 " + clause1 + ", label %" + label1 + ", label %" + labelend);

        System.out.println(label1 + ":");
        String clause2 = n.f2.accept(this, argu);
        System.out.println("\tbr label %" + label2);
        
        System.out.println(label2 + ":");
        System.out.println("\tbr label %" + labelend);

        System.out.println(labelend + ":");
        String reg = get_reg();
        System.out.println("\t" + reg + " = phi i1 [ 0, %" + labelstart + " ], [ " + clause2 + ", %" + label2 + " ]");
        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, String argu) throws Exception {
        String reg1 = n.f0.accept(this, argu);
        String reg2 = n.f2.accept(this, argu);
        String reg = get_reg();
        System.out.println("\t" + reg + " = icmp slt i32 "+reg1+", " + reg2);
        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, String argu) throws Exception {
        String reg1 = n.f0.accept(this, argu);
        String reg2 = n.f2.accept(this, argu);
        String reg = get_reg();
        System.out.println("\t" + reg + " = add i32 "+reg1+", " + reg2);
        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, String argu) throws Exception {
        String reg1 = n.f0.accept(this, argu);
        String reg2 = n.f2.accept(this, argu);
        String reg = get_reg();
        System.out.println("\t" + reg + " = sub i32 "+reg1+", " + reg2);
        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, String argu) throws Exception {
        String reg1 = n.f0.accept(this, argu);
        String reg2 = n.f2.accept(this, argu);
        String reg = get_reg();
        System.out.println("\t" + reg + " = mul i32 "+reg1+", " + reg2);
        return reg;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, String argu) throws Exception {
        String array = n.f0.accept(this, argu);
        String type = array_type;
        String index = n.f2.accept(this, argu);

        String labelok = get_oob_label();
        String labeloob = get_oob_label();
        String labelout = get_oob_label();
        String new_reg, prev_reg;
        new_reg = get_reg();

        if (type.equals("int[]")){
            /* compare size of array with index */
            System.out.println("\t" + new_reg + " = load i32, i32 *" + array);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = icmp ult i32 " + index + ", " + prev_reg);
            System.out.println("\tbr i1 " + new_reg + ", label %" + labelok + ", label %" + labeloob );
    
            /* if size is in bounds */
            System.out.println(labelok+":");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = add i32 " + index + ", 1");

            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = getelementptr i32, i32* " + array + ", i32 " + prev_reg);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = load i32, i32* " + prev_reg);
        }
        else if (type.equals("boolean[]")){
            System.out.println("\t" + new_reg + " = bitcast i8* " + array + " to i32*");
            prev_reg = new_reg;
            new_reg = get_reg();
            /* compare size of array with index */
            System.out.println("\t" + new_reg + " = load i32, i32 *" + prev_reg);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = icmp ult i32 " + index + ", " + prev_reg);
            System.out.println("\tbr i1 " + new_reg + ", label %" + labelok + ", label %" + labeloob );
    
            /* if size is in bounds */
            System.out.println(labelok+":");
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = add i32 " + index + ", 4");

            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = getelementptr i8, i8* " + array + ", i32 " + prev_reg);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = load i8, i8* " + prev_reg);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = trunc i8 " + prev_reg + " to i1");
        }

        System.out.println("\tbr label %" + labelout);

        /* if size is out of bounds */
        System.out.println(labeloob+":");
        System.out.println("\tcall void @throw_oob()\n\tbr label %" + labelout);

        /* out */
        System.out.println(labelout+":");
        return new_reg;
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, String argu) throws Exception {
        String expr = n.f0.accept(this, argu);
        String new_reg = get_reg();
        if (array_type.equals("int[]"))
            System.out.println("\t" + new_reg + " = load i32, i32 *" + expr);
        else if (array_type.equals("boolean[]")){
            System.out.println("\t" + new_reg + " = bitcast i8 *" + expr + " to i32*");
            String prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" + new_reg + " = load i32, i32 *" + prev_reg);
        }
        return new_reg;
    }

    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

     /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, String argu) throws Exception {
        return "1";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, String argu) throws Exception {
        return "0";
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, String argu) throws Exception {
        String name = n.f0.toString();
        currType = symbolTable.find_type_in_scope(name, currMethod, currClass);
        array_type = currType;
        if (argu==null){
            return name;
        }
        if (argu.equals("Expression") && currClass!=null && currMethod!=null && symbolTable.is_field(name, currMethod, currClass)==true){
           
                int offset = offsets.find_field_offset(currClass, name)+8;
                String type = get_ir_type(symbolTable.find_type_in_scope(name, currMethod, currClass));
    
                String new_reg, prev_reg;
                new_reg = get_reg();
                System.out.println("\t" +new_reg + " = getelementptr i8, i8* %this, i32 " + offset);
                prev_reg = new_reg;
                new_reg = get_reg();
                System.out.println("\t" +new_reg + " = bitcast i8* " + prev_reg + " to " + type + "*");
                prev_reg = new_reg;
                new_reg = get_reg();
                System.out.println("\t" +new_reg + " = load " + type + ", " + type + "* " + prev_reg);
                return new_reg;
        }
        else if (argu.equals("Expression")){
            String type = get_ir_type(symbolTable.find_type_in_scope(name, currMethod, currClass));
            String reg = get_reg();
            System.out.println("\t" + reg + " = load " + type + ", " + type + "* %"+name);
            return reg;
            
        }
        else if (argu.equals("lvalue") && currClass!=null && currMethod!=null && symbolTable.is_field(name, currMethod, currClass)==true ){
            int offset = offsets.find_field_offset(currClass, name)+8;

            String new_reg, prev_reg;
            new_reg = get_reg();
            String type = get_ir_type(symbolTable.find_type_in_scope(name, currMethod, currClass));
            System.out.println("\t" +new_reg + " = getelementptr i8, i8* %this, i32 " + offset);
            prev_reg = new_reg;
            new_reg = get_reg();
            System.out.println("\t" +new_reg + " = bitcast i8* " + prev_reg + " to " + type + "*");
            return new_reg;
        }
        else if (argu.equals("lvalue")){
            return "%" + name;
        } 
        else {
            return name;
        }
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, String argu) throws Exception {
        /* this refers to current object. It is used inside String function, inside String class so current class is its type */
        currType = currClass;
        return "%this";
 
    }

    /**
     * f0 -> BooleanArrayAllocationExpression()
    *       | IntegerArrayAllocationExpression()
    */
    public String visit(ArrayAllocationExpression n, String argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(BooleanArrayAllocationExpression n, String argu) throws Exception {
        String size = n.f3.accept(this, argu);
        String new_reg, prev_reg;
        new_reg = get_reg();
        /* check if size of array is negative */
        System.out.println("\t" + new_reg + " = icmp slt i32 " + size + ", 0");
        String labeloob = get_arr_label();
        String labelcont = get_arr_label();
        
        /* if it is negative throw oob */
        System.out.println("\t" + "br i1 " + new_reg + ", label %" + labeloob + ", label %" + labelcont);
        System.out.println(labeloob + ":");
        System.out.println("\t" + "call void @throw_oob()");
        System.out.println("\t" + "br label %" + labelcont);

        /* If not oob */
        System.out.println(labelcont + ":");
        new_reg = get_reg();
        System.out.println("\t" + new_reg + " = add i32 " + size + ", 4"); /* size of array is size+1 so that in the first position we insert the size for oob checking */
        prev_reg= new_reg;
        new_reg = get_reg();
        String to_ret = new_reg;
        System.out.println("\t" + new_reg + " = call i8* @calloc(i32 1, i32 " + prev_reg + ")"); /* calloc size+1 */
        prev_reg= new_reg;
        new_reg = get_reg();

        /* store the size */
        System.out.println("\t" + new_reg + " = bitcast i8* "+ prev_reg+ " to i32*"); 
        System.out.println("\tstore i32 " + size + ", i32* " + new_reg);
        array_type = "boolean[]";
        return to_ret;
    }

    /**
     * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(IntegerArrayAllocationExpression n, String argu) throws Exception {
        String size = n.f3.accept(this, argu);
        String new_reg, prev_reg;
        new_reg = get_reg();
        /* check if size of array is negative */
        System.out.println("\t" + new_reg + " = icmp slt i32 " + size + ", 0");
        String labeloob = get_arr_label();
        String labelcont = get_arr_label();
        
        /* if it is negative throw oob */
        System.out.println("\t" + "br i1 " + new_reg + ", label %" + labeloob + ", label %" + labelcont);
        System.out.println(labeloob + ":");
        System.out.println("\t" + "call void @throw_oob()");
        System.out.println("\t" + "br label %" + labelcont);

        /* If not oob */
        System.out.println(labelcont + ":");
        new_reg = get_reg();
        System.out.println("\t" + new_reg + " = add i32 " + size + ", 1"); /* size of array is size+1 so that in the first position we insert the size for oob checking */
        prev_reg= new_reg;
        new_reg = get_reg();
        System.out.println("\t" + new_reg + " = call i8* @calloc(i32 4, i32 " + prev_reg + ")"); /* calloc size+1 */
        prev_reg= new_reg;
        new_reg = get_reg();

        /* store the size */
        System.out.println("\t" + new_reg + " = bitcast i8* "+ prev_reg+ " to i32*"); 
        System.out.println("\tstore i32 " + size + ", i32* " + new_reg);
        
        array_type = "int[]";
        return new_reg;
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, String argu) throws Exception {
        String id_name = n.f1.accept(this, null);

        String new_reg = get_reg();
        String calloc_reg = new_reg;

        System.out.println("\t" + new_reg + " = call i8* @calloc(i32 1, i32 " + (offsets.last_field_offset.get(id_name) + 8) + ")");
        String prev_reg = new_reg;
        new_reg = get_reg();
        System.out.println("\t" +new_reg + " = bitcast i8* " + prev_reg + " to i8***");
        prev_reg = new_reg;
        new_reg = get_reg();
        System.out.println("\t" +new_reg + " = " + offsets.get_allocation_str(id_name));
        System.out.println("\t" +"store i8** " + new_reg + ", i8*** " + prev_reg);
        currType = id_name;
        return calloc_reg;

    }

    /**
     * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, String argu) throws Exception {
        String reg = get_reg();
        String clause = n.f1.accept(this, argu);
        System.out.println("\t" + reg + " = xor i1 1, " + clause);
        return reg;
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public String visit(IfStatement n, String argu) throws Exception {

        String expr_reg = n.f2.accept(this, null);
        String then = get_if_label();
        String elsel = get_if_label();
        String out = get_if_label();
        System.out.println("\tbr i1 " + expr_reg + ", label %"+then+", label %" + elsel);
        
        System.out.println(then+":");
        n.f4.accept(this, null);
        System.out.println("\tbr label %" + out);

        System.out.println(elsel+":");
        n.f6.accept(this, null);
        System.out.println("\tbr label %" + out);

        System.out.println(out+":");
        return null;
    }

    /**
     * f0 -> "boolean"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(BooleanArrayType n, String argu) throws Exception {
        return "boolean[]";
    }

    /**
     * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(IntegerArrayType n, String argu) throws Exception {
        return "int[]";
    }

    /**
     * f0 -> "boolean"
    */
    public String visit(BooleanType n, String argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n, String argu) throws Exception {
        return n.f0.toString();
    }
}