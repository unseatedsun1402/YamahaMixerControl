package MidiControl.ContextModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ViewRegistry {
    private Map<String, ViewBuilder> viewMap = new HashMap<>();

    public Map<String,ViewBuilder> getViews(){
        return this.viewMap;
    }

    public Optional<ViewBuilder> getView(String key){
        return Optional.ofNullable(this.viewMap.get(key));
    }

    public ViewBuilder addView(ViewBuilder view, String key){
        if (viewMap.isEmpty()){return viewMap.put(key, view);}
        return this.viewMap.putIfAbsent(key, view);
    }
}
