package in.virit.entityexplorer.filter.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.data.value.ValueChangeMode;
import in.virit.entityexplorer.filter.FilterProperty;
import org.vaadin.firitin.components.combobox.VComboBox;
import org.vaadin.firitin.components.datepicker.VDatePicker;
import org.vaadin.firitin.components.datetimepicker.VDateTimePicker;
import org.vaadin.firitin.components.textfield.VBigDecimalField;
import org.vaadin.firitin.components.textfield.VIntegerField;
import org.vaadin.firitin.components.textfield.VNumberField;
import org.vaadin.firitin.components.textfield.VTextField;
import org.vaadin.firitin.components.timepicker.VTimePicker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Consumer;

/**
 * Creates a value input field matching a filter property's java type. The
 * created field pushes its (typed) value to the given consumer, which writes
 * it into the {@code FilterCondition}.
 */
class ConditionValueFields {

    static final String LABEL = "Value";

    private ConditionValueFields() {
    }

    /**
     * @return a field for entering a comparison value, or {@code null} for
     * types whose operators never need a value (boolean)
     */
    static Component createFor(FilterProperty property, Consumer<Object> onValueChange) {
        Class<?> t = property.javaType();
        if (t == boolean.class || t == Boolean.class) {
            // is true / is false operators carry the value semantics
            return null;
        }
        // Viritin text-input fields default to a debounced LAZY value change
        // mode; ON_CHANGE keeps the model in sync deterministically when the
        // user tabs/clicks onward (and matches browser test expectations).
        if (t == String.class) {
            var field = new VTextField(LABEL);
            field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t == int.class || t == Integer.class || t == short.class || t == Short.class
                || t == byte.class || t == Byte.class) {
            var field = new VIntegerField(LABEL);
            field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t == long.class || t == Long.class || t == BigDecimal.class || t == BigInteger.class) {
            // BigDecimal input avoids double-precision loss on long values;
            // the predicate builder converts to the exact attribute type
            var field = new VBigDecimalField(LABEL);
            field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t == double.class || t == Double.class || t == float.class || t == Float.class) {
            var field = new VNumberField(LABEL);
            field.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t == LocalDate.class) {
            var field = new VDatePicker(LABEL);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t == LocalDateTime.class) {
            var field = new VDateTimePicker(LABEL);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t == LocalTime.class) {
            var field = new VTimePicker(LABEL);
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        if (t.isEnum()) {
            var field = new VComboBox<Object>(LABEL);
            field.setItems(t.getEnumConstants());
            field.addValueChangeListener(e -> onValueChange.accept(e.getValue()));
            return field;
        }
        // FilterProperty.listFor only offers types handled above
        throw new IllegalArgumentException("Unsupported filter property type: " + t);
    }
}
