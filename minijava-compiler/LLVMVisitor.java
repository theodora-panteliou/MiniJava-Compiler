import syntaxtree.*;
import visitor.GJDepthFirst;

/* FisrtVisitor fills the symbol table (catches double decalaration errors, type mismatch for overriding) */

public class LLVMVisitor extends GJDepthFirst<String,String> {
    SymbolTable symbolTable = null;
    Offset offsets = null;
    String vtable_string = "";
    int reg_counter = 0;

    LLVMVisitor(SymbolTable st, Offset os){
        this.symbolTable = st;
        this.offsets = os;
        vtable_string = offsets.make_vtable(st);
    }

    /**
    * f0 -> MainClass()
    * f1 -> ( TypeDeclaration() )*
    * f2 -> <EOF>
    */
    public String visit(Goal n, String argu) throws Exception {
        String _ret =
        """
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
        """;

        
        String main = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return vtable_string + _ret + main;
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

        String _ret="\ndefine i32 @main() {\n";

        n.f1.accept(this, argu);
        n.f11.accept(this, argu);

        n.f14.accept(this, argu);
        n.f15.accept(this, argu);

        _ret += "\n\tret i32 0\n}";
        return _ret;
     }
}