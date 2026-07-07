package in.virit.entityexplorer.filter;

/**
 * A node in a filter tree: either a {@link FilterGroup} combining child nodes
 * with AND/OR logic, or a single {@link FilterCondition} leaf.
 * <p>
 * The model is plain data with no UI dependencies, so it can be built
 * programmatically, unit tested against a real {@code EntityManager} and
 * potentially serialized.
 */
public sealed interface FilterNode permits FilterGroup, FilterCondition {

    /**
     * @return a deep copy of this node. Used to snapshot the filter tree when
     * it is applied, so that further edits in the builder UI don't mutate the
     * active filter between grid page fetches.
     */
    FilterNode copy();
}
