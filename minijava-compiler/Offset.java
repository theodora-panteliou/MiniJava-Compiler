import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class Offset {

    LinkedHashMap<String, LinkedHashMap<String, Integer>> field_offsets = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
    LinkedHashMap<String, LinkedHashMap<String, Integer>> method_offsets = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
    
    int next_field_offset = 0;
    int next_method_offset = 0;

    String curr_class = null;
    String main_class = null;

    Map<String, String> inherit = new HashMap<String, String>();
    Map<String, Integer> last_field_offset = new HashMap<String, Integer>();  
    Map<String, Integer> last_method_offset = new HashMap<String, Integer>();  

    Map<String, String> vtable_classes = new HashMap<String, String>(); /* for allocation expressions */

    public void add_main(String name){
        main_class = name;
    }

    public void add_class(String ClassName, String SuperClassName){
        LinkedHashMap<String, Integer> fields_map = new LinkedHashMap<>();
        field_offsets.put(ClassName, fields_map);

        LinkedHashMap<String, Integer> methods_map = new LinkedHashMap<>();
        method_offsets.put(ClassName, methods_map);
        
        curr_class = ClassName;

        if (SuperClassName!=null && !SuperClassName.equals(main_class)) { /* Start offset after the previous class */
            next_field_offset = last_field_offset.get(SuperClassName);
            next_method_offset = last_method_offset.get(SuperClassName);
            inherit.put(ClassName, SuperClassName);
        }
        else {
            next_field_offset = 0;
            next_method_offset = 0;
        }
        last_field_offset.put(curr_class, next_field_offset);
        last_method_offset.put(curr_class, next_method_offset);
    }

    public void add_field(String name, String type){
        field_offsets.get(curr_class).put(name, next_field_offset);
        int offs;
        if (type.equals("int"))
            offs = 4;
        else if (type.equals("boolean"))
            offs = 1;
        else
            offs = 8;

        next_field_offset += offs;
        last_field_offset.put(curr_class, next_field_offset);
    }

    public void add_method(String name){
        /* check if method exists in any superclass */
        String curr = curr_class;
        while (curr!=null) {
            if (method_offsets.get(curr) != null && method_offsets.get(curr).get(name) != null){ method_offsets.get(curr_class).put(name, method_offsets.get(curr).get(name)); return;}
            curr = inherit.get(curr);
        } 

        method_offsets.get(curr_class).put(name, next_method_offset);
        next_method_offset += 8;
        last_method_offset.put(curr_class, next_method_offset);
    }

    public void print(){
        for (String classname: field_offsets.keySet()){
            System.out.println("-----------Class " + classname + "-----------");
            System.out.println("--Variables---");
            for (String field: field_offsets.get(classname).keySet()){
                System.out.println(classname + "." + field + " : " + field_offsets.get(classname).get(field));
            }
            System.out.println("---Methods---");
            for (String method: method_offsets.get(classname).keySet()){
                System.out.println(classname + "." + method + " : " + method_offsets.get(classname).get(method));
            }
            System.out.println();
        }
    }

    public String make_vtable(SymbolTable st) {
        String vtable = "";
        /* add main */
        vtable += "@."+ main_class + "_vtable = global [0 x i8*] []\n";
        for (String classname: method_offsets.keySet()){
            int size = 0;
            String curr = classname;
            String methods_str = "";
            Map<String, Integer> allmethods = new HashMap<>(); /* keeps all methods that will end up in vtable */
            Map<String, String> method_to_class = new HashMap<>(); /* keeps in which class the method belongs */
            while (curr!=null){ /* search superclasses and gather all methods from derived to superclass. If a method is implemented in derived it won't be added for superclasses */
                for (Map.Entry<String, Integer> method: method_offsets.get(curr).entrySet()){
                    if (!allmethods.containsKey(method.getKey())){
                        allmethods.put(method.getKey(), method.getValue()); 
                        method_to_class.put(method.getKey(), curr);
                    }
                }
                curr = inherit.get(curr);
            }

            /* sort by offset and make vtable */
            List<Entry<String, Integer>> sorted = allmethods.entrySet().stream().sorted(Map.Entry.comparingByValue()).toList();
            for (Entry<String, Integer> pair: sorted){
                String method = pair.getKey();
                MethodInfo arglist = st.return_method_info(method, method_to_class.get(method));
                String ret_type = get_ir_type(arglist.getReturnType());
                
                String arg_types = "";
                Iterator<String> args = arglist.getArgTypes().iterator();
                while (args.hasNext()) {
                    arg_types += "," + get_ir_type(args.next());
                }
                methods_str +=  "i8* bitcast (" + ret_type + " (i8*" + arg_types + ")* @" + method_to_class.get(method) + "." + method + " to i8*), ";
            }

            size = sorted.size();
            vtable += "@."+ classname + "_vtable = global [" + size + " x i8*] [";
            if (methods_str.length() < 2){
                vtable += "]\n";
            }
            else {
                vtable += methods_str.substring(0, methods_str.length() - 2) + "]\n";
            }

            /* keep the information needed for new so we don't need to calculate it every time */
            vtable_classes.put(classname, "getelementptr [" + size + " x i8*], [" + size + " x i8*]* @."+ classname + "_vtable, i32 0, i32 0");
        }

        return vtable;
    }

    public int find_method_offset(String ClassName, String MethodName) {
        /* check if method exists in any superclass */
        String curr = ClassName;
        while (curr!=null) {
            if (method_offsets.get(curr) != null && method_offsets.get(curr).get(MethodName) != null){ return method_offsets.get(curr).get(MethodName);}
            curr = inherit.get(curr);
        }
        return -1;
    }

    public int find_field_offset(String ClassName, String FieldName) {
        /* check if variable exists in any superclass */
        String curr = ClassName;
        while (curr!=null) {
            if (field_offsets.get(curr) != null && field_offsets.get(curr).get(FieldName) != null){ return field_offsets.get(curr).get(FieldName);}
            curr = inherit.get(curr);
        }
        return -1;
    }

    public String get_allocation_str(String ClassName) {
        return vtable_classes.get(ClassName);
    }

    private String get_ir_type(String arg){
        String type;
        if (arg.equals("int")){
            type = "i32";
        }
        else if (arg.equals("boolean")){
            type = "i1";
        }
        else if (arg.equals("int[]")){
            type = "i32*";
        }
        else if (arg.equals("boolean[]")){
            type = "i8*";
        }
        else {
            type = "i8*";
        }
        return type;
    }

}
