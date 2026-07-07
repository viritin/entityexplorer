package in.virit.entityexplorer.filter;

/**
 * A single filter condition leaf: property, operator and (for most operators)
 * a value, optionally negated. Instances are mutable because the builder UI
 * edits them in place; {@link #copy()} snapshots the state on apply.
 */
public final class FilterCondition implements FilterNode {

    private FilterProperty property; // null until the user picks one
    private FilterOperator operator;
    private Object value; // typed value from the UI field (String, Integer, BigDecimal, LocalDate, enum constant, ...)
    private boolean negated;

    public FilterProperty getProperty() {
        return property;
    }

    public void setProperty(FilterProperty property) {
        this.property = property;
    }

    public FilterOperator getOperator() {
        return operator;
    }

    public void setOperator(FilterOperator operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    /**
     * @return true when the condition can be turned into a predicate: property
     * and operator are set, and a value is present if the operator needs one.
     * Incomplete conditions are silently skipped by the predicate builder.
     */
    public boolean isComplete() {
        return property != null && operator != null
                && (!operator.requiresValue() || value != null);
    }

    @Override
    public FilterCondition copy() {
        FilterCondition copy = new FilterCondition();
        copy.property = property;
        copy.operator = operator;
        copy.value = value;
        copy.negated = negated;
        return copy;
    }
}
