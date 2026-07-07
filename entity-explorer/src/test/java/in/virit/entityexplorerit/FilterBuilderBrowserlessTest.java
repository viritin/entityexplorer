package in.virit.entityexplorerit;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import in.virit.entityexplorer.EntityExplorer;
import in.virit.entityexplorer.EntityExplorerAutoconfiguration;
import in.virit.entityexplorer.filter.FilterGroup;
import in.virit.entityexplorer.filter.FilterOperator;
import in.virit.entityexplorer.filter.FilterProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Browserless port of the visual filter builder scenarios (see
 * {@link FilterBuilderTest} for the real-browser variant): runs against the
 * server-side component tree in milliseconds, so it can afford more scenarios.
 * <p>
 * Expected counts come from customerdata.sql: 100 customers, 18 in Berlin of
 * which 15 active, 31 inactive, 21 match
 * {@code creditScore >= 95 OR (city = Berlin AND active)}.
 */
@SpringBootTest
public class FilterBuilderBrowserlessTest {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void basicAndFlowBadgeAndJpqlExclusion() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();
            ui.navigate(EntityExplorer.class, "Customer");
            assertEquals(100, gridSize(ui));

            // Build: address.city contains Berlin AND active is true
            ui.findButton().withText("Filters").click();
            ui.findButton().withText("Add condition").click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").selectItem("address.city");
            ui.findSelect(FilterOperator.class).withLabel("Operator").selectItem("contains");
            ui.findTextField().withLabel("Value").setValue("Berlin");
            ui.findButton().withText("Add condition").click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").atIndex(2).selectItem("active");
            // boolean condition renders no value field; the string one remains
            assertEquals(1, ui.findTextField().withLabel("Value").components().size());
            ui.findButton().withText("Apply").click();

            assertEquals(15, gridSize(ui));
            assertEquals("Filters (2)", badgeText(ui));
            assertEquals("", jpqlField(ui).component().getValue(),
                    "applying the visual filter must clear the JPQL quick filter");

            // JPQL quick filter overrides the visual filter and resets the badge
            jpqlField(ui).setValue("active = false");
            assertEquals(31, gridSize(ui));
            assertEquals("Filters", badgeText(ui));
        }
    }

    @Test
    void nestedGroupsAndNegation() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();
            ui.navigate(EntityExplorer.class, "Customer");

            // Root: creditScore >= 95, combined with OR
            ui.findButton().withText("Filters").click();
            ui.findButton().withText("Add condition").click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").selectItem("creditScore");
            ui.findSelect(FilterOperator.class).withLabel("Operator").selectItem(">=");
            ui.findIntegerField().withLabel("Value").setValue(95);
            ui.findSelect(FilterGroup.Logic.class).withLabel("Combine").selectItem("OR");

            // Nested group: address.city = Berlin AND active is true (both use
            // the operator preselected on property pick). The nested group's
            // own "Add condition" button comes first in tree order.
            ui.findButton().withText("Add group").click();
            ui.findButton().withText("Add condition").atIndex(1).click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").atIndex(2).selectItem("address.city");
            ui.findTextField().withLabel("Value").setValue("Berlin");
            ui.findButton().withText("Add condition").atIndex(1).click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").atIndex(3).selectItem("active");
            ui.findButton().withText("Apply").click();

            assertEquals(21, gridSize(ui));
            assertEquals("Filters (3)", badgeText(ui));

            // Negate the whole root group: NOT(score OR (Berlin AND active))
            ui.findButton().withTextContaining("Filters").click();
            ui.findCheckbox().withLabel("NOT").atIndex(1).click();
            ui.findButton().withText("Apply").click();
            assertEquals(79, gridSize(ui));
        }
    }

    @Test
    void clearResetsToUnfilteredListing() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();
            ui.navigate(EntityExplorer.class, "Customer");

            ui.findButton().withText("Filters").click();
            ui.findButton().withText("Add condition").click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").selectItem("active");
            ui.findButton().withText("Apply").click();
            assertEquals(69, gridSize(ui));
            assertEquals("Filters (1)", badgeText(ui));

            ui.findButton().withTextContaining("Filters").click();
            ui.findButton().withText("Clear").click();
            assertEquals(100, gridSize(ui));
            assertEquals("Filters", badgeText(ui));
        }
    }

    private int gridSize(BrowserlessUIContext ui) {
        return ui.test(ui.findGrid(Object.class).component()).size();
    }

    private String badgeText(BrowserlessUIContext ui) {
        return ui.findButton().withTextContaining("Filters").component().getText();
    }

    private com.vaadin.flow.component.textfield.TextFieldLocator jpqlField(BrowserlessUIContext ui) {
        return ui.findTextField().withPlaceholderContaining("jqpl");
    }
}
