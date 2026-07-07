package in.virit.entityexplorer.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A group of {@link FilterNode}s combined with AND or OR. Groups can be nested
 * arbitrarily and optionally negated (NOT).
 */
public final class FilterGroup implements FilterNode {

    public enum Logic {
        AND, OR
    }

    private Logic logic = Logic.AND;
    private boolean negated;
    private final List<FilterNode> children = new ArrayList<>();

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.logic = logic;
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    public List<FilterNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void add(FilterNode node) {
        children.add(node);
    }

    public void remove(FilterNode node) {
        children.remove(node);
    }

    public void clear() {
        children.clear();
    }

    /**
     * @return the number of complete (appliable) conditions in this group and
     * all nested groups. Drives e.g. the "Filters (n)" indicator in the UI.
     */
    public int conditionCount() {
        int count = 0;
        for (FilterNode child : children) {
            count += switch (child) {
                case FilterGroup group -> group.conditionCount();
                case FilterCondition condition -> condition.isComplete() ? 1 : 0;
            };
        }
        return count;
    }

    /**
     * @return true if this group contains no complete conditions, not even in
     * nested groups. An empty group produces no predicate at all.
     */
    public boolean isEmpty() {
        return conditionCount() == 0;
    }

    @Override
    public FilterGroup copy() {
        FilterGroup copy = new FilterGroup();
        copy.logic = logic;
        copy.negated = negated;
        for (FilterNode child : children) {
            copy.add(child.copy());
        }
        return copy;
    }
}
