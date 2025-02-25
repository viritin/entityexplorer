package in.virit.entityexplorer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.event.EventListener;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import org.jboss.logging.Logger;

@AutoConfiguration
public class EntityExplorerVaadinAppAutoconfiguration {

    @EventListener
    void registerRoutes(ServiceInitEvent evt) {
        RouteConfiguration configuration = RouteConfiguration
                .forApplicationScope();
        
        Logger.getLogger(EntityExplorerVaadinAppAutoconfiguration.class).info("Registering Entity Explorer to be available under /entityexplorer/");

        final String prefix = "entityexplorer/";
        configuration.setRoute(prefix + "", About.class, TopLayout.class);
        configuration.setRoute(prefix + "entityeditor", EntityEditorView.class, TopLayout.class);
        configuration.setRoute(prefix + "entityexplorer", EntityExplorer.class, TopLayout.class);
        
        // Better error handling, e.g. with Grid & error in lazy loading from backend
        evt.getSource().addSessionInitListener(e -> {
            e.getSession().setErrorHandler((ErrorHandler) event -> {
                UI current = UI.getCurrent();
                if (current != null) {
                    current.access(() -> Notification.show("Error: " + event.getThrowable().getMessage()));
                    event.getThrowable().printStackTrace();
                } else {
                    event.getThrowable().printStackTrace();
                }
            });
        });


    }

}
