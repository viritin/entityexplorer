package in.virit.entityexplorerit;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Smoke test that visits every entity screen listed in the Entity Explorer and
 * verifies the entity grid renders without a server-side error.
 * <p>
 * The list of screens is taken from the JPA metamodel &mdash; exactly the same
 * source the {@code /entityexplorer/} navigation ({@code TopLayout}) uses to
 * build its menu &mdash; and each entity is opened via its deep-link URL
 * ({@code /entityexplorer/entityexplorer/<EntityName>}).
 * <p>
 * This is a plain Spring Boot test driven with dramafinder's Playwright helpers,
 * so it runs with {@code mvn test}. On the very first run Playwright downloads a
 * browser and Vaadin builds the dev-mode frontend bundle, hence the generous
 * navigation timeout for the initial page load.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EntityScreensSmokeTest extends AbstractBasePlaywrightIT {

    /**
     * Resolves to {@code true} once Vaadin Flow has finished processing and the
     * dev server (if any) has loaded. A copy of dramafinder's own wait script,
     * which is not visible outside its package.
     */
    private static final String WAIT_FOR_VAADIN = """
            () => {
              if (window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.clients) {
                for (var client in window.Vaadin.Flow.clients) {
                  if (window.Vaadin.Flow.clients[client].isActive()) { return false; }
                }
                return true;
              } else if (window.Vaadin && window.Vaadin.Flow && window.Vaadin.Flow.devServerIsNotLoaded) {
                return false;
              }
              return true;
            }""";

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Override
    public String getUrl() {
        return "http://localhost:" + port + "/";
    }

    @Test
    void allEntityScreensOpenWithoutError() {
        // The first navigation triggers a cold Vaadin dev-mode frontend build.
        page.setDefaultNavigationTimeout(300_000);
        page.setDefaultTimeout(60_000);

        List<String> entityNames = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(EntityType::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
        System.out.println("Entity screens to smoke test: " + entityNames);

        List<String> failures = new ArrayList<>();
        for (String entity : entityNames) {
            // Deep link to the Entity Explorer view for this entity.
            String url = getUrl() + "entityexplorer/entityexplorer/" + entity;
            try {
                page.navigate(url);
                page.waitForFunction(WAIT_FOR_VAADIN);
                // Every entity screen shows a JpaEntityGrid (<vaadin-grid>). If the
                // view threw during construction, no grid renders and this fails.
                page.locator("vaadin-grid").first().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15_000));
            } catch (RuntimeException e) {
                failures.add(entity + ": grid did not render. Page said: \""
                        + firstLine(page.locator("body").innerText()) + "\"");
            }
        }

        if (!failures.isEmpty()) {
            fail("Entity screen(s) failed to open cleanly:\n - " + String.join("\n - ", failures));
        }
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "(no visible text)";
        }
        int nl = text.indexOf('\n');
        return (nl > 0 ? text.substring(0, nl) : text).trim();
    }
}
