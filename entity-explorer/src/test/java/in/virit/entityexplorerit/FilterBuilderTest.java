package in.virit.entityexplorerit;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.vaadin.addons.dramafinder.AbstractBasePlaywrightIT;
import org.vaadin.addons.dramafinder.element.ButtonElement;
import org.vaadin.addons.dramafinder.element.CheckboxElement;
import org.vaadin.addons.dramafinder.element.ComboBoxElement;
import org.vaadin.addons.dramafinder.element.DialogElement;
import org.vaadin.addons.dramafinder.element.IntegerFieldElement;
import org.vaadin.addons.dramafinder.element.SelectElement;
import org.vaadin.addons.dramafinder.element.TextFieldElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browser test for the visual filter builder: builds
 * {@code address.city contains 'Berlin' AND active is true} for Customer,
 * applies it and verifies the grid, the "Filters (n)" badge and the mutual
 * exclusion with the JPQL quick filter.
 * <p>
 * Expected counts come from customerdata.sql: 18 customers in Berlin, 15 of
 * them active, 31 inactive customers in total.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FilterBuilderTest extends AbstractBasePlaywrightIT {

    /** Same wait script as in {@link EntityScreensSmokeTest}. */
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

    @Override
    public String getUrl() {
        return "http://localhost:" + port + "/";
    }

    @Test
    void buildApplyAndOverrideVisualFilter() {
        // The first navigation may trigger a cold Vaadin dev-mode frontend build.
        page.setDefaultNavigationTimeout(300_000);
        page.setDefaultTimeout(60_000);
        page.navigate(getUrl() + "entityexplorer/entityexplorer/Customer");
        page.waitForFunction(WAIT_FOR_VAADIN);
        page.locator("vaadin-grid").first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));

        // Open the builder and add: address.city contains Berlin
        ButtonElement.getByText(page, "Filters").click();
        new DialogElement(page).assertOpen();
        ButtonElement.getByText(page, "Add condition").click();
        ComboBoxElement.getByLabel(page, "Property").selectItem("address.city");
        SelectElement.getByLabel(page, "Operator").selectItem("contains");
        TextFieldElement.getByLabel(page, "Value").setValue("Berlin");

        // Second condition: active is true (property pick preselects "is true";
        // boolean conditions must not render a value field)
        ButtonElement.getByText(page, "Add condition").click();
        new ComboBoxElement(page.locator("vaadin-combo-box").nth(1)).selectItem("active");
        // selects: 0 = root "Combine", 1 = first row operator, 2 = this row's
        new SelectElement(page.locator("vaadin-select").nth(2)).assertValue("is true");
        assertEquals(1, page.getByLabel("Value").count(),
                "boolean condition must not have a value field");

        ButtonElement.getByText(page, "Apply").click();

        waitForGridSize(15);
        assertBadge("Filters (2)");
        // applying the visual filter must clear the JPQL quick filter
        new TextFieldElement(page.locator("vaadin-text-field").first()).assertValue("");

        // Reopen: the built conditions are still there for further editing
        ButtonElement.getByText(page, "Filters").click();
        new DialogElement(page).assertOpen();
        assertEquals(2, page.getByLabel("Property").count());
        ButtonElement.getByText(page, "Close").click();

        // Applying a JPQL quick filter overrides the visual one and resets the badge
        new TextFieldElement(page.locator("vaadin-text-field").first()).setValue("active = false");
        waitForGridSize(31);
        assertBadge("Filters");
    }

    @Test
    void nestedGroupsAndNegation() {
        page.setDefaultNavigationTimeout(300_000);
        page.setDefaultTimeout(60_000);
        page.navigate(getUrl() + "entityexplorer/entityexplorer/Customer");
        page.waitForFunction(WAIT_FOR_VAADIN);
        page.locator("vaadin-grid").first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));

        ButtonElement.getByText(page, "Filters").click();
        new DialogElement(page).assertOpen();

        // Root: creditScore >= 95, combined with OR
        ButtonElement.getByText(page, "Add condition").click();
        ComboBoxElement.getByLabel(page, "Property").selectItem("creditScore");
        SelectElement.getByLabel(page, "Operator").selectItem(">=");
        IntegerFieldElement.getByLabel(page, "Value").setValue("95");
        SelectElement.getByLabel(page, "Combine").selectItem("OR");

        // Nested group: address.city = Berlin AND active is true (both use the
        // operator preselected on property pick). Wait for the nested group to
        // render before clicking its "Add condition", which then comes first
        // in DOM order. Positional lookups use nth(k) — unlike last(), nth
        // auto-waits until the server-added element at that index exists.
        ButtonElement.getByText(page, "Add group").click();
        page.waitForFunction("() => document.querySelectorAll('.filter-group').length === 2");
        firstButton("Add condition").click();
        new ComboBoxElement(page.locator("vaadin-combo-box").nth(1)).selectItem("address.city");
        TextFieldElement.getByLabel(page, "Value").setValue("Berlin");
        firstButton("Add condition").click();
        new ComboBoxElement(page.locator("vaadin-combo-box").nth(2)).selectItem("active");

        ButtonElement.getByText(page, "Apply").click();
        // creditScore >= 95 OR (city = Berlin AND active): 21 rows in customerdata.sql
        waitForGridSize(21);
        assertBadge("Filters (3)");

        // Negate the whole root group: NOT(score OR (Berlin AND active)) -> 79
        ButtonElement.getByText(page, "Filters").click();
        new DialogElement(page).assertOpen();
        new CheckboxElement(page.locator("vaadin-checkbox").first()).check();
        ButtonElement.getByText(page, "Apply").click();
        // 79 exceeds the first page: scroll until the lazy size settles
        waitForGridSizeScrolling(79);
    }

    private ButtonElement firstButton(String text) {
        return new ButtonElement(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(text)).first());
    }

    private void waitForGridSize(int expected) {
        page.waitForFunction("n => document.querySelector('vaadin-grid')?._flatSize === n", expected);
    }

    /**
     * With an undefined-size lazy data provider the grid only learns the real
     * row count once it has scrolled past the end; nudge it until it settles.
     */
    private void waitForGridSizeScrolling(int expected) {
        for (int i = 0; i < 15; i++) {
            if (Integer.valueOf(expected).equals(
                    page.evaluate("() => document.querySelector('vaadin-grid')?._flatSize"))) {
                break;
            }
            page.evaluate("() => document.querySelector('vaadin-grid').scrollToIndex(1e9)");
            page.waitForTimeout(300);
        }
        waitForGridSize(expected);
    }

    private void assertBadge(String expected) {
        // textContent includes the button's tooltip text -> compare the start
        String text = ButtonElement.getByText(page, "Filters").getText().trim();
        String caption = text.replaceFirst("(?s)(Filters( \\(\\d+\\))?).*", "$1");
        assertEquals(expected, caption);
    }
}
