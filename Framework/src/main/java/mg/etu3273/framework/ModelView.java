package mg.etu3273.framework;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    
    private String view; 
    private Map<String, Object> data; 
    
    public ModelView() {
        this.data = new HashMap<>();
    }
    
    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }
    
    public void addObject(String key, Object value) {
        this.data.put(key, value);
    }

    public Map<String, Object> getData() {
        return data;
    }
    
    public String getView() {
        return view;
    }
    
    public void setView(String view) {
        this.view = view;
    }
    
   @Override
    public String toString() {
        return "ModelView{view='" + view + "', data=" + data.size() + " élément(s)}";
    }
}