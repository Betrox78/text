/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils for valitation of data
 *
 * @author Ulises Beltrán Gómez - beltrangomezulises@gmail.com
 */
public class UtilsValidation {

    public static final String MISSING_REQUIRED_VALUE = "missing required value";
    public static final String INVALID_FORMAT = "invalid format";
    public static final String INVALID_PARAMETER = "invalid parameter";
    public static final String PARAMETER_DOES_NOT_EXIST = "field does not exist";
    public static final String ALREADY_EXISTS = "field already exists";
    private static final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static final String EMAIL_MULTIPLE_PATTERN = "^(([_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@)([A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,}))[,]*)+$";
    private static final String PHONE_NUMBER_PATTERN = "^[0-9]*$";
    private static final String SPECIAL_CHARS = "/*!@#$%^&*()\"{}_[]|\\?/<>,.";
    private static final String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
    private static final String TIME_HOURS_PATTERN = "([01]?[0-9]|[0-9][0-9]):[0-5][0-9]";
    private static final String DATE = "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$";
    private static final String DATETIME = "^(((\\d{4})(-)(0[13578]|10|12)(-)(0[1-9]|[12][0-9]|3[01]))|((\\d{4})(-)(0[469]|11)(-)([0][1-9]|[12][0-9]|30))|((\\d{4})(-)(02)(-)(0[1-9]|1[0-9]|2[0-8]))|(([02468]\u200C\u200B[048]00)(-)(02)(-)(29))|(([13579][26]00)(-)(02)(-)(29))|(([0-9][0-9][0][48])(-)(0\u200C\u200B2)(-)(29))|(([0-9][0-9][2468][048])(-)(02)(-)(29))|(([0-9][0-9][13579][26])(-)(02\u200C\u200B)(-)(29)))(\\s([0-1][0-9]|2[0-4]):([0-5][0-9]):([0-5][0-9]))$";
    private static final String DECIMAL_PATTERN = "^(\\d{1,9})\\.([0-9]{1,2})";
    private static final String HAS_NUMBER = ".*\\d+.*";
    private static final String PASSWORD = "^(?=.*\\d)(?=.*[a-z])[\\w~@#$%^&*+=`'<>|{}:;!.?\\\"()\\[\\]-]{8,}$";
     //private static final String PASSWORD = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[\\w~@#$%^&*+=`'<>|{}:;!.?\\\"()\\[\\]-]{8,}$";
    private static final String MUST_BE_ACTIVE = "Branch must be active";
    private static final String MUST_BE_DIFFERENT = "The values must be different";

    /**
     * Validates that the property propertyName in the JsonObject object is an
     * email with the pattern
     * "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isMail(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        evaluate(EMAIL_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is an
     * decimal with the pattern
     * "([0-9]{0,12})+(\.[0-9]{0,2})$",
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws utils.UtilsValidation.PropertyValueException if the evaluation
     * fails
     */
    public static void isDecimal(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        try {
            Double f = object.getDouble(propertyName);
            if (f == null) {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
            Matcher matcher = Pattern.compile(DECIMAL_PATTERN).matcher(String.valueOf(f));
            if (!matcher.matches()) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * boolean (true/false, 1/0)
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBoolean(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        //validate true false
        try {
            object.getBoolean(propertyName);
        } catch (ClassCastException e) {
            try {
                Integer integer = object.getInteger(propertyName);
                if (integer != null) {
                    if (integer == 1 || integer == 0) {
                        return;
                    }
                }
            } catch (ClassCastException ex) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * phone number with the pattern: "^[0-9]*$", ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isPhoneNumber(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        evaluate(PHONE_NUMBER_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * date with the pattern:
     * "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$" , ignoring
     * null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isDate(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        evaluate(DATE, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * date with the pattern:
     * "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$" , ignoring
     * null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isDateTime(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        evaluate(DATETIME, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|2[0-3]):[0-5][0-9]", ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHour24(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        evaluate(TIME24HOURS_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|[0-9][0-9]):[0-5][0-9]", ignoring
     * null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHour(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        evaluate(TIME_HOURS_PATTERN, object, propertyName);
    }

    /**
     * Validates that the text contains special character and do not be empty,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void containsSpecialCharacter(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
                for (int i = 0; i < s.length(); i++) {
                    if (SPECIAL_CHARS.contains(s.substring(i, (i + 1)))) {
                        throw new PropertyValueException(propertyName, INVALID_FORMAT);
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the property matches with a personal name, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isName(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
                if (s.contains("\"") || s.contains("'")) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
                Matcher matcher = Pattern.compile(HAS_NUMBER).matcher(s);
                if (matcher.matches()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the int value in the property is between the range from-to
     * including from and to values, ignoring null
     *
     * @param from initial value to start the range
     * @param to final value to end the range
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBetweenRange(final JsonObject object,
            final String propertyName, final int from, final int to)
            throws PropertyValueException {
        try {
            Integer val = object.getInteger(propertyName);
            if (val != null) {
                if (val < from && val > to) { //out of range
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the value in the property is one of the values provided,
     * ignoring null
     *
     * @param values values to check equality
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isContained(final JsonObject object, final String propertyName, String... values)
            throws PropertyValueException {
        try {
            if (object.getString(propertyName) != null){
                String propertyValue = object.getString(propertyName);
                if (propertyValue != null) {
                    for (String value : values) {
                        if (value.equals(propertyValue)) {
                            return;
                        }
                    }
                    throw new PropertyValueException(propertyName, INVALID_FORMAT + ", values only acepted are: " + Arrays.toString(values));
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the int value in the property is grater than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGrater(final JsonObject object,
            final String propertyName, final int value)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue > value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT + ", has to be grater than " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is grater than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGrater(final JsonObject object,
            final String propertyName, final double value)
            throws PropertyValueException {
        try {
            final Double propertyValue = object.getDouble(propertyName);
            if (propertyValue != null && propertyValue <= value) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT + ", has to be grater than " + value);
            }

        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is grater or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterEqual(final JsonObject object,
            final String propertyName, final int value)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT + ", has to be grater than or equal to " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLower(final JsonObject object, final String propertyName,
            final int value) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue < value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT + ", has to be lower than " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLowerEqual(final JsonObject object,
            final String propertyName, final int value) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT + ", has to be lower than or equal to " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is an
     * email with the pattern
     * "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isMailAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        evaluateAndNotNull(EMAIL_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is an
     * email with the pattern
     * "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isMultipleMailAndNotNull(final JsonObject object,
                                        final String propertyName) throws PropertyValueException {
        evaluateAndNotNull(EMAIL_MULTIPLE_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * phone number with the pattern: "^[0-9]*$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isPhoneNumberAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        evaluateAndNotNull(PHONE_NUMBER_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * date with the pattern:
     * "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isDateAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        evaluateAndNotNull(DATE, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|2[0-3]):[0-5][0-9]"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHour24AndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        evaluateAndNotNull(TIME24HOURS_PATTERN, object, propertyName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|[0-9][0-9]):[0-5][0-9]"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHourAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        evaluateAndNotNull(TIME_HOURS_PATTERN, object, propertyName);
    }

    /**
     * Validates that the text contains special character and do not be empty,
     * considering not null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void containsSpecialCharacterAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
            for (int i = 0; i < s.length(); i++) {
                if (SPECIAL_CHARS.contains(s.substring(i, (i + 1)))) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the text contains special character and do not be empty,
     * considering not null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isPasswordAndNotNull(final JsonObject object,
                                                          final String propertyName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
            Matcher matcher = Pattern.compile(PASSWORD).matcher(s);
            if (!matcher.matches()) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property matches with a personal name
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isNameAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
                if (s.contains("\"") || s.contains("'")) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
                Matcher matcher = Pattern.compile(HAS_NUMBER).matcher(s);
                if (matcher.matches()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is between the range from-to
     * including from and to values
     *
     * @param from initial value to start the range
     * @param to final value to end the range
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBetweenRangeAndNotNull(final JsonObject object,
            final String propertyName, final int from, final int to) throws PropertyValueException {
        try {
            Integer val = object.getInteger(propertyName);
            if (val != null) {
                if (val < from && val > to) { //out of range
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the value in the property is one of the values provided
     *
     * @param values values to check equality
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isContainedAndNotNull(final JsonObject object,
            final String propertyName, String... values) throws PropertyValueException {
        try {
            String propertyValue = object.getString(propertyName);
            if (propertyValue != null) {
                for (String value : values) {
                    if (value.equals(propertyValue)) {
                        return;
                    }
                }
                throw new PropertyValueException(propertyName, INVALID_FORMAT + ", values only acepted are: " + Arrays.toString(values));
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that a string has almost 1 character
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isEmptyAndNotNull(final JsonObject object,
            final String propertyName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    public static void isEmptyAndNotNull(final JsonArray object,
            final String nameToDisplay) throws PropertyValueException {
        if (object == null) {
            throw new PropertyValueException(nameToDisplay, MISSING_REQUIRED_VALUE);
        }
        if (object.isEmpty()) {
            throw new PropertyValueException(nameToDisplay, MISSING_REQUIRED_VALUE);
        }
    }

    public static void isNotNull(final Object object,
            final String nameToDisplay) throws PropertyValueException {
        if (object == null) {
            throw new PropertyValueException(nameToDisplay, MISSING_REQUIRED_VALUE);
        }
    }

    /**
     * Validates that a string has almost 1 character, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isEmpty(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the int value in the property is grater than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterAndNotNull(final JsonObject object,
            final String propertyName, final double value) throws PropertyValueException {
        try {
            final Double propertyValue = object.getDouble(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue > value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is grater than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterAndNotNull(final JsonObject object,
            final String propertyName, final int value) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue > value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is grater or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterEqualAndNotNull(final JsonObject object,
            final String propertyName, final int value) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLowerAndNotNull(final JsonObject object,
            final String propertyName, final int value) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue < value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLowerEqualAndNotNull(final JsonObject object,
            final String propertyName, final int value) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is an
     * email with the pattern
     * "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$",
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isMail(final JsonObject object, final String propertyName,
            final String modelName) throws PropertyValueException {
        evaluate(EMAIL_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * boolean (true/false, 1/0)
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBoolean(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        //validate true false
        try {
            object.getBoolean(propertyName);
        } catch (ClassCastException e) {
            try {
                Integer integer = object.getInteger(propertyName);
                if (integer != null) {
                    if (integer == 1 || integer == 0) {
                        return;
                    }
                }
            } catch (ClassCastException ex) {
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
            }
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * phone number with the pattern: "^[0-9]*$", ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isPhoneNumber(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        evaluate(PHONE_NUMBER_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * date with the pattern:
     * "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$" , ignoring
     * null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isDate(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        evaluate(DATE, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|2[0-3]):[0-5][0-9]", ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHour24(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        evaluate(TIME24HOURS_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|[0-9][0-9]):[0-5][0-9]", ignoring
     * null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHour(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        evaluate(TIME_HOURS_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the text contains special character and do not be empty,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void containsSpecialCharacter(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
                for (int i = 0; i < s.length(); i++) {
                    if (SPECIAL_CHARS.contains(s.substring(i, (i + 1)))) {
                        throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                    }
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the property matches with a personal name, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isName(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
                if (s.contains("\"") || s.contains("'")) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
                Matcher matcher = Pattern.compile(HAS_NUMBER).matcher(s);
                if (matcher.matches()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the int value in the property is between the range from-to
     * including from and to values, ignoring null
     *
     * @param from initial value to start the range
     * @param to final value to end the range
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBetweenRange(final JsonObject object,
            final String propertyName, final int from, final int to,
            final String modelName)
            throws PropertyValueException {
        try {
            Integer val = object.getInteger(propertyName);
            if (val != null) {
                if (val < from && val > to) { //out of range
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the value in the property is one of the values provided,
     * ignoring null
     *
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @param values values to check equality
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isContained(final String modelName, final JsonObject object, final String propertyName, String... values)
            throws PropertyValueException {
        try {
            String propertyValue = object.getString(propertyName);
            if (propertyValue != null) {
                for (String value : values) {
                    if (value.equals(propertyValue)) {
                        return;
                    }
                }
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT + ", values only acepted are: " + Arrays.toString(values));
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the int value in the property is grater than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGrater(final JsonObject object,
            final String propertyName, final int value, final String modelName)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue > value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT + ", has to be grater than " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is grater or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterEqual(final JsonObject object,
            final String propertyName, final int value, final String modelName)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT + ", has to be grater than or equal to " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLower(final JsonObject object, final String propertyName,
            final int value, final String modelName) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue < value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT + ", has to be lower than " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLowerEqual(final JsonObject object,
            final String propertyName, final int value, final String modelName)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT + ", has to be lower than or equal to " + value);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is an
     * email with the pattern
     * "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isMailAndNotNull(final JsonObject object,
            final String propertyName, final String modelName)
            throws PropertyValueException {
        evaluateAndNotNull(EMAIL_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * phone number with the pattern: "^[0-9]*$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isPhoneNumberAndNotNull(final JsonObject object,
            final String propertyName, final String modelName) throws PropertyValueException {
        evaluateAndNotNull(PHONE_NUMBER_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * date with the pattern:
     * "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isDateAndNotNull(final JsonObject object,
            final String propertyName, final String modelName) throws PropertyValueException {
        evaluateAndNotNull(DATE, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * date with the pattern:
     * "^((19|20)\\d\\d)-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isDateTimeAndNotNull(final JsonObject object,
                                        final String propertyName, final String modelName) throws PropertyValueException {
        evaluateAndNotNull(DATETIME, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|2[0-3]):[0-5][0-9]"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHour24AndNotNull(final JsonObject object,
            final String propertyName, final String modelName)
            throws PropertyValueException {
        evaluateAndNotNull(TIME24HOURS_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * hour with the pattern: "([01]?[0-9]|[0-9][0-9]):[0-5][0-9]"
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isHourAndNotNull(final JsonObject object,
            final String propertyName, final String modelName)
            throws PropertyValueException {
        evaluateAndNotNull(TIME_HOURS_PATTERN, object, propertyName, modelName);
    }

    /**
     * Validates that the text contains special character and do not be empty,
     * considering not null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void containsSpecialCharacterAndNotNull(final JsonObject object,
            final String propertyName, final String modelName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
            }
            for (int i = 0; i < s.length(); i++) {
                if (SPECIAL_CHARS.contains(s.substring(i, (i + 1)))) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property matches with a personal name
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isNameAndNotNull(final JsonObject object,
            final String propertyName, final String modelName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
                if (s.contains("\"") || s.contains("'")) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
                Matcher matcher = Pattern.compile(HAS_NUMBER).matcher(s);
                if (matcher.matches()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is between the range from-to
     * including from and to values
     *
     * @param from initial value to start the range
     * @param to final value to end the range
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBetweenRangeAndNotNull(final JsonObject object,
            final String propertyName, final int from, final int to,
            final String modelName) throws PropertyValueException {
        try {
            Integer val = object.getInteger(propertyName);
            if (val != null) {
                if (val < from && val > to) { //out of range
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the value in the property is one of the values provided
     *
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @param values values to check equality
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isContainedAndNotNull(final String modelName, final JsonObject object,
            final String propertyName, String... values) throws PropertyValueException {
        try {
            String propertyValue = object.getString(propertyName);
            if (propertyValue != null) {
                for (String value : values) {
                    if (value.equals(propertyValue)) {
                        return;
                    }
                }
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT + ", values only acepted are: " + Arrays.toString(values));
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that a string has almost 1 character
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isEmptyAndNotNull(final JsonObject object,
            final String propertyName, final String modelName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that a string has almost 1 character
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isEmptyAndNotNull(final JsonArray object,
            final String propertyName, final String modelName) throws PropertyValueException {
        if (object == null) {
            throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
        }
        if (object.isEmpty()) {
            throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
        }
    }

    /**
     * Validates that a string has almost 1 character
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isEmpty(final JsonArray object,
            final String propertyName) throws PropertyValueException {
        if (Objects.nonNull(object) && object.isEmpty()) {
            throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
        }
    }

    /**
     * Validates that a string has almost 1 character, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isEmpty(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }

    }

    /**
     * Validates that the int value in the property is grater than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterAndNotNull(final JsonObject object,
            final String propertyName, final int value, final String modelName) throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue > value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }
    public static void isGraterAndNotNull(final JsonObject object,
            final String propertyName, final double value, final String modelName) throws PropertyValueException {
        try {
            final Double propertyValue = object.getDouble(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue > value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is grater or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws  PropertyValueException if the evaluation
     * fails
     */
    public static void isGraterEqualAndNotNull(final JsonObject object,
            final String propertyName, final int value, final String modelName)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    public static void isGraterEqualAndNotNull(final JsonObject object,
            final String propertyName, final double value, final String modelName)
            throws PropertyValueException {
        try {
            final Double propertyValue = object.getDouble(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower than th value,
     * ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLowerAndNotNull(final JsonObject object,
            final String propertyName, final int value, final String modelName)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue < value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the int value in the property is lower or equal than th
     * value, ignoring null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param value reference value to comptare with
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isLowerEqualAndNotNull(final JsonObject object,
            final String propertyName, final int value, final String modelName)
            throws PropertyValueException {
        try {
            final Integer propertyValue = object.getInteger(propertyName);
            if (propertyValue != null) {
                if (!(propertyValue >= value)) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            } else {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validate the value of the property in the object with the pattern given
     * as regular expresion, includes null validation, ignoring null
     *
     * @param pattern pattern to compile
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    private static void evaluate(final String pattern, JsonObject object,
            String propertyName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
                Matcher matcher = Pattern.compile(pattern).matcher(s);
                if (!matcher.matches()) {
                    throw new PropertyValueException(propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validate the value of the property in the object with the pattern given
     * as regular expresion, includes null validation
     *
     * @param pattern pattern to compile
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    private static void evaluateAndNotNull(final String pattern, JsonObject object,
            String propertyName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
            }
            Matcher matcher = Pattern.compile(pattern).matcher(s);
            if (!matcher.matches()) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validate the value of the property in the object with the pattern given
     * as regular expresion, includes null validation, ignoring null
     *
     * @param pattern pattern to compile
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    private static void evaluate(final String pattern, JsonObject object,
            final String propertyName, final String modelName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s != null) {
                if (s.isEmpty()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
                Matcher matcher = Pattern.compile(pattern).matcher(s);
                if (!matcher.matches()) {
                    throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
                }
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validate the value of the property in the object with the pattern given
     * as regular expresion, includes null validation
     *
     * @param pattern pattern to compile
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    private static void evaluateAndNotNull(final String pattern, JsonObject object,
            final String propertyName, final String modelName) throws PropertyValueException {
        try {
            String s = object.getString(propertyName);
            if (s == null) {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
            if (s.isEmpty()) {
                throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
            }
            Matcher matcher = Pattern.compile(pattern).matcher(s);
            if (!matcher.matches()) {
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
            }
        } catch (ClassCastException e) {
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * boolean (true/false, 1/0) considering null
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBooleanAndNotNull(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        //validate true false
        if (object.getValue(propertyName) == null) {
            throw new PropertyValueException(propertyName, MISSING_REQUIRED_VALUE);
        }
        try {
            object.getBoolean(propertyName);
        } catch (ClassCastException e) {
            try {
                Integer integer = object.getInteger(propertyName);
                if (integer != null) {
                    if (integer == 1 || integer == 0) {
                        return;
                    }
                }
            } catch (ClassCastException ex) {
                throw new PropertyValueException(propertyName, INVALID_FORMAT);
            }
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * boolean (true/false, 1/0)
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * @param modelName sub-name to concat as prefix to the property name in the
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isBooleanAndNotNull(final JsonObject object, final String propertyName,
            final String modelName)
            throws PropertyValueException {
        //validate true false
        if (object.getValue(propertyName) == null) {
            throw new PropertyValueException(modelName + "." + propertyName, MISSING_REQUIRED_VALUE);
        }
        try {
            object.getBoolean(propertyName);
        } catch (ClassCastException e) {
            try {
                Integer integer = object.getInteger(propertyName);
                if (integer != null) {
                    if (integer == 1 || integer == 0) {
                        return;
                    }
                }
            } catch (ClassCastException ex) {
                throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
            }
            throw new PropertyValueException(modelName + "." + propertyName, INVALID_FORMAT);
        }
    }

    /**
     * Validates that the property propertyName in the JsonObject object is a
     * array
     *
     * @param object object to evaluate
     * @param propertyName name of the property to evaluate
     * exception message
     * @throws PropertyValueException if the evaluation
     * fails
     */
    public static void isArray(final JsonObject object, final String propertyName)
            throws PropertyValueException {
        try {
            object.getJsonArray(propertyName);
        } catch (ClassCastException e) {
            throw new PropertyValueException(propertyName, INVALID_FORMAT);
        }
    }

    public static void isStatusActive(final JsonObject object, final String propertyName) throws PropertyValueException {
        if(object.getInteger(propertyName) != 1) throw new PropertyValueException(propertyName, MUST_BE_ACTIVE);
    }

    public static void differentValues(final JsonObject object, final String propertyName1, final String propertyName2) throws PropertyValueException {
        if(object.getInteger(propertyName1).equals(object.getInteger(propertyName2)))
            throw new PropertyValueException(propertyName1.concat(" - ").concat(propertyName2), MUST_BE_DIFFERENT);
    }

    public static boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return email != null && email.matches(emailRegex);
    }

    public static class PropertyValueException extends Exception {

        private static final long serialVersionUID = 5766879594293856607L;
        private String name;
        private String error;

        public PropertyValueException(String name, String error) {
            this.name = name;
            this.error = error;
        }

        public PropertyValueException(String string) {
            super(string);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public String getMessage(){
            return name + ": " + error;
        }

    }

}
