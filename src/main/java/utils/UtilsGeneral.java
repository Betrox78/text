/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

/**
 *
 * @author Alonso --- alongo@kriblet.com at 16/04/2018
 */
public class UtilsGeneral {

    /**
     * imprime todas las propiedades en un objeto Properties
     *
     * @param props objeto con las propiedades a imprimir
     */
    public static void printProps(Properties props) {
        //impresion de props
        Enumeration<?> enums = props.propertyNames();
        String key;
        String prop;
        while (enums.hasMoreElements()) {
            key = (String) enums.nextElement();
            prop = props.getProperty(key);
            System.out.println(key + " = " + prop);
        }
    }

    public static JsonArray mergeAndSortJsonArrays(JsonArray array1, JsonArray array2, String dateField) {
        JsonArray combined = new JsonArray();
        array1.forEach(combined::add);
        array2.forEach(combined::add);

        combined = combined.stream()
                .map(JsonObject.class::cast)
                .sorted((o1, o2) -> {
                    try {
                        Date date1 = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(o1.getString(dateField));
                        Date date2 = UtilsDate.parse_yyyy_MM_dd_T_HH_mm_ss(o2.getString(dateField));
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        throw new RuntimeException("Error parsing dates for sorting", e);
                    }
                })
                .collect(JsonArray::new, JsonArray::add, JsonArray::addAll);

        return combined;
    }

}
