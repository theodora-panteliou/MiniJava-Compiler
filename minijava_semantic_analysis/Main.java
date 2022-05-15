import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }


        FileInputStream fis = null;
        for (int i=0; i<args.length; i++) {
            System.out.println("***File: " + args[i]+"***");
            try{
                fis = new FileInputStream(args[i]);
                MiniJavaParser parser = new MiniJavaParser(fis);

                Goal root = parser.Goal();

                // System.err.println("Program parsed successfully.");

                FirstVisitor eval = new FirstVisitor();
                root.accept(eval, null);
                // System.err.println("FirstVisitor done.");

                SecondVisitor eval2 = new SecondVisitor(eval.symbolTable);
                root.accept(eval2, null);
                
                System.err.println("Program is semantically correct.");
                eval.offset.print(); /* print offsets */
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            catch(Exception ex){
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
            System.out.println();
        }
    }
}
