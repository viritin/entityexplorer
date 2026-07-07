package in.virit.entityexplorer;

import com.vaadin.flow.component.icon.VaadinIcon;
import in.virit.entityexplorer.filter.FilterGroup;
import in.virit.entityexplorer.filter.FilterPredicateBuilder;
import in.virit.entityexplorer.filter.FilterProperty;
import in.virit.entityexplorer.filter.ui.FilterBuilderDialog;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;

/**
 * The filtering toolbar of a {@link JpaEntityGrid}: the free-form JPQL quick
 * filter and a button opening the visual {@link FilterBuilderDialog}. The two
 * filtering mechanisms coexist; the last applied one wins (applying either
 * replaces the grid's data provider wholesale).
 */
public class FilterToolbar<T> extends VHorizontalLayout {

    private final JpaEntityGrid<T> grid;
    private final JpaEntityGrid.FilterInput filterInput;
    private final FilterBuilderButton filterBuilderButton = new FilterBuilderButton();

    public FilterToolbar(JpaEntityGrid<T> grid) {
        this.grid = grid;
        this.filterInput = new JpaEntityGrid.FilterInput(grid);
        // typing a JPQL filter overrides the visual one -> reset its badge
        filterInput.addValueChangeListener(e -> {
            if (!e.getValue().isEmpty()) {
                filterBuilderButton.updateBadge(0);
            }
        });
        setDefaultVerticalComponentAlignment(Alignment.BASELINE);
        addAndExpand(filterInput);
        add(filterBuilderButton);
    }

    private void applyVisualFilter(FilterGroup snapshot) {
        // Clear the JPQL field first: its value-change listener re-lists
        // unfiltered, and the visual filter is applied after that, so the
        // ordering keeps "last applied wins" true without suppressing events.
        filterInput.setValue("");
        grid.filter((root, cb) -> FilterPredicateBuilder.toPredicate(snapshot, root, cb));
        filterBuilderButton.updateBadge(snapshot.conditionCount());
    }

    private void clearVisualFilter() {
        grid.filter("");
        filterBuilderButton.updateBadge(0);
    }

    /**
     * Opens the (lazily created, then reused) filter builder dialog. Reusing
     * one dialog instance keeps the filter tree editable across openings.
     */
    private class FilterBuilderButton extends VButton {

        private FilterBuilderDialog dialog;

        FilterBuilderButton() {
            setIcon(VaadinIcon.FILTER.create());
            updateBadge(0);
            setTooltipText("Build a filter visually from entity properties");
            addClickListener(() -> {
                if (dialog == null) {
                    dialog = new FilterBuilderDialog(
                            grid.getEntityType().getName(),
                            FilterProperty.listFor(grid.getEntityType()),
                            FilterToolbar.this::applyVisualFilter,
                            FilterToolbar.this::clearVisualFilter);
                }
                dialog.open();
            });
        }

        void updateBadge(int appliedConditionCount) {
            setText(appliedConditionCount > 0 ? "Filters (" + appliedConditionCount + ")" : "Filters");
        }
    }
}
