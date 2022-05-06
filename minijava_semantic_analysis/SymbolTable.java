import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

public class SymbolTable {
    Map<String, String> inherits = new HashMap<String, String>(); /* class1 extends class2 */
    Map<String, List<String>> field_in_class = new HashMap<String, List<String>>(); /* field(1) is declared in list of classes(2) */
    Map<String, List<MethodInfo>> method_in_class = new HashMap<String, List<MethodInfo>>(); /* method(1) is declared in list of classes(2) */
    Map<String, Map<String, Map<String, MethodInfo>>> var_in_method_in_class = new HashMap<String,Map<String,Map<String, MethodInfo>>>();; /* variable(1) is declared in method(2) */

    public void addClassDeclaration(String ClassName) throws Exception { 
        if (inherits.containsKey(ClassName)){
            throw new Exception("Class "+ ClassName + "already defined");
        }
        else {
            inherits.put(ClassName, null); 
        }
    }

    public void addClassDeclaration(String ClassName, String SuperName) throws Exception { 
        /* SuperName can be null if class does not inherit from another class
         * If SuperName doesn't exist in class declarations at the time we call this function, it is invalid because the superclass must be declared first
         */
        if (SuperName == null){
            addClassDeclaration(ClassName);
        }
        else if (inherits.containsKey(ClassName)){
            throw new Exception("Class "+ ClassName + "already defined");
        }
        else if (!inherits.containsKey(SuperName)){
            throw new Exception("The superclass <"+ SuperName + "> that class <" + ClassName + "> inherits from is not defined");
        } 
        else {
            inherits.put(ClassName, SuperName);
        }
    }

    public void addClassField(String FieldName, String ClassName) throws Exception { /* */
        List<String> classes_list = field_in_class.get(FieldName);
        if (classes_list == null){
            classes_list = new LinkedList<String>();
        }
        else if (classes_list.contains(FieldName)){
            throw new Exception("Field <" + FieldName + "> already declared in class <" + ClassName + ">");
        }
        classes_list.add(ClassName);
        field_in_class.put(FieldName, classes_list);
    }

    public void addClassMethod(String MethodName, String ClassName, List<String> args, String ReturnType) throws Exception {
        List<MethodInfo> methodinfo_list = method_in_class.get(MethodName);
        
        /* create a method info object */
        MethodInfo new_method_info = new MethodInfo(MethodName, ClassName, args, ReturnType);
        
        if (methodinfo_list == null){
            methodinfo_list = new LinkedList<MethodInfo>();
        } 
        else if (methodinfo_list.contains(new_method_info)) {
            throw new Exception("Method <" + MethodName + "> already declared in class <" + ClassName + ">");
        }

        methodinfo_list.add(new_method_info);
        method_in_class.put(MethodName, methodinfo_list);

        /* TODO: Add checks for superclass override */
    }

    public void addMethodVariable(String VariableName, String MethodName, String ClassName) throws Exception { /* method should already exist in method_in_class map */
        // HashMap<String, MethodInfo> methodinfo_list = var_in_method_in_class.get(VariableName);
        
        List<MethodInfo> declared_methods_list = method_in_class.get(MethodName);

        /* check if method doesn't exist in methods' map: THIS SHOULD NOT HAPPEN */
        MethodInfo temp_method_info = new MethodInfo(MethodName, ClassName);
        if (!declared_methods_list.contains(temp_method_info)) {
            throw new Exception("Method <" + MethodName + "> at <" + ClassName + "> not declared when trying to insert <" + VariableName + "> into Symbol Table");
        }
        
        // /* get list */
        // if (methodinfo_list == null){
        //     methodinfo_list = new LinkedList<MethodInfo>();
        // } 
        // /* check if variable already exists */
        // else if (methodinfo_list.contains(temp_method_info)) {
        //     throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> in class <" + ClassName + ">");
        // }

        // /* TODO: check if variable exists in args */
        
        // methodinfo_list.add(temp_method_info);
        // method_in_class.put(MethodName, methodinfo_list);
        
        // MethodInfo temp_method_info = new MethodInfo(MethodName, ClassName);
        
        /* first search by VariableName in map */
        Map<String, Map<String, MethodInfo>> first= var_in_method_in_class.get(VariableName);
        if (first != null) {
            /* then by method */
            Map<String, MethodInfo> second = first.get(MethodName);
            if (second !=  null){
                /* at last by class */
                MethodInfo third = second.get(ClassName);
                if (third != null){
                    throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> in class <" + ClassName + ">");
                }
                else {
                    second.put(ClassName, temp_method_info);
                }
            }
            else { 
                /* create second and put */
                second = new HashMap<String, MethodInfo>();
                second.put(ClassName, temp_method_info);
                first.put(MethodName, second);
            }
        }
        else {
            /* create first and second and put */
            first = new HashMap<String, Map<String, MethodInfo>>();
            Map<String, MethodInfo> second = new HashMap<String, MethodInfo>();
            second.put(ClassName, temp_method_info);
            first.put(MethodName, second);
        }
    
    }

}

class MethodInfo { /* holds all information for the method */
    String ClassName;
    String MethodName;
    String ReturnType;
    List<String> args;

    MethodInfo(String MethodName, String ClassName, List<String> args, String ReturnType){ /* proper initialization */
        this.ClassName = ClassName;
        this.MethodName = MethodName;
        this.args = args;
        this.ReturnType = ReturnType;
    }

    MethodInfo(String MethodName, String ClassName){ /* for comparisons */
        this.ClassName = ClassName;
        this.MethodName = MethodName;
    }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;
        return MethodName.equals(that.MethodName) &&
        ClassName.equals(that.ClassName);
    }
}
