import syntaxtree.*;
import visitor.*;

// import java.io.FileInputStream;
// import java.io.FileNotFoundException;
// import java.io.IOException;

public class FirstVisitor extends GJDepthFirst<String,Void> {
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
        System.out.println("PrimaryExpression:"+n.f0.accept(this, argu));
        return n.f0.accept(this, argu);
    }

    /**
    * f0 -> "true"
    */
    @Override
    public String visit(TrueLiteral n, Void argu) throws Exception {
        return "boolean";
    }

    /**
     * f0 -> "false"
    */
    @Override
    public String visit(FalseLiteral n, Void argu) throws Exception {
        return "boolean";
    }
    
    /**
    * f0 -> <INTEGER_LITERAL>
    */
    @Override
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        // System.out.println("I am int: "+n.f0.toString());
        return "int";
    }

}
