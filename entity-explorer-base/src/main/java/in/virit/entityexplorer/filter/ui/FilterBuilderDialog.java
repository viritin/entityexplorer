package in.virit.entityexplorer.filter.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import in.virit.entityexplorer.filter.FilterCondition;
import in.virit.entityexplorer.filter.FilterGroup;
import in.virit.entityexplorer.filter.FilterOperator;
import in.virit.entityexplorer.filter.FilterProperty;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.checkbox.VCheckBox;
import org.vaadin.firitin.components.combobox.VComboBox;
import org.vaadin.firitin.components.dialog.VDialog;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.select.VSelect;

import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for visually building a filter for one entity type. The dialog owns
 * a live {@link FilterGroup} tree that survives closing and reopening; Apply
 * hands out a deep copy so later edits don't mutate the active filter.
 */
public class FilterBuilderDialog extends VDialog {

    private final List<FilterProperty> properties;
    private final Consumer<FilterGroup> onApply;
    private final Runnable onClear;

    private final FilterGroup rootGroup = new FilterGroup();
    private final VVerticalLayout groupSlot = new VVerticalLayout();

    public FilterBuilderDialog(String entityName, List<FilterProperty> properties,
            Consumer<FilterGroup> onApply, Runnable onClear) {
        this.properties = properties;
        this.onApply = onApply;
        this.onClear = onClear;

        setHeaderTitle("Filter " + entityName);
        setMinWidth("650px");

        groupSlot.setPadding(false);
        groupSlot.add(new GroupEditor(rootGroup));
        add(groupSlot);

        getFooter().add(new ApplyButton(), new ClearButton(), new CloseButton());
    }

    private class ApplyButton extends VButton {
        ApplyButton() {
            super("Apply");
            addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addClickListener(() -> {
                onApply.accept(rootGroup.copy());
                close();
            });
        }
    }

    private class ClearButton extends VButton {
        ClearButton() {
            super("Clear");
            setTooltipText("Remove all conditions and show unfiltered listing");
            addClickListener(() -> {
                rootGroup.clear();
                groupSlot.removeAll();
                groupSlot.add(new GroupEditor(rootGroup));
                onClear.run();
            });
        }
    }

    private class CloseButton extends VButton {
        CloseButton() {
            super("Close");
            addClickListener(() -> close());
        }
    }

    /** Negation toggle used both on groups and on individual conditions. */
    private static class NotCheckbox extends VCheckBox {
        NotCheckbox(boolean initialValue, Consumer<Boolean> onChange) {
            super("NOT");
            setValue(initialValue);
            addValueChangeListener(e -> onChange.accept(e.getValue()));
        }
    }

    /**
     * Editor for one {@link FilterGroup}: AND/OR logic, NOT toggle, condition
     * rows, arbitrarily nested child groups and buttons to add more of each.
     */
    class GroupEditor extends VVerticalLayout {

        private final FilterGroup group;
        private final VVerticalLayout rows = new VVerticalLayout();

        GroupEditor(FilterGroup group) {
            this(group, null);
        }

        GroupEditor(FilterGroup group, GroupEditor parent) {
            this.group = group;
            addClassName("filter-group");
            setPadding(false);
            setSpacing(false);
            if (parent != null) {
                // visual nesting for child groups
                getStyle().setBorderLeft("2px solid var(--lumo-contrast-20pct)");
                getStyle().setPaddingLeft("var(--lumo-space-m)");
                getStyle().setMarginTop("var(--lumo-space-s)");
            }
            add(new GroupHeader(parent));
            rows.setPadding(false);
            rows.setSpacing(false);
            // render pre-existing tree (dialog reuse / reopen)
            group.getChildren().forEach(child -> {
                switch (child) {
                    case FilterCondition condition -> rows.add(new ConditionRow(condition));
                    case FilterGroup nested -> rows.add(new GroupEditor(nested, this));
                }
            });
            add(rows, new VHorizontalLayout(new AddConditionButton(), new AddGroupButton()));
        }

        private class GroupHeader extends VHorizontalLayout {
            GroupHeader(GroupEditor parent) {
                setDefaultVerticalComponentAlignment(Alignment.BASELINE);
                var logicSelect = new VSelect<FilterGroup.Logic>("Combine");
                logicSelect.setItems(FilterGroup.Logic.values());
                logicSelect.setValue(group.getLogic());
                logicSelect.addValueChangeListener(e -> group.setLogic(e.getValue()));
                logicSelect.setWidth("6em");
                add(logicSelect, new NotCheckbox(group.isNegated(), group::setNegated));
                if (parent != null) {
                    add(new VButton(VaadinIcon.TRASH.create(), e -> {
                        parent.group.remove(group);
                        GroupEditor.this.removeFromParent();
                    }) {
                        {
                            addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                            setTooltipText("Remove group");
                        }
                    });
                }
            }
        }

        private class AddConditionButton extends VButton {
            AddConditionButton() {
                super("Add condition");
                setIcon(VaadinIcon.PLUS.create());
                addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                addClickListener(() -> {
                    FilterCondition condition = new FilterCondition();
                    group.add(condition);
                    rows.add(new ConditionRow(condition));
                });
            }
        }

        private class AddGroupButton extends VButton {
            AddGroupButton() {
                super("Add group");
                setIcon(VaadinIcon.PLUS_CIRCLE.create());
                addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                setTooltipText("Add a nested AND/OR group");
                addClickListener(() -> {
                    FilterGroup nested = new FilterGroup();
                    group.add(nested);
                    rows.add(new GroupEditor(nested, GroupEditor.this));
                });
            }
        }

        /**
         * One condition: property + operator + (optional) value + remove.
         * Writes directly into its {@link FilterCondition}.
         */
        class ConditionRow extends VHorizontalLayout {

            private final FilterCondition condition;
            private final VComboBox<FilterProperty> propertySelect = new VComboBox<>("Property");
            private final VSelect<FilterOperator> operatorSelect = new VSelect<>("Operator");
            private Component valueField;

            ConditionRow(FilterCondition condition) {
                this.condition = condition;
                setDefaultVerticalComponentAlignment(Alignment.BASELINE);
                setWidthFull();

                propertySelect.setItems(properties);
                propertySelect.setItemLabelGenerator(FilterProperty::label);
                propertySelect.setValue(condition.getProperty());
                propertySelect.addValueChangeListener(e -> propertyChanged(e.getValue()));

                operatorSelect.setItemLabelGenerator(FilterOperator::getCaption);
                if (condition.getProperty() != null) {
                    operatorSelect.setItems(FilterOperator.forProperty(condition.getProperty()));
                    operatorSelect.setValue(condition.getOperator());
                }
                operatorSelect.addValueChangeListener(e -> operatorChanged(e.getValue()));

                add(propertySelect, operatorSelect,
                        new NotCheckbox(condition.isNegated(), condition::setNegated),
                        new RemoveButton());
                updateValueField();
            }

            private void propertyChanged(FilterProperty property) {
                condition.setProperty(property);
                condition.setValue(null);
                removeValueField(); // type changed, the old field no longer fits
                List<FilterOperator> operators = property == null
                        ? List.of() : FilterOperator.forProperty(property);
                operatorSelect.setItems(operators);
                // preselect the first operator; usually what the user wants
                condition.setOperator(operators.isEmpty() ? null : operators.get(0));
                operatorSelect.setValue(condition.getOperator());
                updateValueField();
            }

            private void operatorChanged(FilterOperator operator) {
                condition.setOperator(operator);
                updateValueField();
            }

            private void updateValueField() {
                boolean needsValue = condition.getProperty() != null
                        && condition.getOperator() != null
                        && condition.getOperator().requiresValue();
                if (!needsValue) {
                    removeValueField();
                } else if (valueField == null) {
                    // switching between two value-requiring operators keeps
                    // the existing field (and the value the user typed)
                    valueField = ConditionValueFields.createFor(condition.getProperty(), condition::setValue);
                    if (valueField != null) {
                        addComponentAtIndex(indexOf(operatorSelect) + 1, valueField);
                    }
                }
            }

            private void removeValueField() {
                if (valueField != null) {
                    remove(valueField);
                    valueField = null;
                }
            }

            private class RemoveButton extends VButton {
                RemoveButton() {
                    setIcon(VaadinIcon.TRASH.create());
                    addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                    setTooltipText("Remove condition");
                    addClickListener(() -> {
                        group.remove(condition);
                        ConditionRow.this.removeFromParent();
                    });
                }
            }
        }
    }
}
