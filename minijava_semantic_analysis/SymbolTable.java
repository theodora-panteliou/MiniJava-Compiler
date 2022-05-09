import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.Iterator;

public class SymbolTable {
    Map<String, ClassInfo> class_dec = new HashMap<String, ClassInfo>(); /* return ClassInfo by class name */
    Map<String, Map<String, VariableInfo>> field_in_class = new HashMap<String, Map<String, VariableInfo>>(); /* field(1) is declared in list of classes(2) */
    Map<String, Map<String, MethodInfo>> method_in_class = new HashMap<String, Map<String, MethodInfo>>(); /* method(1) is declared in list of classes(2) */
    Map<String, Map<String, Map<String, VariableInfo>>> var_in_method_in_class = new HashMap<String,Map<String,Map<String, VariableInfo>>>();; /* variable(1) is declared in method(2) */

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
        
        /* check for superclass override */
        /* repeat until you find a superclass with the same method name or null */
        MethodInfo curr_method_info = new MethodInfo(MethodName, ClassName, arg_names, arg_types, ReturnType);
        ClassInfo curr_class = class_dec.get(ClassName).getSuper();
        while (curr_class != null){
            // MethodInfo temp = curr_class.getMethod(MethodName);
            if (method_in_class.get(MethodName) == null ) break;
            MethodInfo temp =  method_in_class.get(MethodName).get(curr_class.name()); //TODO maybe dont keep methods in classinfo nd just get them this way
            if (temp != null){
                if (!curr_method_info.equals(temp)){
                    throw new Exception("Invalid method override in method <" + MethodName + "> in class <" + ClassName + ">. Previous definition was in class <" + curr_class.name() +">.");
                }
                break;
            }
            curr_class = curr_class.getSuper();
        }
        
        Map<String, MethodInfo> get_class_map = method_in_class.get(MethodName);
        if (get_class_map != null){
            MethodInfo method_info = get_class_map.get(ClassName);
            if (method_info != null) {
                throw new Exception("Method <" + MethodName + "> already declared in class <" + ClassName + ">");
            }
            else {
                get_class_map.put(ClassName, curr_method_info);
            }
        }
        else {
            get_class_map = new HashMap<String, MethodInfo>();
            get_class_map.put(ClassName, curr_method_info);
            method_in_class.put(MethodName, get_class_map);
        }

        /* now add all arguments in the variable_in_method_in_class */
        Iterator<String> names = arg_names.iterator();
        Iterator<String> types = arg_types.iterator();
        while (names.hasNext() && types.hasNext()) {
            this.addMethodVariable(names.next(), MethodName, ClassName, types.next());
        }

    }

    public void addMethodVariable(String VariableName, String MethodName, String ClassName, String Type) throws Exception { /* method should already exist in method_in_class map */
        VariableInfo to_insert = new VariableInfo(VariableName, Type, MethodName, ClassName);

        Map<String, MethodInfo> declared_methods_map_class = method_in_class.get(MethodName);
        MethodInfo method = declared_methods_map_class.get(ClassName);

        /* check if method doesn't exist in methods' map: THIS SHOULD NOT HAPPEN */
        if (method == null) {
            throw new Exception("Method <" + MethodName + "> at <" + ClassName + "> not declared when trying to insert <" + VariableName + "> into Symbol Table");
        }
        
        /* first search by VariableName in map */
        Map<String, Map<String, VariableInfo>> first= var_in_method_in_class.get(VariableName);
        if (first != null) {
            /* then by method */
            Map<String, VariableInfo> second = first.get(MethodName);
            if (second !=  null){
                /* at last by class */
                VariableInfo third = second.get(ClassName); //TODO: maybe we dont need MethodInfo here, replace with VariableInfo 
                if (third != null){
                    throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> in class <" + ClassName + ">");
                }
                else {
                    // /* check if the variable name appears as argument */
                    // if (method.hasArgName(VariableName)){ /* method from ethod_in_class map */
                    //     throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> as argument in class <" + ClassName + ">");
                    // }
                    second.put(ClassName, to_insert);
                    first.put(MethodName, second);
                    var_in_method_in_class.put(VariableName, first);
                }
            }
            else { 
                // /* check if the variable name appears as argument */
                // if (method.hasArgName(VariableName)){ /* method from ethod_in_class map */
                //     throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> as argument in class <" + ClassName + ">");
                // }
                /* create second and put */
                second = new HashMap<String, VariableInfo>();
                second.put(ClassName, to_insert);
                first.put(MethodName, second);
                var_in_method_in_class.put(VariableName, first);
            }
        }
        else {
            // /* check if the variable name appears as argument */
            // if (method.hasArgName(VariableName)){ /* method from ethod_in_class map */
            //     throw new Exception("Variable <" + VariableName + "> already declared in method <" + MethodName + "> as argument in class <" + ClassName + ">");
            // }
            /* create first and second and put */
            first = new HashMap<String, Map<String, VariableInfo>>();
            Map<String, VariableInfo> second = new HashMap<String, VariableInfo>();
            second.put(ClassName, to_insert);
            first.put(MethodName, second);
            var_in_method_in_class.put(VariableName, first);
        }
    
    }

    /* find methods */

    /* returns type */
    String find_variable_in_scope(String VariableName, String MethodName, String ClassName) throws Exception{
        /* variable in a scope (MethodName, ClassName) can be declared inside the Method, as argument 
        to the Method, as Field in Class or a Field in superclass */

        /* first search the nearest scope: variable or argument in a method */
        VariableInfo variable = var_in_method_in_class.get(ClassName).get(MethodName).get(VariableName);
        if (variable != null) return variable.getType();

        /* search at class or superclasses */
        /* get the map of classes that belongs to our variable name. In that map we will search for the closest superclass each time */
        Map<String, VariableInfo> map_of_classes = field_in_class.get(VariableName);
        VariableInfo temp;
        ClassInfo curr_class = class_dec.get(VariableName);
        while (curr_class != null){
            temp = map_of_classes.get(curr_class.name());
            
            if (temp!=null) {
                return temp.getType();
            }
            
            curr_class = curr_class.getSuper();
        }

        throw new Exception("find_variable_in_scope: Variable not found. Should never reach here.");
    }

    // String find_class_type(String ClassName){
    //     return class_dec.get(ClassName).nam
    // }

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

    ClassInfo(String name, ClassInfo superclass){
        this.name = name;
        this.superclass = superclass;
    }

    ClassInfo(String name){
        this.name = name;
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

    VariableInfo(String name, String type, String method, String classname){
        this.name = name;
        this.type = type;
        this.method = method;
        this.classname = classname;
    }

    VariableInfo(String name, String ClassName){ /* for comparisons only */
        this.name = name;
        this.classname = ClassName;
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
