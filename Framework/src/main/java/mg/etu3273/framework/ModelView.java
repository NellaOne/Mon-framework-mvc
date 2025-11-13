package mg.etu3273.framework;

public class ModelView {
    
    private String view;  
    
    public ModelView() {
    }
    
    public ModelView(String view) {
        this.view = view;
    }
    
    public String getView() {
        return view;
    }
    
    public void setView(String view) {
        this.view = view;
    }
    
    @Override
    public String toString() {
        return "ModelView{view='" + view + "'}";
    }
}