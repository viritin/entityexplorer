package in.virit.entityexplorer.filter;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

/**
 * Executes a paged criteria query for a {@link FilterSpecification}. Extracted
 * from the grid so the fetch logic can be tested with a plain EntityManager.
 * <p>
 * The criteria objects are created fresh on every call on purpose: predicates
 * are tied to the CriteriaBuilder they were created with and must never be
 * reused across queries or entity managers.
 */
public final class CriteriaListing {

    private CriteriaListing() {
    }

    public static <T> List<T> fetch(EntityManager em, Class<T> entityClass,
            FilterSpecification<T> specification, int offset, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        Predicate predicate = specification.toPredicate(root, cb);
        if (predicate != null) {
            cq.where(predicate);
        }
        return em.createQuery(cq)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
