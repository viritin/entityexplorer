package in.virit.entityexplorer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.firitin.appframework.AdvancedSideNav;
import org.vaadin.firitin.appframework.MainLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SpringComponent
@Scope("prototype")
@AnonymousAllowed
public class TopLayout extends MainLayout {

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Override
    protected String getDrawerHeader() {
        return "}> JPA Explorer";
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
    }

    @Override
    protected void addDrawerContent() {
        super.addDrawerContent();

    }

    @Override
    public void buildMenu() {
        super.buildMenu();
        AdvancedSideNav submenu = new AdvancedSideNav();
        submenu.setLabel("Application entities");
        submenu.setExpanded(true);
        getMenu().addSubMenu(submenu);

        String ee = RouteConfiguration.forSessionScope().getUrl(EntityExplorer.class);

        List<EntityType<?>> entities = new ArrayList<>(entityManagerFactory.getMetamodel().getEntities());
        Collections.sort(entities, Comparator.comparing(EntityType::getName));
        entities.forEach(e -> {
            // TODO VMP
            // TODO VMP 2X

            submenu.addItem(new SideNavItem(e.getName(), ee + "/"+e.getName()){{
                getStyle().setMarginBottom("-5px");
                getStyle().setMarginLeft("5px");
                }});
        });
    }
}
