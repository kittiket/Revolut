package ev.demo.revolut.db.jdbc;

import java.util.Arrays;
import java.util.List;

public class WhereItem {
    private String fieldName;
    private List<Object> orParameters;

    public WhereItem(String fieldName, Object... orParameters) {
        this.fieldName = fieldName;
        this.orParameters = Arrays.asList(orParameters);
    }
    String getFieldName() {
        return fieldName;
    }

    List<Object> getOrParameters() {
        return orParameters;
    }
}
