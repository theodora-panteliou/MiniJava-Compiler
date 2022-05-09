import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;

public class SymbolTable {
    Map<String, ClassInfo> class_dec = new HashMap<String, ClassInfo>(); /* class1 extends class2 */
    Map<String, Map<String, VariableInfo>> field_in_class = new HashMap<String, Map<String, VariableInfo>>(); /* field(1) is declared in list of classes(2) */
    Map<String, Map<String, MethodInfo>> method_in_class = new HashMap<String, Map<String, MethodInfo>>(); /* method(1) is declared in list of classes(2) */
    Map<String, Map<String, Map<String, MethodInfo>>> var_in_method_in_class = new HashMap<String,Map<String,Map<String, MethodInfo>>>();; /* variable(1) is declared in method(2) */

    public void addClassDeclaration(String ClassName) throws Exception {
        if (class_dec.containsKey(ClassName)){
            throw new Exception("Class <"+ ClassName + "> already defined");
        }
        else {
            ClassInfo new_class = new ClassInfo(ClassName, null);
            class_dec.put(ClassName, new_class);
        }
    }

    public void addClassDeclaration(String ClassName, String SuperName) throws Exception { 
        /* SuperName can be null if class does not inherit from another class
         * If SuperName doesn't exist in class declarations at the time we call this function, it is invalid because the superclass must be declared first
         */
        if (SuperName == null){
            addClassDeclaration(ClassName);
        }
        else if (class_dec.containsKey(ClassName)){
            throw new Exception("Class <"+ ClassName + "> already defined");
        }
        else if (!class_dec.containsKey(SuperName)){
            throw new Exception("The superclass <"+ SuperName + "> that class <" + ClassName + "> inherits from is not defined");
        } 
        else {
            ClassInfo new_class = new ClassInfo(ClassName, class_dec.get(SuperName));
            class_dec.put(ClassName, new_class);
        }
    }

    public void addClassField(String FieldName, String ClassName, String Type) throws Exception { /* */
        Map<String, VariableInfo> classes_map = field_in_class.get(FieldName);
        if (classes_map == null){
            classes_map = new HashMap<String, VariableInfo>();
        }
        else if (classes_map.containsKey(ClassName)){
            throw new Exception("Field <" + FieldName + "> already declared in class <" + ClassName + ">");
        }
        VariableInfo new_var = new VariableInfo(FieldName, Type, null, ClassName);
        classes_map.put(ClassName, new_var);
        field_in_class.put(FieldName, classes_map);
    }

    public void addClassMethod(String MethodName, String ClassName, List<String> arg_names, List<String> arg_types, String ReturnType) throws Exception {

        Map<String, MethodInfo> get_class_map = method_in_class.get(MethodName);
        if (get_class_map != null){
            MethodInfo method_info = get_class_map.get(ClassName);
            if (method_info != null) {
                throw new Exception("Method <" + MethodName + "> already declared in class <" + ClassName + ">");
            }
            else {
                method_info = new MethodInfo(MethodName, ClassName, arg_names, arg_types, ReturnType);
                get_class_map.put(ClassName, method_info);
            }
        }
        else {
            get_class_map = new HashMap<String, MethodInfo>();
            MethodInfo method_info = new MethodInfo(MethodName, ClassName, arg_names, arg_types, ReturnType);
            get_class_map.put(ClassName, method_info);
            method_in_class.put(MethodName, get_class_map);
        }

        /* check for superclass override */
        /* repeat until you find a superclass with the same method name or null */
        MethodInfo method_info = new MethodInfo(MethodName, ClassName, arg_names, arg_types, ReturnType);
        ClassInfo curr_class = class_dec.get(ClassName).getSuper();
        while (curr_class != null){
            // MethodInfo temp = curr_class.getMethod(MethodName);
            if (method_in_class.get(MethodName) == null ) break;
            MethodInfo temp =  method_in_class.get(MethodName).get(curr_class.name()); //TODO maybe dont keep methods in classinfo nd just get them this way
            if (temp != null){
                if (!method_info.equals(temp)){
                    throw new Exception("Invalid method override in method <" + MethodName + "> in class <" + ClassName + ">. Previous definition was in class <" + curr_class.name() +">.");
                }
                break;
            }
            curr_class = curr_class.getSuper();
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
                MethodInfo third = second.get(ClassName); //TODO: maybe we dont need MethodInfo here, replace with String 
                if (third != null){
                    throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> in class <" + ClassName + ">");
                }
                else {
                    /* check if the variable name appears as argument */
                    if (method.hasArgName(VariableName)){ /* method from ethod_in_class map */
                        throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> as argument in class <" + ClassName + ">");
                    }
                    second.put(ClassName, method);
                    first.put(MethodName, second);
                    var_in_method_in_class.put(VariableName, first);
                    System.out.println("here3 "+ VariableName + " " +ClassName+" "+ MethodName);
                }
            }
            else { 
                /* check if the variable name appears as argument */
                if (method.hasArgName(VariableName)){ /* method from ethod_in_class map */
                    throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> as argument in class <" + ClassName + ">");
                }
                /* create second and put */
                second = new HashMap<String, MethodInfo>();
                second.put(ClassName, method);
                first.put(MethodName, second);
                var_in_method_in_class.put(VariableName, first);
                System.out.println("here2 "+ VariableName + " " +ClassName+" "+ MethodName);
            }
        }
        else {
            /* check if the variable name appears as argument */
            if (method.hasArgName(VariableName)){ /* method from ethod_in_class map */
                throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> as argument in class <" + ClassName + ">");
            }
            /* create first and second and put */
            first = new HashMap<String, Map<String, MethodInfo>>();
            Map<String, MethodInfo> second = new HashMap<String, MethodInfo>();
            second.put(ClassName, method);
            first.put(MethodName, second);
            var_in_method_in_class.put(VariableName, first);
            System.out.println("here " + VariableName + " " + ClassName + " " + MethodName);
        }
    
    }

}

class MethodInfo { /* holds all information for the method */
    String ClassName;
    String MethodName;
    String ReturnType;
    List<String> arg_names;
    List<String> arg_types;

    MethodInfo(String MethodName, String ClassName, List<String> arg_names, List<String> arg_types, String ReturnType){ /* proper initialization */
        this.ClassName = ClassName;
        this.MethodName = MethodName;
        this.arg_names = new LinkedList<>(arg_names); /* copy */
        this.arg_types = new LinkedList<>(arg_types); /* copy */
        this.ReturnType = ReturnType;
    }

    // MethodInfo(String MethodName, String ClassName){ /* for comparisons */
    //     this.ClassName = ClassName;
    //     this.MethodName = MethodName;
    // }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInfo that = (MethodInfo) o;

        return MethodName.equals(that.MethodName) &&
        ReturnType.equals(that.ReturnType) &&
        arg_types.equals(that.arg_types); /* 2 methods are considered equals based on types */
    }

    public boolean hasArgName(String name) {
        return arg_names.contains(name);
    }
}

class ClassInfo {
    String name;
    ClassInfo superclass;
    Map<String, MethodInfo> methods = new HashMap<String, MethodInfo>();

    ClassInfo(String name){
        this.name = name;
    }

    ClassInfo(String name, ClassInfo superclass){
        this.name = name;
        this.superclass = superclass;
    }

    public void addMethod(String name,MethodInfo method){
        methods.put(name, method);
    }

    public MethodInfo getMethod(String name){
        return methods.get(name);
    }
    
    public ClassInfo getSuper(){
        return superclass;
    }

    public String name(){
        return name;
    }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassInfo that = (ClassInfo) o;
        
        return name.equals(that.name);
    }
}

class VariableInfo {
    String name;
    String type;
    String method;
    String classname;

    VariableInfo(String name, String ClassName){ /* for comparisons only */
        this.name = name;
        this.classname = ClassName;
    }

    VariableInfo(String name, String type, String method, String classname){
        this.name = name;
        this.type = type;
        this.method = method;
        this.classname = classname;
    }

    public String getType(){
        return type;
    }

    public String getMethod(){
        return method;
    }

    public String getClassname(){
        return method;
    }

    @Override
    public boolean equals(Object o) {
        /* source: https://www.baeldung.com/java-comparing-objects */
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableInfo that = (VariableInfo) o;
        
        return name.equals(that.name) && classname.equals(that.classname);
    }
}
