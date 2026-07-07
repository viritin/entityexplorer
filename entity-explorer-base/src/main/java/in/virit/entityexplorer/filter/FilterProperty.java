package in.virit.entityexplorer.filter;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.SingularAttribute;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One filterable target of an entity: a direct basic-typed singular attribute
 * or a one-level path into an embedded attribute (e.g. {@code address.city}).
 *
 * @param path     the attribute path relative to the entity root, dot
 *                 separated for embedded attributes
 * @param label    human-readable label shown in the UI (same as path for now)
 * @param javaType the java type of the attribute
 * @param nullable whether the attribute is optional/nullable
 */
public record FilterProperty(String path, String label, Class<?> javaType, boolean nullable) {

    /**
     * Discovers the filterable properties of an entity from the JPA metamodel.
     * Associations and unsupported types are skipped; embedded attributes are
     * expanded one level deep. Derived getters without a backing persistent
     * attribute never appear here, as this is purely metamodel driven.
     */
    public static List<FilterProperty> listFor(EntityType<?> entityType) {
        List<FilterProperty> properties = new ArrayList<>();
        collect(entityType, "", properties);
        properties.sort(Comparator.comparing(FilterProperty::path));
        return properties;
    }

    private static void collect(ManagedType<?> type, String pathPrefix, List<FilterProperty> properties) {
        for (SingularAttribute<?, ?> attr : type.getSingularAttributes()) {
            if (attr.isAssociation()) {
                // v1 scope: no filtering through associations
                continue;
            }
            String path = pathPrefix + attr.getName();
            if (attr.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED
                    && pathPrefix.isEmpty()) {
                // expand embeddables one level deep (address.city etc.)
                collect((EmbeddableType<?>) attr.getType(), path + ".", properties);
            } else if (isSupportedBasicType(attr.getJavaType())) {
                properties.add(new FilterProperty(path, path, attr.getJavaType(), attr.isOptional()));
            }
        }
    }

    private static boolean isSupportedBasicType(Class<?> t) {
        return t == String.class
                || Number.class.isAssignableFrom(t)
                || t == int.class || t == long.class || t == short.class || t == byte.class
                || t == double.class || t == float.class
                || t == boolean.class || t == Boolean.class
                || t.isEnum()
                || t == BigDecimal.class || t == BigInteger.class
                || t == LocalDate.class || t == LocalDateTime.class
                || t == LocalTime.class;
    }
}
