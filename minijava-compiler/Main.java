import syntaxtree.*;
import visitor.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length < 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }


        FileInputStream fis = null;
        for (int i=0; i<args.length; i++) {
            System.out.println("\u001B[35m" + "***File: " + args[i]+"***" + "\u001B[0m");
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
                
                System.err.println("Program is semantically correct.\n");
                eval.offset.print(); /* print offsets */

                LLVMVisitor compile = new LLVMVisitor(eval.symbolTable, eval.offset);
                String output = root.accept(compile, null);

                /* save the output program to a .ll file 
                src: https://stackoverflow.com/questions/28947250/create-a-directory-if-it-does-not-exist-and-then-create-the-files-in-that-direct*/
                
                String directoryName = "./outputs";
                String fileName = args[i].substring(args[i].lastIndexOf('/')+1, args[i].lastIndexOf('.')) + ".ll";
                
                File directory = new File(directoryName);
                if (! directory.exists()){
                    directory.mkdir();
                }

                File file = new File(directoryName + "/" + fileName);
                try{
                    FileWriter fw = new FileWriter(file.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fw);
                    bw.write(output);
                    bw.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                    System.exit(-1);
                }

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

    public void writeFile(String name, String value){
        
    }
}
