package in.virit.entityexplorer;

import com.vaadin.flow.router.Route;
import org.vaadin.firitin.components.RichText;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;

@Route(value = "", layout = TopLayout.class)
public class About extends VVerticalLayout {

    public About() {
        add(new RichText().withMarkDown("""
        # Entity Explorer
        
        This is a tiny generic web UI for JPA backend showing all entities and allowing you to browse and
        modify them during development/testing. Think of it as a generic DB admin tool, 
        that works on JPA level. In certain cases it can replace tools like phpMyAdmin, H2 console or native
        DB admin apps. Note that some entities can be read-only (e.g those mapping to views etc)
        so you can't modify them and inserting new Entities may fail unless some automatic ID generation
        is in use.
        
        DISCLAIMER: This is not a full-fledged admin UI, but a simple tool to help you during development.
        Don't use it as a starting point for your production Vaadin UIs, as this uses patterns that
        are "non-optimal" especially related to how entity manager and transactions are handled. In most
        cases it is better to implement a service layer that your Vaadin UI accesses instead of fiddling
        directly with EntityManager. In those rare cases, where this tooling might be your "missing admin UI",
        be sure to secure it properly and don't expose it to the public internet.
        
        The source code for this tool is available on [GitHub](https://github.com/viritin/entityexplorer/).
        
        """));
    }

}
