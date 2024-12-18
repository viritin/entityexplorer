package in.virit.entityexplorer;

import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.dom.Style;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.grid.GridSelect;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.popover.PopoverButton;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.rad.PrettyPrinter;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.vaadin.firitin.util.style.LumoProps;

public class JpaEntityGrid<T> extends GridSelect<T> implements EntityManagerAwareComponent {

    private EntityType entityType;

    public JpaEntityGrid(EntityType<?> entityType) {
        this(entityType, null);
    }

    public JpaEntityGrid(EntityType<?> entityType, EntityManager entityManager) {
        super((Class<T>) entityType.getJavaType(), false);
        if (entityManager != null) {
            // If explicit entitymanager is not used, one will be create from the context
            ComponentUtil.setData(this, EntityManager.class, entityManager);
        }
        this.entityType = entityType;
        addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        addComponentColumn(entity -> new VHorizontalLayout(
                new QuickEditButton(entity),
                new DeleteEntityButton(entity),
                new PrettyPrintButton(entity)
        )
                .withSpacing(false))
                .setFlexGrow(0)
                .setAutoWidth(true)
                .setKey("actions")
                .setHeader("Actions");

        for (BeanPropertyDefinition bpf : getBeanPropertyDefinitions()) {
            Attribute<?, ?> attribute = entityType.getAttribute(bpf.getName());
            Column column;
            if (!attribute.isAssociation()) {
                if (attribute.getJavaType() == String.class) {
                    column = addColumn(b -> {
                        try {
                            String str = "" + bpf.getGetter().callOn(b);
                            // cut long strings. Less data and vaadin don't support max width for grid cols (and viritin's solution seem to bug some times)
                            if (str.length() > 40) {
                                str = str.substring(0, 40) + "...";
                            }
                            return str;
                        } catch (Exception ex) {
                            Logger.getLogger(JpaEntityGrid.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        return "??";
                    });
                    column.setKey(bpf.getName());
                } else {
                    // Let Vaadin figure out the best renderer
                    column = addColumn(bpf.getName());
                }
                if (bpf.getName().equals("id") || bpf.getName().equals("lastUpdate")) {
                    column.setVisible(false);
                }
            } else {
                column = addComponentColumn(entity -> {
                    Object associationValue = bpf.getGetter().getValue(entity);
                    return new AssociationColumn(associationValue);
                });
                column.setKey(attribute.getName());
            }

            column.setHeader(new ColumnHeader(attribute));
            column.setResizable(true);
            column.setAutoWidth(true);
            column.setSortable(false);
        }
        withColumnSelector();

        listEntities(entityType);

    }

    class ColumnHeader extends Div {

        ColumnHeader(Attribute attr) {
            String name = attr.getName();
            var pt = attr.getPersistentAttributeType();
            var javaSimpleType = attr.getJavaType().getSimpleName();
            add(new Div(name));
            add(new Div(pt + ":" + javaSimpleType) {
                {
                    getStyle().setColor(LumoProps.CONTRAST_50PCT.var());
                    getStyle().setFontSize(LumoProps.FONT_SIZE_XXS.var());
                }
            });
        }
    }

    EntityType getEntityType() {
        return entityType;
    }

    private void listEntities(EntityType<?> entityType) {
        String jpql = getBaseJpqlQuery();
        listEntities(jpql);
    }

    private String getBaseJpqlQuery() {
        String jpql = "select e from " + entityType.getName() + " e";
        return jpql;
    }

    private void listEntities(String jpql) {
        setItems(query -> {
            // TODO consider supporting sorting & filtering
            return getEntityManager().createQuery(jpql)
                    .setFirstResult(query.getOffset())
                    .setMaxResults(query.getLimit())
                    .getResultList().stream();
        });
    }

    public void filter(String jpqlFilter) {
        if (jpqlFilter.isEmpty()) {
            listEntities(entityType);
        } else {
            String baseJpqlQuery = getBaseJpqlQuery();
            String jpql = baseJpqlQuery + " where ";
            jpql += jpqlFilter;
            listEntities(jpql);
        }
    }

    public Component createFilterField() {
        return new FilterInput(this);
    }

    private class DeleteEntityButton extends org.vaadin.firitin.components.button.DeleteButton {

        DeleteEntityButton(Object entity) {
            super(() -> {
                EntityManager em = getEntityManager();
                em.getTransaction().begin();
                var reattached = em.merge(entity);
                em.remove(reattached);
                try {
                    em.getTransaction().commit();
                } catch (Exception e) {
                    em.getTransaction().rollback();
                    String msg = e.getMessage();
                    if (e instanceof jakarta.persistence.RollbackException re) {
                        msg += ":" + e.getCause().getMessage();
                    }
                    Notification.show(msg);
                }
                listEntities(entityType);
            });
            setTooltipText("Tries to delete entity. Note, that this can fail for constraint violations.");
            addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        }
    }

    private static class PrettyPrintButton extends PopoverButton {

        public PrettyPrintButton(Object entity) {
            super(() -> PrettyPrinter.toVaadin(entity));
            setIcon(VaadinIcon.EYE.create());
            addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            setTooltipText("Browse entity details");
        }
    }

    private static class AssociationColumn extends VHorizontalLayout {

        public AssociationColumn(Object value) {
            add(
                    new Emphasis("🔗→ " + PrettyPrinter.printOneLiner(value, 100)) {
                {
                    setMaxWidth("200px");
                    getStyle().setOverflow(Style.Overflow.HIDDEN);
                    getStyle().set("text-overflow", "ellipsis");
                    setTitle("Column is an assosiation to another entity.");
                }
            },
                    new PrettyPrintButton(value)
            );
            setSpacing(false);
        }
    }

    static class FilterInput extends VTextField {

        public FilterInput(JpaEntityGrid grid) {
            setClearButtonVisible(true);
            setPlaceholder("Filter with jqpl, e.g. \"name like '%foo%'\"");
            addValueChangeListener(e -> {
                try {
                    grid.filter(e.getValue());
                } catch (Exception ex) {
                    Notification.show("Failed to filter: " + ex.getMessage());
                }
            });

            var popoverButton = new PopoverButton(() -> {
                Set<Attribute<?, ?>> attributes = (Set<Attribute<?, ?>>) grid.getEntityType().getAttributes();
                return new VVerticalLayout() {
                    {
                        add(new H3("Add new filter with template"));
                        for (Attribute<?, ?> attribute : attributes) {
                            add(new VHorizontalLayout() {
                                {
                                    setAlignItems(Alignment.BASELINE);
                                    add(attribute.getName() + ":");
                                    for (FilterType filterType : FilterType.values()) {
                                        add(new VButton(filterType.name()) {
                                            {
                                                addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                                                addClickListener(e -> {
                                                    addFilterTemplate(attribute, filterType);
                                                    findAncestor(Popover.class).close();
                                                });
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                };

            });
            popoverButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            setSuffixComponent(popoverButton);
            setWidthFull();
        }

        void addFilterTemplate(Attribute attribute, FilterType filterType) {
            StringBuilder sb = new StringBuilder(getValue());
            if (!sb.isEmpty()) {
                sb.append(" AND ");
            }
            sb.append(attribute.getName()).append(" ");
            switch (filterType) {
                case like:
                    sb.append("LIKE '%foo%'");
                    break;
                case startsWith:
                    sb.append("LIKE 'foo%'");
                    break;
                case equals:
                    sb.append("= 1");
                    break;
            }
            String filter = sb.toString();
            setValue(filter);
            int foo = filter.lastIndexOf("foo");
            setSelectionRange(foo, foo + 3);
        }

        // TODO make these data type specific, so that e.g. strings, numbers and dates are handled
        // differently
        enum FilterType {
            like, startsWith, equals
        }
    }

    private class QuickEditButton extends VButton {

        public QuickEditButton(Object proxy) {
            setIcon(VaadinIcon.EDIT.create());
            addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            addClickListener(() -> {
                navigate(EntityEditorView.class).ifPresent(view -> {
                    view.editEntity(proxy);
                });
            });
            setTooltipText("Edit entity");
        }
    }

}
