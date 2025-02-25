package in.virit.entityexplorer;

import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.vaadin.firitin.appframework.MenuItem;
import org.vaadin.firitin.components.button.VButton;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;

import java.util.logging.Level;
import java.util.logging.Logger;

@MenuItem(hidden = true)
@AnonymousAllowed
public class EntityExplorer extends VVerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {
    private final EntityManagerFactory entityManagerFactory;
    private EntityType entityType;

    public EntityExplorer(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void setEntityType(EntityType<?> entityType) {
        removeAll();
        this.entityType = entityType;
        JpaEntityGrid<Object> grid = new JpaEntityGrid<>(entityType);
        add(new VHorizontalLayout()
                .withDefaultVerticalComponentAlignment(Alignment.BASELINE)
                .withExpanded(grid.createFilterField())
                .withComponents(new NewEntityButton())
        );
        addAndExpand(grid);
        updateViewTitle();
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String s) {
        if (s != null) {
            if (entityType != null && entityType.getName().equals(s)) {
                return;
            }
            // If coming in with deep link, we need to find the entity type
            EntityType<?> entityType = entityManagerFactory.getMetamodel().getEntities().stream()
                    .filter(e -> e.getName().equals(s))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No entity with name " + s));
            setEntityType(entityType);
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        updateViewTitle();
    }

    private void updateViewTitle() {
        if (isAttached() && entityType != null) {
            var layout = findAncestor(TopLayout.class);
            // May be used in differet layouts as well programmatically
            if(layout != null)
                layout.setViewTitle("Entity Explorer: " + entityType.getName());
        }
    }
    
    private class NewEntityButton extends VButton {
        public NewEntityButton() {
            addClickListener(event -> {
                        try {
                            navigate(EntityEditorView.class).ifPresent(v -> v.addEntity(entityType));
                        } catch (Exception e1) {
                            Notification.show("Failed to create new entity: " + e1.getMessage());
                            Logger.getLogger(EntityExplorer.class.getName()).log(Level.INFO, "Failed to create new entity", e1);
                        }
                    }
            );
            setIcon(VaadinIcon.PLUS.create());
            setText("New " + entityType.getName());
            addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            getStyle().setMarginRight("1em");
            setTooltipText("Tries to add new " + entityType.getName() + " entity. Note that this may not work for all entities.");
        }
    }
}
