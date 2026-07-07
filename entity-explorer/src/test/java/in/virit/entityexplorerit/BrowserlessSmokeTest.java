package in.virit.entityexplorerit;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.grid.Grid;
import in.virit.entityexplorer.EntityExplorer;
import in.virit.entityexplorer.EntityExplorerAutoconfiguration;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browserless smoke test: opens every entity screen of the Entity Explorer
 * against the server-side component tree — no browser, no frontend build, so
 * this runs in milliseconds compared to the Playwright based
 * {@link EntityScreensSmokeTest}.
 * <p>
 * The routes are registered dynamically in a {@code ServiceInitEvent} listener
 * ({@link EntityExplorerAutoconfiguration}), so the view package scan finds
 * nothing by annotation; navigation still works because the listener runs when
 * the browserless Vaadin service starts.
 */
@SpringBootTest
public class BrowserlessSmokeTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    EntityManagerFactory emf;

    @Test
    void allEntityScreensOpenWithoutError() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();

            List<String> entityNames = emf.getMetamodel().getEntities().stream()
                    .map(EntityType::getName)
                    .sorted(Comparator.naturalOrder())
                    .toList();

            for (String entityName : entityNames) {
                // Navigation fails loudly if the view constructor or
                // setParameter throws (the old creditRating bug would have
                // surfaced here as an InternalServerError navigation target).
                EntityExplorer view = ui.navigate(EntityExplorer.class, entityName);
                assertInstanceOf(EntityExplorer.class, ui.getCurrentView(),
                        entityName + " screen did not open");
                assertTrue(ui.findGrid(Object.class).inside(view).exists(),
                        entityName + " screen has no entity grid");
            }

            // Spot check that the Customer grid actually serves the seed data
            EntityExplorer view = ui.navigate(EntityExplorer.class, "Customer");
            Grid<Object> grid = ui.findGrid(Object.class).inside(view).component();
            var tester = ui.test(grid);
            assertTrue(tester.size() > 0, "Customer grid should have rows");
        }
    }
}
