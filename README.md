# Entity Explorer

A general purpose "database browser" for Spring Boot apps that works on JPA level (~ uses JPA &amp; JPQL instead of raw JDBC/SQL). It can be dropped to a Spring Boot application during development (easier to see what is happening in DB and to make tiny ad hoc changes to the test data) or even to implement a very generic view for administrators, to replace raw "SQL UI" for less techical users.

<img width="1439" alt="Screenshot 2024-12-17 at 22 42 49" src="https://github.com/user-attachments/assets/b3479f8d-50e6-4344-a612-d636e9555dfc" />

*A Screenshot of a development version of Entity Explorer applied on [Sakila example database](https://github.com/mstahv/sakila-spring-data-jpa-starter).*

## Disclaimers and Design Notes

 * The app aquires EntityManagerFactory directly form ApplicationContext and creates an EntityManager per view/component. This is NOT the most efficient way to consume your DB, but fairly handy for this kind of test/admin UI as lazy loading works like a charm.
 * As EntityExplorer knows nothing about usage patterns, it makes no smart joins. Thus, especially on entities with lot of relations, there can be dozens of DB queries. Again, not an approach you want to take for an application with lot of active users, but most likely find fine for this kind of usage and might be ok for e.g. admin users.
 * Because of the above design decisisions DOT NOT USE this as an architectural reference for your actual web UI built with Vaadin.

## Current Features

 * Lists all JPA entities in the menu
 * Shows all entities in a grid
  * id hidden by default (but can be made visible or explored with the eye symbol)
  * Also shows a preview relations ( "-> |id|col1|col2..." )
  * Allows digging even to deep relation chains (use the first eye symbol on the row to open viewer)
 * Allows modifying the query predicate aka filtering for developers. Contains templates for all properties.
 * Allows editing existing entities. Simple data types and ManyToOne relations are supported.
 * Allows inserting new entities (if identifier is autogenerated)

## Usage (for non-Vaadin apps)

Add dependency:

		<dependency>
			<groupId>in.virit.entityexplorer</groupId>
			<artifactId>entity-explorer</artifactId>
			<version>0.0.4</version>
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

If you are fine having the EntityExplorer as a "non-integrated" part, for example during development like suggested above, use the following dependency, which expects you already have a reasonably modern Vaadin app in your module. It registers views to /entityexplorer, using the same servlet, with @AnonymousAllowed annotation:

        <dependency>
            <groupId>in.virit.entityexplorer</groupId>
            <artifactId>entity-explorer-vaadin</artifactId>
            <version>0.0.4</version>
	    <scope>test</scope>
        </dependency>

If you want it to be more integrated part of your app, check out the "base classes" from this module. Then sublass them and register to your own app with @Route annotation and with your desired access control logic or do it somehow dynamically, something like [the entity-explorer-vaadin autoconfiguration](https://github.com/viritin/entityexplorer/blob/main/entity-explorer-vaadin/src/main/java/in/virit/entityexplorer/EntityExplorerVaadinAppAutoconfiguration.java). Also see the [TopLayout class](https://github.com/viritin/entityexplorer/blob/main/entity-explorer-base/src/main/java/in/virit/entityexplorer/TopLayout.java) for an example how it registers quick links to all found entities.

        <dependency>
            <groupId>in.virit.entityexplorer</groupId>
            <artifactId>entity-explorer-base</artifactId>
            <version>0.0.4</version>
        </dependency>
