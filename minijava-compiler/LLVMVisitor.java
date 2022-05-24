import java.util.Iterator;
import java.util.List;

import syntaxtree.*;
import visitor.GJDepthFirst;

/* FisrtVisitor fills the symbol table (catches double decalaration errors, type mismatch for overriding) */

public class LLVMVisitor extends GJDepthFirst<String,String> {
    SymbolTable symbolTable = null;
    Offset offsets = null;
    private int reg_counter = 0;
    
    private String get_reg() {
        return "%_"+reg_counter++;
    }

    LLVMVisitor(SymbolTable st, Offset os){
        this.symbolTable = st;
        this.offsets = os;
        System.out.println(offsets.make_vtable(st));
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
        else {
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

        n.f1.accept(this, argu);
        n.f11.accept(this, argu);

        n.f14.accept(this, argu);
        n.f15.accept(this, argu);

        System.out.println("\n\tret i32 0\n}");
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
        String _ret=null;
        String ClassName = n.f1.accept(this, argu);

        n.f3.accept(this, argu);
        n.f4.accept(this, ClassName);
        // symbolTable.get()

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
        String _ret=null;

        String ClassName = n.f1.accept(this, argu);

        n.f3.accept(this, argu);

        n.f5.accept(this, argu);
        n.f6.accept(this, ClassName);

        return argu;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, String argu) throws Exception {
        String _ret=null;
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        // System.out.println("x " + type);
        System.out.println("%" + name + " = alloca " + get_ir_type(type));
        return argu;
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

        String ret = n.f10.accept(this, null); /* Expression should return the register or value. We know that the type is the correct one from type checking */
        System.out.println("ret " + RetType + " " + ret + "}\n");
        return null;
    }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
    public String visit(FormalParameterList n, String argu) throws Exception {
        
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        // System.out.println("\nFormalParameterList {\n"+argu+"}\n");
        return argu;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, String argu) throws Exception { // TODO: exclude fields
        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        System.out.println("\t%" + name + " = alloca " + get_ir_type(type));
        System.out.println("\tstore " + get_ir_type(type) + " %." + name + ", " + get_ir_type(type) + "* %" + name);
        // System.out.println("\nFormalParameter {\n"+argu+"}\n");
        return argu;
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
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, String argu) throws Exception {

        String res = n.f2.accept(this, argu); /* the result of the expression should be the register to which the value is loaded */
        System.out.println("call void (i32) @print_int(i32 " + res + ")"); /* always prints ints */
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

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, String argu) throws Exception {
        return n.f0.toString();
    }
}