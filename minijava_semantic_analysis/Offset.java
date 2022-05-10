import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

public class Offset {

    LinkedHashMap<String, LinkedHashMap<String, Integer>> field_offsets = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
    LinkedHashMap<String, LinkedHashMap<String, Integer>> method_offsets = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();
    
    int next_field_offset = 0;
    int next_method_offset = 0;

    String curr_class = null;

    Map<String, String> inherit = new HashMap<String, String>();
    Map<String, Integer> last_field_offset = new HashMap<String, Integer>();  
    Map<String, Integer> last_method_offset = new HashMap<String, Integer>();  

    public void add_class(String ClassName, String SuperClassName){
        LinkedHashMap<String, Integer> fields_map = new LinkedHashMap<>();
        field_offsets.put(ClassName, fields_map);

        LinkedHashMap<String, Integer> methods_map = new LinkedHashMap<>();
        method_offsets.put(ClassName, methods_map);
        
        curr_class = ClassName;

        if (SuperClassName!=null) { /* Start offset after the previous class */
            System.out.println("in superclass");
            next_field_offset = last_field_offset.get(SuperClassName);
            next_method_offset = last_method_offset.get(SuperClassName);
            System.out.println("after superclass");
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
        String curr = inherit.get(name);
        while (curr!=null) {
            if (method_offsets.get(curr) != null) return;
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
            System.out.println("--Methods---");
            for (String method: method_offsets.get(classname).keySet()){
                System.out.println(classname + "." + method + " : " + method_offsets.get(classname).get(method));
            }
            System.out.println();
        }
    }

}
