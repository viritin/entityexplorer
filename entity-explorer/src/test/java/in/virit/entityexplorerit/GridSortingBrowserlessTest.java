package in.virit.entityexplorerit;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridTester;
import com.vaadin.flow.data.provider.SortDirection;
import in.virit.entityexplorer.EntityExplorer;
import in.virit.entityexplorer.EntityExplorerAutoconfiguration;
import in.virit.entityexplorer.filter.FilterProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Browserless tests for the grid's backend sorting: basic-typed columns sort
 * through both listing paths (JPQL and, with a visual filter applied, the
 * criteria one); association, embedded and computed columns are not sortable.
 * <p>
 * Expected values come from customerdata.sql (100 rows): first company name
 * ascending is "Acme Manufacturing Cologne", descending "Zenith Medical
 * Hamburg"; 69 active customers with max creditScore 99.
 */
@SpringBootTest
public class GridSortingBrowserlessTest {

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void sortsThroughJpqlListingPath() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();
            ui.navigate(EntityExplorer.class, "Customer");
            GridTester<Grid<Object>, Object> grid = ui.test(ui.findGrid(Object.class).component());

            grid.sortByColumn("companyName", SortDirection.ASCENDING);
            assertEquals("Acme Manufacturing Cologne", customer(grid, 0).getCompanyName());

            grid.sortByColumn("companyName", SortDirection.DESCENDING);
            assertEquals("Zenith Medical Hamburg", customer(grid, 0).getCompanyName());

            // sorting composes with the JPQL quick filter too
            ui.findTextField().withPlaceholderContaining("jqpl").setValue("active = true");
            grid.sortByColumn("creditScore", SortDirection.DESCENDING);
            assertEquals(69, grid.size());
            assertEquals(99, customer(grid, 0).getCreditScore());
        }
    }

    @Test
    void sortsThroughCriteriaListingPath() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();
            ui.navigate(EntityExplorer.class, "Customer");

            // active is true via the visual builder -> criteria listing path
            ui.findButton().withText("Filters").click();
            ui.findButton().withText("Add condition").click();
            ui.findComboBox(FilterProperty.class).withLabel("Property").selectItem("active");
            ui.findButton().withText("Apply").click();

            GridTester<Grid<Object>, Object> grid = ui.test(ui.findGrid(Object.class).component());
            assertEquals(69, grid.size());
            grid.sortByColumn("creditScore", SortDirection.DESCENDING);
            assertEquals(99, customer(grid, 0).getCreditScore());
            assertTrue(customer(grid, 0).isActive());

            grid.sortByColumn("companyName", SortDirection.ASCENDING);
            assertEquals("Acme Manufacturing Cologne", customer(grid, 0).getCompanyName());
        }
    }

    @Test
    void onlyBasicTypedColumnsAreSortable() {
        try (var app = SpringBrowserlessApplicationContext.create(applicationContext, EntityExplorerAutoconfiguration.class)) {
            BrowserlessUIContext ui = app.newUser().newWindow();
            ui.navigate(EntityExplorer.class, "Customer");
            Grid<Object> grid = ui.findGrid(Object.class).component();

            assertTrue(grid.getColumnByKey("companyName").isSortable());
            assertTrue(grid.getColumnByKey("creditScore").isSortable());
            assertTrue(grid.getColumnByKey("customerSince").isSortable());
            assertFalse(grid.getColumnByKey("address").isSortable(), "embedded is not sortable");
            assertFalse(grid.getColumnByKey("creditRating").isSortable(), "computed is not sortable");
            assertFalse(grid.getColumnByKey("actions").isSortable());

            ui.navigate(EntityExplorer.class, "Person");
            Grid<Object> personGrid = ui.findGrid(Object.class).component();
            assertTrue(personGrid.getColumnByKey("firstName").isSortable());
            assertFalse(personGrid.getColumnByKey("friend").isSortable(), "association is not sortable");
        }
    }

    private static Customer customer(GridTester<Grid<Object>, Object> grid, int row) {
        return (Customer) grid.getRow(row);
    }
}
