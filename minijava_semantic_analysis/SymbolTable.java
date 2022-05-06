import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

public class SymbolTable {
    Map<String, String> inherits = new HashMap<String, String>(); /* class1 extends class2 */
    Map<String, List<String>> field_in_class = new HashMap<String, List<String>>(); /* field(1) is declared in list of classes(2) */
    Map<String, Map<String, MethodInfo>> method_in_class = new HashMap<String, Map<String, MethodInfo>>(); /* method(1) is declared in list of classes(2) */
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

        /* TODO: Add checks for superclass override */

        Map<String, MethodInfo> get_class_map = method_in_class.get(MethodName);
        if (get_class_map != null){
            MethodInfo method_info = get_class_map.get(ClassName);
            if (method_info != null) {
                throw new Exception("Method <" + MethodName + "> already declared in class <" + ClassName + ">");
            }
            else {
                method_info = new MethodInfo(MethodName, ClassName, args, ReturnType);
                get_class_map.put(ClassName, method_info);
            }
        }
        else {
            get_class_map = new HashMap<String, MethodInfo>();
            MethodInfo method_info = new MethodInfo(MethodName, ClassName, args, ReturnType);
            get_class_map.put(ClassName, method_info);
            method_in_class.put(MethodName, get_class_map);
        }
    }

    public void addMethodVariable(String VariableName, String MethodName, String ClassName) throws Exception { /* method should already exist in method_in_class map */
        
        Map<String, MethodInfo> declared_methods_map_class = method_in_class.get(MethodName);
        MethodInfo method = declared_methods_map_class.get(ClassName);

        /* check if method doesn't exist in methods' map: THIS SHOULD NOT HAPPEN */
        if (method == null) {
            throw new Exception("Method <" + MethodName + "> at <" + ClassName + "> not declared when trying to insert <" + VariableName + "> into Symbol Table");
        }
        
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
                    second.put(ClassName, method);
                }
            }
            else { 
                /* create second and put */
                second = new HashMap<String, MethodInfo>();
                second.put(ClassName, method);
                first.put(MethodName, second);
            }
        }
        else {
            /* create first and second and put */
            first = new HashMap<String, Map<String, MethodInfo>>();
            Map<String, MethodInfo> second = new HashMap<String, MethodInfo>();
            second.put(ClassName, method);
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