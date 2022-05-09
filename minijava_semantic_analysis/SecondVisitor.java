import visitor.GJDepthFirst;
import syntaxtree.*;

public class SecondVisitor extends GJDepthFirst<String,Void>{
    SymbolTable symbolTable;
    String currClass = null;
    String currMethod = null;
    Boolean method_statements = false;

    SecondVisitor(SymbolTable st){
        this.symbolTable = st;
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
    public String visit(MainClass n, Void argu) throws Exception {

        currClass = n.f1.accept(this, argu);
        currMethod = n.f6.toString();
       
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);

        currMethod = null;
        currClass = null;
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
    public String visit(ClassDeclaration n, Void argu) throws Exception {
  
        currClass = n.f1.accept(this, argu);

        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        currClass = null;

        return null;
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
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {

        currClass = n.f1.accept(this, argu);

        n.f5.accept(this, argu);
        n.f6.accept(this, argu);

        currClass = null;
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
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        String ReturnType = n.f1.accept(this, argu);

        currMethod = n.f2.accept(this, argu);

        n.f4.accept(this, argu);

        n.f7.accept(this, argu);
        method_statements = true;
        n.f8.accept(this, argu);

        String ReturnExpr = n.f10.accept(this, argu);
        if (!ReturnExpr.equals(ReturnType)) {
            throw new Exception("Return expression doesn't match actual return type.");
        }

        method_statements = false;

        currMethod = null;
        return null;
    }

    
    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
    public String visit(Clause n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
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
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, Void argu) throws Exception { //TODO: maybe I can check the identifiers in higher levels
        /* check the scope. If we are inside a funtion below declarations we need to return the type */
        if (method_statements == true) {
            String type = symbolTable.find_type_in_scope(n.f0.toString(), currMethod, currClass);
            System.out.println("found type "+type+" for variable "+n.f0.toString()+ " in method " + currMethod + " in class " +currClass);
            if (type != null) return type;
        }
        return n.f0.toString();
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, Void argu) throws Exception {
        /* this refers to current object. It is used inside a function, inside a class so current class is its type */
        return currClass;
    }

    /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(BooleanArrayAllocationExpression n, Void argu) throws Exception {

        String index_type = n.f3.accept(this, argu);
        if (!index_type.equals("int"))
            throw new Exception("Array size not int.");

        return "boolean[]";
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(IntegerArrayAllocationExpression n, Void argu) throws Exception {
        
        String index_type = n.f3.accept(this, argu);
        if (!index_type.equals("int"))
            throw new Exception("Array size not int.");

        return "int[]";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n, Void argu) throws Exception {
        String type = n.f1.accept(this, argu); /* TODO check that identifier exists */
        return type;
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n, Void argu) throws Exception {
        return n.f1.accept(this, argu); /* type is f1's type */
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n, Void argu) throws Exception {
        
        String ex1 = n.f0.accept(this, argu);
        String ex2 = n.f2.accept(this, argu);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in < operator");

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n, Void argu) throws Exception {
        String ex1 = n.f0.accept(this, argu);
        String ex2 = n.f2.accept(this, argu);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in + operator");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n, Void argu) throws Exception {
        String ex1 = n.f0.accept(this, argu);
        String ex2 = n.f2.accept(this, argu);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in - operator");

        return "int";
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n, Void argu) throws Exception {
        String ex1 = n.f0.accept(this, argu);
        String ex2 = n.f2.accept(this, argu);

        if (!ex1.equals("int") || !ex2.equals("int")) 
            throw new Exception("Operands not int in * operator");

        return "int";
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n, Void argu) throws Exception {
        String ReturnType = n.f0.accept(this, argu);

        String array_index = n.f2.accept(this, argu);
        if (!array_index.equals("int"))
            throw new Exception("Array index not int.");

        if (ReturnType.equals("int[]"))
            return "int";
        else if (ReturnType.equals("boolean[]"))
            return "boolean";
        else 
            throw new Exception("Array Expression not array type.");
    }

    
    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n, Void argu) throws Exception {
        
        String type = n.f0.accept(this, argu);
        if (!type.equals("int[]") && !type.equals("boolean[]"))
            throw new Exception("Non array objects don't have .length member.");

        return "int";
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
    public String visit(Expression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n, Void argu) throws Exception {
        String type1 = n.f0.accept(this, argu);
        String type2 = n.f2.accept(this, argu);
        if (!type1.equals("boolean") || !type2.equals("boolean"))
            throw new Exception("Clauses in && operand not boolean.");
        return "boolean";
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n, Void argu) throws Exception {
        String type = n.f1.accept(this, argu);
        if (!type.equals("boolean"))
            throw new Exception("Clause in ! operand not boolean.");
        return "boolean";
    }

    
   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n, Void argu) throws Exception { //TODO: check classes with subclasses
        String id_type = n.f0.accept(this, argu);
        String expr_type = n.f2.accept(this, argu);

        if (!id_type.equals(expr_type))
            throw new Exception("Identifier type different from expression type in assignment statement.");

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
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception { //TODO: check classes with subclasses
        String id_type = n.f0.accept(this, argu);
        String index_type = n.f2.accept(this, argu);
        String expr_type = n.f5.accept(this, argu);
        if (!index_type.equals("int"))
            throw new Exception("Index type in array assignment statement is not int.");

        if ((id_type.equals("int[]") && expr_type.equals("int")) 
            || (id_type.equals("boolean[]") && expr_type.equals("boolean")))
            return null;
        else throw new Exception("Incompatible type variables in array assignmet expression.");

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
    public String visit(IfStatement n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expr_type = n.f2.accept(this, argu);
        if (!expr_type.equals("boolean"))
            throw new Exception("If condition is not boolean.");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String expr_type = n.f2.accept(this, argu);
        if (!expr_type.equals("boolean"))
            throw new Exception("While condition is not boolean.");
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        return null;
    }

    /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n, Void argu) throws Exception {

        String type = n.f2.accept(this, argu);
        if (!type.equals("int"))
            throw new Exception("Print expression only accepts int.");
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
    public String visit(MessageSend n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        String return_type = n.f2.accept(this, argu);
        System.out.println("Return type: "+ return_type);
        n.f3.accept(this, argu);
        n.f4.accept(this, argu);
        n.f5.accept(this, argu);
        return return_type;
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        return "int";
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n, Void argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n, Void argu) throws Exception {
        return "boolean";
    }

        /**
    * f0 -> "boolean"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(BooleanArrayType n, Void argu) throws Exception {
        return "boolean[]";
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(IntegerArrayType n, Void argu) throws Exception {
        return "int[]";
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n, Void argu) throws Exception {
        return n.f0.toString();
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n, Void argu) throws Exception {
        return n.f0.toString();
    }
}
