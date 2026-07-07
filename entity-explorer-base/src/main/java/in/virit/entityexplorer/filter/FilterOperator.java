package in.virit.entityexplorer.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Operators available in filter conditions. Which operators apply to a given
 * property is decided by {@link #forProperty(FilterProperty)} based on the
 * property's java type and nullability.
 */
public enum FilterOperator {

    EQUALS("="),
    NOT_EQUALS("!="),
    CONTAINS("contains"),
    STARTS_WITH("starts with"),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    IS_TRUE("is true"),
    IS_FALSE("is false"),
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),
    /** String only: null or empty string. */
    IS_EMPTY("is empty");

    private final String caption;

    FilterOperator(String caption) {
        this.caption = caption;
    }

    public String getCaption() {
        return caption;
    }

    /**
     * @return false for operators that carry their semantics without a
     * user-provided value (is null, is true, ...).
     */
    public boolean requiresValue() {
        return switch (this) {
            case IS_TRUE, IS_FALSE, IS_NULL, IS_NOT_NULL, IS_EMPTY -> false;
            default -> true;
        };
    }

    /**
     * @return the operators applicable to the given property, in the order
     * they should be offered in the UI.
     */
    public static List<FilterOperator> forProperty(FilterProperty property) {
        Class<?> t = property.javaType();
        List<FilterOperator> operators = new ArrayList<>();
        if (t == boolean.class || t == Boolean.class) {
            operators.add(IS_TRUE);
            operators.add(IS_FALSE);
        } else if (t == String.class) {
            operators.add(EQUALS);
            operators.add(NOT_EQUALS);
            operators.add(CONTAINS);
            operators.add(STARTS_WITH);
        } else if (t.isEnum()) {
            operators.add(EQUALS);
            operators.add(NOT_EQUALS);
        } else {
            // numeric and temporal types
            operators.add(EQUALS);
            operators.add(NOT_EQUALS);
            operators.add(LT);
            operators.add(LTE);
            operators.add(GT);
            operators.add(GTE);
        }
        if (property.nullable()) {
            operators.add(IS_NULL);
            operators.add(IS_NOT_NULL);
            if (t == String.class) {
                operators.add(IS_EMPTY);
            }
        }
        return operators;
    }
}
