package org.ruyisdk.packages;

import org.eclipse.jface.viewers.LabelProvider;

import javax.json.JsonValue;
import java.util.Map;

public class PackageLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
        if (element instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) element;
            return entry.getKey() + ": " + getJsonValueText((JsonValue) entry.getValue());
        } else if (element instanceof JsonValue) {
            return getJsonValueText((JsonValue) element);
        }
        return super.getText(element);
    }

    private String getJsonValueText(JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT:
                return "{...}";
            case ARRAY:
                return "[...]";
            case STRING:
                return value.toString().replace("\"", ""); 
            case NUMBER:
                return value.toString();
            case TRUE:
            case FALSE:
                return value.toString();
            case NULL:
                return "null";
            default:
                return value.toString();
        }
    }
}
