package service.commons;

import io.vertx.core.json.JsonObject;
import utils.UtilsDate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static service.commons.Constants._ADDITIONAL_PROMISE_TIME;
import static service.commons.Constants._PROMISE_DELIVERY_DATE;

public class FestiveCalendar {
    
    public static Set<LocalDate> getFestiveDays(int year) {
        Set<LocalDate> festiveDays = new HashSet<>();

        festiveDays.add(LocalDate.of(year, Month.JANUARY, 1)); // año nuevo
        festiveDays.add(getFirstMondayOf(year, Month.FEBRUARY)); // dia de la constitucion
        festiveDays.add(getThirdMondayOf(year, Month.MARCH)); // natalicio de benito juarez
        festiveDays.add(LocalDate.of(year, Month.APRIL, 17)); // jueves santo 2025
        festiveDays.add(LocalDate.of(year, Month.APRIL, 18)); // viernes santo 2025
        festiveDays.add(LocalDate.of(year, Month.MAY, 1)); // dia del trabajo
        festiveDays.add(LocalDate.of(year, Month.SEPTEMBER, 16)); // dia de la independencia
        festiveDays.add(getThirdMondayOf(year, Month.NOVEMBER)); // dia de la revolucino
        festiveDays.add(LocalDate.of(year, Month.DECEMBER, 25)); // navidad

        // agregar el 1 de diciembre de cada 6 años a partir del 2024
        // cambio de presidente
        if ((year - 2024) % 6 == 0) {
            festiveDays.add(LocalDate.of(year, Month.DECEMBER, 1));
        }

        return festiveDays;
    }

    private static LocalDate getFirstMondayOf(int year, Month month) {
        LocalDate date = LocalDate.of(year, month, 1);
        while (date.getDayOfWeek() != DayOfWeek.MONDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private static LocalDate getThirdMondayOf(int year, Month month) {
        LocalDate date = LocalDate.of(year, month, 1);
        int mondays = 0;
        while (mondays < 3) {
            if (date.getDayOfWeek() == DayOfWeek.MONDAY) {
                mondays++;
            }
            if (mondays < 3) {
                date = date.plusDays(1);
            }
        }
        return date;
    }

    private static JsonObject addWorkingDays(LocalDateTime today, int hoursToAdd) {
        DayOfWeek documentationDay = today.getDayOfWeek();
        int daysToAdd = (int) Math.ceil(hoursToAdd / 24.0);
        int year = today.getYear();
        Set<LocalDate> festiveDays = getFestiveDays(year);
        long additionalHours = getHoursToNextBusinessDay(documentationDay, today, festiveDays);
        today = today.plusHours(additionalHours);

        // Si el año cambio, obtenemos los dias festivos del nuevo año
        if (today.getYear() != year) {
            year = today.getYear();
            festiveDays = getFestiveDays(year);
        }

        while(daysToAdd > 0) {
            today = today.plusDays(1);
            daysToAdd--;
            while(today.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    today.getDayOfWeek() == DayOfWeek.SUNDAY ||
                    festiveDays.contains(today.toLocalDate())) {
                today =today.plusDays(1);
                additionalHours += 24;
            }
        }

        return new JsonObject()
                .put(_PROMISE_DELIVERY_DATE, UtilsDate.localDateTimeToStringDate(today))
                .put(_ADDITIONAL_PROMISE_TIME, additionalHours);
    }

    private static long getHoursToNextBusinessDay(DayOfWeek documentationDay, LocalDateTime today, Set<LocalDate> festiveDays) {
        long hours = 0;
        while((documentationDay != DayOfWeek.SATURDAY && today.getDayOfWeek() == DayOfWeek.SATURDAY) ||
                today.getDayOfWeek() == DayOfWeek.SUNDAY ||
                festiveDays.contains(today.toLocalDate())) {
            today = today.plusDays(1);
            hours += 24;
        }
        return hours;
    }

    public static JsonObject getPromiseDeliveryDate(String date, int hoursToAdd) {
        LocalDateTime today = LocalDateTime.now();

        // Definir la zona de origen y la nueva zona
        ZoneId zonaOrigen = ZoneId.systemDefault(); // Zona actual
        ZoneId zonaDestino = ZoneId.of(UtilsDate.TIME_ZONE); // Nueva zona horaria

        // Convertir LocalDateTime a ZonedDateTime en la zona original
        ZonedDateTime zonedDateTime = today.atZone(zonaOrigen);

        // Convertirlo a la nueva zona horaria y obtener el LocalDateTime
        LocalDateTime convertedToday = zonedDateTime.withZoneSameInstant(zonaDestino).toLocalDateTime();

        if (Objects.nonNull(date)) {
            DateTimeFormatter formatter;
            if (date.length() == 10) { // "yyyy-MM-dd"
                date += " 00:00:00";
            }
            formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            convertedToday = LocalDateTime.parse(date, formatter);
        }
        return addWorkingDays(convertedToday, hoursToAdd);
    }

    public static int getBusinessDifferenceHours(LocalDate date, LocalDate dateCompare) {
        int year = date.getYear();
        Set<LocalDate> festiveDays = getFestiveDays(year);
        long differenceHours = ChronoUnit.DAYS.between(date, dateCompare) * 24;
        int hoursToSubstract = 0;
        while (date.isBefore(dateCompare)) {
            if(date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                    date.getDayOfWeek() == DayOfWeek.SUNDAY ||
                    festiveDays.contains(date)) {
                hoursToSubstract += 24;
            }
            date = date.plusDays(1);
        }
        return (int) (differenceHours - hoursToSubstract);
    }
}
