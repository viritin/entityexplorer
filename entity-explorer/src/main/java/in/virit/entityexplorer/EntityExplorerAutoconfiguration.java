package in.virit.entityexplorer;

import static com.vaadin.flow.spring.SpringBootAutoConfiguration.configureServletRegistrationBean;

import java.util.Arrays;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

import jakarta.servlet.MultipartConfigElement;

// TODO figure out how to completely/properly override the default Vaadin Spring Boot autoconfiguration
// Currently Vaadin still steals all 404s even if not under /ee/* !!
// Guess: the default Vaaddin autoconfiguration still registers root mapped servlet
@AutoConfiguration
@AutoConfigureBefore(SpringBootAutoConfiguration.class)
public class EntityExplorerAutoconfiguration {

    @Autowired
    private WebApplicationContext context;

    @EventListener
    void registerRoutes(ServiceInitEvent evt) {
        RouteConfiguration configuration = RouteConfiguration
                .forApplicationScope();
        
        configuration.setRoute("", About.class, TopLayout.class);
        configuration.setRoute("entityeditor", EntityEditorView.class, TopLayout.class);
        configuration.setRoute("entityexplorer", EntityExplorer.class, TopLayout.class);

        // Better error handling, e.g. with Grid & error in lazy loading from backend
        evt.getSource().addSessionInitListener(e -> {
            e.getSession().setErrorHandler((ErrorHandler) event -> {
                UI current = UI.getCurrent();
                if (current != null) {
                    current.access(() -> Notification.show("Error: " + event.getThrowable().getMessage()));
                } else {
                    event.getThrowable().printStackTrace();
                }
            });
        });
    }

    @Bean
    public VaadinServlet vaadinServlet() {
        return new OverriddenSpringServlet(context);
    }

    @Component
    @Push(transport = Transport.WEBSOCKET)
    public static class MyVaadinAppShell implements AppShellConfigurator {
    }

    @Bean
    public ServletRegistrationBean<SpringServlet> entityExplorerservletRegistrationBean(
            ObjectProvider<MultipartConfigElement> multipartConfig,
            VaadinConfigurationProperties configurationProperties, VaadinServlet vaadinServlet) {
        // only actual customisation to default Vaadin Sprinb Boot autoconfiguration
        // using custom url mapping, not to interfere with the actual application
        configurationProperties.setUrlMapping("/entityexplorer/*");
        return configureServletRegistrationBean(multipartConfig,
                configurationProperties, (SpringServlet) vaadinServlet);
    }

    public static class OverriddenSpringServlet extends SpringServlet {

        public OverriddenSpringServlet(ApplicationContext context) {
            // without the second parameter, mystrerious things will happen...
            super(context, false);
        }
    }
}
