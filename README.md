# Entity Explorer

A general purpose "database browser" for Spring Boot apps that works on JPA level (~ uses JPA &amp; JPQL instead of raw JDBC/SQL). It can be dropped to a Spring Boot application during development (easier to see what is happening in DB and to make tiny ad hoc changes to the test data) or even to implement a very generic view for administrators, to replace raw "SQL UI" for less techical users.

<img width="1439" alt="Screenshot 2024-12-17 at 22 42 49" src="https://github.com/user-attachments/assets/b3479f8d-50e6-4344-a612-d636e9555dfc" />

*A Screenshot of a development version of Entity Explorer applied on [Sakila example database](https://github.com/mstahv/sakila-spring-data-jpa-starter).*

## Usage (for non-Vaadin apps)

Add dependency:

		<dependency>
			<groupId>in.virit.entityexplorer</groupId>
			<artifactId>entity-explorer</artifactId>
			<version>0.0.1</version>
			<scope>test</scope>
		</dependency>

The above uses test scope, which I like, together with Testcontainers initialized database for development/testing, but if you want to use also in normal apps, remove the scope tag.  If you go without the test scope, remember to remove it before going to production or secure the /entityexlorer path properly! Similar in nature with Gradle, if you are a Gradle wizard, I trust you can do it on the above example 💪

Now if you run the app, the EntityExplorer UI is automatically mapped to http://localhost:8080/entityexplorer/ (or whatever your port is). 

To reduce logging and to revert to normal Spring Boot style 404s also add these to your *application.properties* file (or yaml, in src/main/resources or in src/test/resources, whatever are your preferences):


```
# Workaround to make vaadin (via entity-exploorer) not steal 404s in the root context, without this 404s are a bit broken...
vaadin.url-mapping=/entityexplorer/*
# Atmosphere, used by entityexplorer, is very verbose, so we turn it off
logging.level.org.atmosphere=OFF
# And Vaadin itself should not interest unless we are debugging it
logging.level.com.vaadin=WARN
```

## Usage for Vaadin apps

With Vaadin apps, setup is bit more diffifult and may change in upcoming version (if I get some enhancementw to core 🤓). Currently this has proven to work in a trivial case:

Instead of the above dependency, use this module that don't include Spring Boot autoconfiguration: 

        <dependency>
            <groupId>in.virit.entityexplorer</groupId>
            <artifactId>entity-explorer-base</artifactId>
            <version>0.0.1</version>
        </dependency>

Then manually register require views. You might want to use your own main layout, but here we add them to the one included in the module. Add this code snippet to your @SpringBootApplication:

    @EventListener
    void registerRoutes(ServiceInitEvent evt) {
        RouteConfiguration configuration = RouteConfiguration
                .forApplicationScope();
        configuration.setRoute("entityexplorerabout", About.class, TopLayout.class);
        Arrays.asList(EntityEditorView.class, EntityExplorer.class)
                .forEach(view -> {
                    configuration.setAnnotatedRoute(view);
                });

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
