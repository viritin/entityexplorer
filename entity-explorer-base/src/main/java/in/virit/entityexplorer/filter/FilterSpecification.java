package in.virit.entityexplorer.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * A filter over entities of type {@code T}, expressed with the JPA Criteria
 * API. Similar in spirit to Spring Data's {@code Specification}, but with no
 * Spring dependency so it can live in this plain-JPA module.
 * <p>
 * Implementations are called for every fetch; the returned {@link Predicate}
 * must be built with the given {@code root} and {@code cb} and never cached,
 * as criteria objects are tied to the {@code CriteriaBuilder} they came from.
 */
@FunctionalInterface
public interface FilterSpecification<T> {

    /**
     * @return the predicate to filter with, or {@code null} for no filtering
     */
    Predicate toPredicate(Root<T> root, CriteriaBuilder cb);
}
