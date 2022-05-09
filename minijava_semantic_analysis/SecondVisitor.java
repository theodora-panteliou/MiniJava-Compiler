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

        currMethod = n.f2.accept(this, argu);

        n.f4.accept(this, argu);

        n.f7.accept(this, argu);
        method_statements = true;
        n.f8.accept(this, argu);
        method_statements = false;

        n.f10.accept(this, argu); //TODO check return type

        currMethod = null;
        return null;
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
    public String visit(Identifier n, Void argu) throws Exception {
        /* check the scope. If we are inside a funtion below declarations we need to return the type */
        if (method_statements == true) {
            return symbolTable.find_variable_in_scope(n.f0.toString(), currMethod, currClass);
        }
        else {
            return n.f0.toString();
        }
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }
    
    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        return "integer";
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
