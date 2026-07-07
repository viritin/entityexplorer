package in.virit.entityexplorerit;

import in.virit.entityexplorer.filter.CriteriaListing;
import in.virit.entityexplorer.filter.FilterCondition;
import in.virit.entityexplorer.filter.FilterGroup;
import in.virit.entityexplorer.filter.FilterOperator;
import in.virit.entityexplorer.filter.FilterPredicateBuilder;
import in.virit.entityexplorer.filter.FilterProperty;
import in.virit.entityexplorer.filter.FilterSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JPA-level tests for {@link FilterPredicateBuilder} against the H2 database
 * seeded with customerdata.sql (100 Customer rows). Where practical, results
 * are cross-checked against an equivalent hand-written JPQL count.
 * <p>
 * Uses the default MOCK web environment (no real server): the application's
 * autoconfiguration requires a WebApplicationContext, but these tests only
 * need the EntityManagerFactory.
 */
@SpringBootTest
public class FilterPredicateBuilderTest {

    @Autowired
    EntityManagerFactory emf;

    // ---- helpers ----

    private long count(Class<?> entityClass, FilterGroup group) {
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<?> root = cq.from(entityClass);
            cq.select(cb.count(root));
            Predicate predicate = FilterPredicateBuilder.toPredicate(group, root, cb);
            if (predicate != null) {
                cq.where(predicate);
            }
            return em.createQuery(cq).getSingleResult();
        }
    }

    private long jpqlCount(String jpql) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(jpql, Long.class).getSingleResult();
        }
    }

    private long jpqlCount(String jpql, Object param) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(jpql, Long.class).setParameter(1, param).getSingleResult();
        }
    }

    private static FilterCondition condition(String path, Class<?> javaType, FilterOperator op, Object value) {
        FilterCondition condition = new FilterCondition();
        condition.setProperty(new FilterProperty(path, path, javaType, true));
        condition.setOperator(op);
        condition.setValue(value);
        return condition;
    }

    private static FilterGroup group(FilterGroup.Logic logic, in.virit.entityexplorer.filter.FilterNode... children) {
        FilterGroup group = new FilterGroup();
        group.setLogic(logic);
        for (var child : children) {
            group.add(child);
        }
        return group;
    }

    // ---- property discovery ----

    @Test
    void listFor_customer_containsBasicAndEmbeddedButNotDerived() {
        var entityType = emf.getMetamodel().entity(Customer.class);
        List<String> paths = FilterProperty.listFor(entityType).stream().map(FilterProperty::path).toList();

        assertTrue(paths.contains("companyName"));
        assertTrue(paths.contains("creditScore"));
        assertTrue(paths.contains("active"));
        assertTrue(paths.contains("annualRevenue"));
        assertTrue(paths.contains("customerSince"));
        assertTrue(paths.contains("address.city"), "embedded path expected: " + paths);
        assertFalse(paths.contains("creditRating"), "derived getter must not appear");
        assertFalse(paths.contains("address"), "embeddable itself is not filterable");
    }

    @Test
    void listFor_person_excludesAssociationIncludesEnum() {
        var entityType = emf.getMetamodel().entity(Person.class);
        List<String> paths = FilterProperty.listFor(entityType).stream().map(FilterProperty::path).toList();

        assertTrue(paths.contains("firstName"));
        assertTrue(paths.contains("preferredContactMethod"));
        assertFalse(paths.contains("friend"), "associations are out of v1 scope");
    }

    // ---- operator applicability ----

    @Test
    void operatorsForBooleanNeverRequireValue() {
        var active = new FilterProperty("active", "active", boolean.class, false);
        List<FilterOperator> ops = FilterOperator.forProperty(active);
        assertEquals(List.of(FilterOperator.IS_TRUE, FilterOperator.IS_FALSE), ops);
        assertTrue(ops.stream().noneMatch(FilterOperator::requiresValue));
    }

    // ---- predicates against customerdata.sql ----

    @Test
    void containsOnEmbeddedPathIsCaseInsensitive() {
        var filter = group(FilterGroup.Logic.AND,
                condition("address.city", String.class, FilterOperator.CONTAINS, "berlin"));
        assertEquals(18, count(Customer.class, filter));
    }

    @Test
    void andGroupCombines() {
        var filter = group(FilterGroup.Logic.AND,
                condition("address.city", String.class, FilterOperator.CONTAINS, "Berlin"),
                condition("active", boolean.class, FilterOperator.IS_TRUE, null));
        assertEquals(15, count(Customer.class, filter));
    }

    @Test
    void negatedConditionInverts() {
        var activeBerlin = condition("active", boolean.class, FilterOperator.IS_TRUE, null);
        activeBerlin.setNegated(true);
        var filter = group(FilterGroup.Logic.AND,
                condition("address.city", String.class, FilterOperator.CONTAINS, "Berlin"),
                activeBerlin);
        assertEquals(3, count(Customer.class, filter));
    }

    @Test
    void bigDecimalComparisonMatchesJpql() {
        var filter = group(FilterGroup.Logic.AND,
                condition("annualRevenue", BigDecimal.class, FilterOperator.GT, new BigDecimal("200000")));
        assertEquals(
                jpqlCount("select count(c) from Customer c where c.annualRevenue > 200000"),
                count(Customer.class, filter));
    }

    @Test
    void primitiveIntComparisonWithIntegerValue() {
        var filter = group(FilterGroup.Logic.AND,
                condition("creditScore", int.class, FilterOperator.GTE, Integer.valueOf(80)));
        assertEquals(
                jpqlCount("select count(c) from Customer c where c.creditScore >= 80"),
                count(Customer.class, filter));
    }

    @Test
    void primitiveIntComparisonWithDoubleValueFromNumberField() {
        // UI number fields may hand out Doubles; conversion must handle it
        var filter = group(FilterGroup.Logic.AND,
                condition("creditScore", int.class, FilterOperator.GTE, Double.valueOf(80.0)));
        assertEquals(
                jpqlCount("select count(c) from Customer c where c.creditScore >= 80"),
                count(Customer.class, filter));
    }

    @Test
    void temporalComparison() {
        var filter = group(FilterGroup.Logic.AND,
                condition("customerSince", LocalDate.class, FilterOperator.LT, LocalDate.of(2010, 1, 1)));
        assertEquals(
                jpqlCount("select count(c) from Customer c where c.customerSince < ?1", LocalDate.of(2010, 1, 1)),
                count(Customer.class, filter));
    }

    @Test
    void startsWith() {
        var filter = group(FilterGroup.Logic.AND,
                condition("companyName", String.class, FilterOperator.STARTS_WITH, "Berlin"));
        assertEquals(1, count(Customer.class, filter)); // Berlin Data Works
    }

    @Test
    void likeWildcardsAreEscaped() {
        // literal '100%' occurs nowhere; unescaped it would match anything containing "100"
        var filter = group(FilterGroup.Logic.AND,
                condition("companyName", String.class, FilterOperator.CONTAINS, "100%"));
        assertEquals(0, count(Customer.class, filter));
    }

    @Test
    void orGroupWithNestedAndGroup() {
        var filter = group(FilterGroup.Logic.OR,
                group(FilterGroup.Logic.AND,
                        condition("address.city", String.class, FilterOperator.CONTAINS, "berlin"),
                        condition("active", boolean.class, FilterOperator.IS_TRUE, null)),
                condition("creditScore", int.class, FilterOperator.GTE, 95));
        assertEquals(
                jpqlCount("select count(c) from Customer c where (lower(c.address.city) like '%berlin%' and c.active = true) or c.creditScore >= 95"),
                count(Customer.class, filter));
    }

    @Test
    void negatedGroup() {
        var inner = group(FilterGroup.Logic.AND,
                condition("address.city", String.class, FilterOperator.CONTAINS, "Berlin"),
                condition("active", boolean.class, FilterOperator.IS_TRUE, null));
        inner.setNegated(true);
        var filter = group(FilterGroup.Logic.AND, inner);
        assertEquals(100 - 15, count(Customer.class, filter));
    }

    @Test
    void emptyGroupMeansNoFiltering() {
        var empty = new FilterGroup();
        try (EntityManager em = emf.createEntityManager()) {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            Root<?> root = cb.createQuery(Customer.class).from(Customer.class);
            assertNull(FilterPredicateBuilder.toPredicate(empty, root, cb));
        }
        assertEquals(100, count(Customer.class, empty));
    }

    @Test
    void incompleteConditionIsSkipped() {
        var incomplete = new FilterCondition(); // no property/operator at all
        var missingValue = condition("companyName", String.class, FilterOperator.CONTAINS, null);
        var filter = group(FilterGroup.Logic.AND, incomplete, missingValue);
        assertTrue(filter.isEmpty());
        assertEquals(100, count(Customer.class, filter));
    }

    // ---- paged fetching (the grid's criteria listing path) ----

    @Test
    void criteriaListingPagesThroughFilteredResults() {
        var berlin = group(FilterGroup.Logic.AND,
                condition("address.city", String.class, FilterOperator.CONTAINS, "berlin"));
        FilterSpecification<Customer> spec = (root, cb) -> FilterPredicateBuilder.toPredicate(berlin, root, cb);
        try (EntityManager em = emf.createEntityManager()) {
            List<Customer> firstPage = CriteriaListing.fetch(em, Customer.class, spec, 0, 10);
            List<Customer> secondPage = CriteriaListing.fetch(em, Customer.class, spec, 10, 10);
            List<Customer> beyond = CriteriaListing.fetch(em, Customer.class, spec, 20, 10);

            assertEquals(10, firstPage.size());
            assertEquals(8, secondPage.size()); // 18 Berlin rows in total
            assertTrue(beyond.isEmpty());

            var ids = new java.util.HashSet<Long>();
            firstPage.forEach(c -> ids.add(c.getId()));
            secondPage.forEach(c -> ids.add(c.getId()));
            assertEquals(18, ids.size(), "pages must not overlap");
            firstPage.forEach(c -> assertEquals("Berlin", c.getAddress().getCity()));
        }
    }

    // ---- enum handling with Person ----

    @Test
    void enumEqualsAndNullChecks() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                var alice = new Person();
                alice.setFirstName("Alice");
                alice.setPreferredContactMethod(ContactMethod.EMAIL);
                var bob = new Person();
                bob.setFirstName("Bob");
                bob.setPreferredContactMethod(ContactMethod.PHONE);
                var carol = new Person();
                carol.setFirstName("Carol"); // no preference
                em.persist(alice);
                em.persist(bob);
                em.persist(carol);
                em.getTransaction().commit();

                var email = group(FilterGroup.Logic.AND,
                        condition("preferredContactMethod", ContactMethod.class, FilterOperator.EQUALS, ContactMethod.EMAIL));
                assertEquals(1, count(Person.class, email));

                var hasPreference = group(FilterGroup.Logic.AND,
                        condition("preferredContactMethod", ContactMethod.class, FilterOperator.IS_NOT_NULL, null));
                assertEquals(2, count(Person.class, hasPreference));

                var noPreference = group(FilterGroup.Logic.AND,
                        condition("preferredContactMethod", ContactMethod.class, FilterOperator.IS_NULL, null));
                assertEquals(1, count(Person.class, noPreference));
            } finally {
                em.getTransaction().begin();
                em.createQuery("delete from Person").executeUpdate();
                em.getTransaction().commit();
            }
        }
    }
}
