package in.virit.entityexplorer.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link FilterGroup} tree into a JPA Criteria {@link Predicate}.
 * Incomplete conditions and (effectively) empty groups are silently skipped.
 */
public final class FilterPredicateBuilder {

    private FilterPredicateBuilder() {
    }

    /**
     * @return the predicate for the group, or {@code null} when the group
     * contains no complete conditions (meaning: no filtering)
     */
    public static Predicate toPredicate(FilterGroup group, Root<?> root, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        for (FilterNode child : group.getChildren()) {
            Predicate predicate = switch (child) {
                case FilterGroup nested -> toPredicate(nested, root, cb);
                case FilterCondition condition ->
                    condition.isComplete() ? toPredicate(condition, root, cb) : null;
            };
            if (predicate != null) {
                predicates.add(predicate);
            }
        }
        if (predicates.isEmpty()) {
            // never emit cb.and()/cb.or() on an empty array
            return null;
        }
        Predicate combined = group.getLogic() == FilterGroup.Logic.AND
                ? cb.and(predicates.toArray(Predicate[]::new))
                : cb.or(predicates.toArray(Predicate[]::new));
        return group.isNegated() ? cb.not(combined) : combined;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate toPredicate(FilterCondition condition, Root<?> root, CriteriaBuilder cb) {
        Path<?> path = resolvePath(root, condition.getProperty().path());
        // Convert the UI-provided value to the attribute's exact java type
        // before use; comparison correctness relies on this.
        Object value = convert(condition.getValue(), path.getJavaType());

        Predicate predicate = switch (condition.getOperator()) {
            case EQUALS -> cb.equal(path, value);
            // Plain SQL semantics: NULL rows don't match != (IS_NULL exists for those)
            case NOT_EQUALS -> cb.notEqual(path, value);
            case CONTAINS -> like(cb, path, "%" + escapeLike(String.valueOf(value)) + "%");
            case STARTS_WITH -> like(cb, path, escapeLike(String.valueOf(value)) + "%");
            case GT -> cb.greaterThan((Expression<Comparable>) path, (Comparable) value);
            case GTE -> cb.greaterThanOrEqualTo((Expression<Comparable>) path, (Comparable) value);
            case LT -> cb.lessThan((Expression<Comparable>) path, (Comparable) value);
            case LTE -> cb.lessThanOrEqualTo((Expression<Comparable>) path, (Comparable) value);
            case IS_TRUE -> cb.isTrue((Expression<Boolean>) path);
            case IS_FALSE -> cb.isFalse((Expression<Boolean>) path);
            case IS_NULL -> cb.isNull(path);
            case IS_NOT_NULL -> cb.isNotNull(path);
            case IS_EMPTY -> cb.or(cb.isNull(path), cb.equal((Expression<String>) path, ""));
        };
        return condition.isNegated() ? cb.not(predicate) : predicate;
    }

    private static Path<?> resolvePath(Root<?> root, String propertyPath) {
        Path<?> path = root;
        for (String part : propertyPath.split("\\.")) {
            path = path.get(part);
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    private static Predicate like(CriteriaBuilder cb, Path<?> path, String pattern) {
        // case-insensitive
        return cb.like(cb.lower((Expression<String>) path), pattern.toLowerCase(), '\\');
    }

    /** Escapes LIKE wildcards so user input is always matched literally. */
    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /**
     * Converts a value from a UI field to the target attribute type. Numeric
     * conversions are routed through BigDecimal so that e.g. a Double from a
     * number field can filter a long attribute.
     */
    private static Object convert(Object value, Class<?> target) {
        if (value == null) {
            return null;
        }
        Class<?> wrapped = wrap(target);
        if (wrapped.isInstance(value)) {
            return value;
        }
        if (Number.class.isAssignableFrom(wrapped) && value instanceof Number number) {
            BigDecimal bd = new BigDecimal(number.toString());
            if (wrapped == Integer.class) {
                return bd.intValue();
            } else if (wrapped == Long.class) {
                return bd.longValue();
            } else if (wrapped == Short.class) {
                return bd.shortValue();
            } else if (wrapped == Byte.class) {
                return bd.byteValue();
            } else if (wrapped == Double.class) {
                return bd.doubleValue();
            } else if (wrapped == Float.class) {
                return bd.floatValue();
            } else if (wrapped == BigInteger.class) {
                return bd.toBigInteger();
            } else if (wrapped == BigDecimal.class) {
                return bd;
            }
        }
        return value;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }
}
