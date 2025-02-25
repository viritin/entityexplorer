package in.virit.entityexplorer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.persistence.MapsId;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.customfield.VCustomField;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.components.popover.PopoverButton;
import org.vaadin.firitin.rad.AutoForm;
import org.vaadin.firitin.rad.AutoFormContext;
import org.vaadin.firitin.rad.PrettyPrinter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@MenuItem(hidden = true)
@AnonymousAllowed
public class EntityEditorView extends VVerticalLayout implements EntityManagerAwareComponent {

    private static ObjectMapper jack = new ObjectMapper();
    private final AutoFormContext ctx;
    private EntityType entityType;

    public EntityEditorView() {
        ctx = new AutoFormContext();

        ctx.withPropertyEditor(propertyContext -> {
            String name = propertyContext.getName();
            Attribute attr = entityType.getAttribute(name);
            if (attr.isAssociation()) {
                if (attr.getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE) {
                    return new GenericManyToOneEditor(attr);
                } else if (attr.getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_ONE) {
                    if (true) {
                        return new OneToOneEditor();
                    }
                }
            }

            return null;
        });
        add(new Paragraph("Pick entity type from the menu"));
    }

    private static void showPropertyDetails(EntityType entityType, AutoForm<Object> form) {
        List<String> boundProperties = form.getBinder().getBoundProperties();
        for (String boundProperty : boundProperties) {
            Attribute attribute = entityType.getAttribute(boundProperty);
            Member javaMember = attribute.getJavaMember();
            if (javaMember instanceof Field field) {
                var metadata = new StringBuilder();
                metadata.append(attribute.getName());
                metadata.append(" ");
                metadata.append(field.getType().getName());
                metadata.append(" ");
                Annotation[] annotations = field.getAnnotations();
                for (Annotation annotation : annotations) {
                    metadata.append("@");
                    metadata.append(annotation.annotationType().getSimpleName());
                    metadata.append("(");
                    Method[] declaredMethods = annotation.annotationType().getDeclaredMethods();
                    boolean first = true;
                    for (Method m : declaredMethods) {
                        try {
                            Object pv = m.invoke(annotation);
                            Parameter[] parameters = m.getParameters();
                            Object defaultValue = m.getDefaultValue();
                            if (pv.getClass().isArray()) {
                                pv = jack.writeValueAsString(pv);
                                if (defaultValue != null) {
                                    defaultValue = jack.writeValueAsString(defaultValue);
                                }
                            }
                            if (defaultValue == null || !pv.equals(defaultValue)) {
                                if (first) {
                                    first = false;
                                } else {
                                    metadata.append(", ");
                                }
                                metadata.append(m.getName()).append("=");
                                if (pv instanceof String) {
                                    String s = (String) pv;
                                    metadata.append("\"");
                                    metadata.append(s);
                                    metadata.append("\"");
                                } else {
                                    metadata.append(pv);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    metadata.append(")");
                    metadata.append(" ");
                }
                HasValue editor = form.getBinder().getEditor(boundProperty);

                if (editor instanceof HasHelper ht) {
                    ht.setHelperText(metadata.toString());
                }
            }
        }
    }

    public void editEntity(Object detachedEntity) {
        Object merged = getEntityManager().merge(detachedEntity);
        editEntity(merged, getEntityManagerFactory().getMetamodel().entity(merged.getClass()));
    }

    public void addEntity(EntityType entityType) {
        try {
            Object newInstance = entityType.getJavaType().getConstructor().newInstance();
            editEntity(newInstance, entityType);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void editEntity(Object entity, EntityType entityType) {
        updateViewTitle(entity);
        removeAll();
        this.entityType = entityType;
        var em = getEntityManager();
        AutoForm<Object> form = ctx.createForm(entity);
        form.setSaveHandler(v -> {
            em.getTransaction().begin();
            em.merge(entity);
            try {
                em.getTransaction().commit();
            } catch (Exception e) {
                em.getTransaction().rollback();
                Notification.show("Error occured while saving:" + e.getMessage());
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
            }
            navigate(EntityExplorer.class)
                    .ifPresent(view -> view.setEntityType(entityType));
        });

        add(form.getFormBody());
        add(form.getActions());

        add(new VButton("Display property details...", e -> {
            showPropertyDetails(entityType, form);
            e.getSource().removeFromParent();
        }).withThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL));

        Optional<String> first = form.getBinder().getBoundProperties().stream().findFirst();
        first.ifPresent(s -> {
            HasValue editor = form.getBinder().getEditor(s);
            if (editor instanceof Focusable<?> f) {
                f.focus();
            }
        });

    }

    protected void updateViewTitle(Object entity) {
        var v = findAncestor(TopLayout.class);
        if (v != null) {
            v.setViewTitle("Editing " + entity.getClass().getSimpleName());
        }
    }

    private static class GenericManyToOneEditor extends CustomField<Object> implements EntityManagerAwareComponent {

        private final SingularAttribute attr;
        Object value;
        Emphasis currentValue = new Emphasis() {
            {
                getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
                getStyle().setOverflow(Style.Overflow.HIDDEN);
                getStyle().set("text-overflow", "ellipsis");
                getStyle().setMaxWidth("400px");
            }
        };
        PopoverButton popoverButton = new PopoverButton(() -> {
            return pickEntity();
        });

        private Component pickEntity() {
            // from a grid
            // For many to one we can use a Grid to select the entity (ComboBox would be kind of better UX, but no easy way to do a good toString/filtering
            ManagedType managedType = getEntityManagerFactory().getMetamodel().managedType(getJavaType());

            EntityType<Object> entityType1 = getEntityManagerFactory().getMetamodel().entity(getJavaType());
            JpaEntityGrid<Object> gridSelect = new JpaEntityGrid<>(entityType1, getEntityManager());
            gridSelect.setMinWidth("70vw");
            gridSelect.getColumnByKey("actions").removeFromParent();

            gridSelect.addValueChangeListener(e -> {
                value = e.getValue();
                GenericManyToOneEditor.this.updateValue();
                setPresentationValue(value);
                gridSelect.findAncestor(Popover.class).close();
            });
            return new VVerticalLayout(
                    new H4("Pick a new value for relation:"),
                    gridSelect.createFilterField(),
                    gridSelect
            );

        }

        public GenericManyToOneEditor(Attribute attr) {
            this.attr = (SingularAttribute) attr;
            popoverButton.setTooltipText("Pick a new value...");
            add(new HorizontalLayout() {
                {
                    setAlignItems(Alignment.BASELINE);
                    setWidthFull();
                    addAndExpand(currentValue);
                    add(popoverButton);
                }
            });
            Member javaMember = this.attr.getJavaMember();
            if (javaMember instanceof Field field) {
                if (field.getAnnotation(MapsId.class) != null) {
                    setReadOnly(true);
                    GenericManyToOneEditor.this.setTooltipText("Part of id, immutable");
                }
            }
        }

        private Class<Object> getJavaType() {
            return attr.getType().getJavaType();
        }

        @Override
        protected Object generateModelValue() {
            return value;
        }

        @Override
        protected void setPresentationValue(Object o) {
            String string = "🔗→ " + PrettyPrinter.printOneLiner(o, 400);
            currentValue.setText(string);
            currentValue.setTitle(string); // maybe clipped by browser/css
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            //super.setReadOnly(readOnly);
            popoverButton.setEnabled(!readOnly);
        }
    }

    public class OneToOneEditor extends VCustomField<Object> {

        private HorizontalLayout content = new HorizontalLayout();

        public OneToOneEditor() {
            super();
            add(content);
        }

        @Override
        protected OneToOneEditor generateModelValue() {
            return null;
        }

        @Override
        protected void setPresentationValue(Object o) {
            content.removeAll();
            if (o == null) {
                content.add(new Span("null"));
            } else {
                content.add(new Emphasis(PrettyPrinter.printOneLiner(o, 40)));
                content.add(new VButton(VaadinIcon.EDIT, () -> {
                    navigate(EntityEditorView.class).ifPresent(entityEditorView
                            -> entityEditorView.editEntity(o));
                }).withTooltip("Edit referenced (1-1) entity..."));
            }
        }

    }
}
