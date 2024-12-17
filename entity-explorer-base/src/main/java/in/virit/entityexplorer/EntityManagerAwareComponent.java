package in.virit.entityexplorer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachNotifier;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.server.VaadinService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.logging.Logger;

/**
 * A helper interface for components that need an entity manager. The entity manager is created lazily and closed when
 * the component is detached.
 * <p>
 * NOTE: this is not the best practice for production usage and can easily create
 * an excessive amount of DB queries. Although this is handy, especially related to accessing lazy loaded entities
 * in the UI, it is recommended to use service classes (and possibly DTOs) for DB access and never use entity manager
 * directly in the UI classes.
 */
public interface EntityManagerAwareComponent extends DetachNotifier, HasElement {

    /**
     * Get the entity manager for this component. The entity manager is created lazily and closed when the component is
     * detached.
     *
     * @return the entity manager
     */
    default EntityManager getEntityManager() {
        Logger.getLogger(EntityManagerAwareComponent.class.getName()).fine("Accessing entity manager for " + getClass().getSimpleName());
        Component component = (Component) this;
        EntityManager entityManager = ComponentUtil.getData(component, EntityManager.class);
        if (entityManager == null) {
            Logger.getLogger(EntityManagerAwareComponent.class.getName()).fine("Creating a new entity manager for " + getClass().getSimpleName());
            entityManager = getEntityManagerFactory().createEntityManager();
            ComponentUtil.setData(component, EntityManager.class, entityManager);
            EntityManager finalEntityManager = entityManager;
            addDetachListener(e -> {
                finalEntityManager.close();
                Logger.getLogger(EntityManagerAwareComponent.class.getName()).fine("Closed entity manager for " + getClass().getSimpleName());
            });
        }
        return entityManager;
    }

    /**
     * Get the entity manager factory for this component. The entity manager factory is retrieved from the Vaadin
     * service (in practice via Spring ApplicationContext).
     *
     * @return the entity manager factory
     */
    default EntityManagerFactory getEntityManagerFactory() {
        return VaadinService.getCurrent().getInstantiator().getOrCreate(EntityManagerFactory.class);
    }

}
