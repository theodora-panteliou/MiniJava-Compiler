import syntaxtree.*;
import visitor.*;
import java.util.List;
import java.util.LinkedList;
// import java.io.FileInputStream;
// import java.io.FileNotFoundException;
// import java.io.IOException;

/* FisrtVisitor fills the symbol table (catches double decalaration errors, type mismatch for overriding) */

public class FirstVisitor extends GJDepthFirst<String,Void> {
    SymbolTable symbolTable = new SymbolTable();
    String currClass = null;
    String currMethod = null;
    List<String> helperListArgs = new LinkedList<String>();
    List<String> helperListTypes = new LinkedList<String>();
    
    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n, Void argu) throws Exception {
  
        n.f0.accept(this, argu);
        String className = n.f1.accept(this, argu);
        currClass = className;
        symbolTable.addClassDeclaration(className);

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

        String className = n.f1.accept(this, argu);
        currClass = className;
        String superName = n.f3.accept(this, argu);
        symbolTable.addClassDeclaration(className, superName);

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

        String retType = n.f1.accept(this, argu);
        String methodName = n.f2.accept(this, argu);
        currMethod = methodName;

        n.f4.accept(this, argu);
        
        symbolTable.addClassMethod(methodName, currClass, helperListArgs, helperListTypes, retType);
        helperListArgs.clear();
        helperListTypes.clear();

        n.f7.accept(this, argu);
        n.f8.accept(this, argu);

        n.f10.accept(this, argu);
        currMethod = null;
        return null;
    }
   
    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n, Void argu) throws Exception {
        helperListTypes.add(n.f0.accept(this, argu));
        helperListArgs.add(n.f1.accept(this, argu));
        return null;
     }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n, Void argu) throws Exception {

        String type = n.f0.accept(this, argu);
        String name = n.f1.accept(this, argu);
        
        if (currClass==null) {
            throw new Exception("Variable <" + name + "> defined out of class scope");
        }
        if (currMethod==null){
            symbolTable.addClassField(name, currClass, type);
        }
        else {
            symbolTable.addMethodVariable(name, currMethod, currClass);
        }

        return null;
    }
 
    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n, Void argu) throws Exception {
        // System.out.println("Identifier returns: " + n.f0.toString());
        return n.f0.toString();
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
    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        // System.out.println("PrimaryExpression:"+n.f0.accept(this, argu));
        return n.f0.accept(this, argu);
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

    // /**
    // * f0 -> "true"
    // */
    // @Override
    // public String visit(TrueLiteral n, Void argu) throws Exception {
    //     return "boolean";
    // }

    // /**
    //  * f0 -> "false"
    // */
    // @Override
    // public String visit(FalseLiteral n, Void argu) throws Exception {
    //     return "boolean";
    // }
    
    // /**
    // * f0 -> <INTEGER_LITERAL>
    // */
    // @Override
    // public String visit(IntegerLiteral n, Void argu) throws Exception {
    //     // System.out.println("I am int: "+n.f0.toString());
    //     return "int";
    // }

}
