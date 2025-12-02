package utils;

import org.joda.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * utileria de conversion de fechas, herramienta de manejo de fechas donde
 * incluye suma de tiempo, días y conversiones diferentes formatos de
 * presentación
 *
 * @author Ulises Beltrán Gómez --- beltrangomezulises@gmail.com
 */
public class UtilsDate {

    private static final SimpleDateFormat SDF_D_MM_YYYY = new SimpleDateFormat("d/MM/yyyy");
    private static final SimpleDateFormat SDF_D_MM_YYYY_HH_MM = new SimpleDateFormat("d/MM/yyyy HH:mm");
    private static final SimpleDateFormat SDF_DD_MM_YYYY = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat SDF_MM_DD_YYYY = new SimpleDateFormat("MM/dd/yyyy");
    private static final SimpleDateFormat SDF_HM = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat SDF_HMS = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SDF_NDOW = new SimpleDateFormat("EEEE");
    private static final SimpleDateFormat SDF_UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final SimpleDateFormat SDF_YYYY_MM_DD_T_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat SDF_YYYY_MM_DD_T_HH_MM = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
    private static final SimpleDateFormat SDF_UTC_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final SimpleDateFormat SDF_DATABASE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat SDF_YYYY_MM_DD_HH_MM = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static final SimpleDateFormat SDF_YYYY_M_DD_HH_MM_SS = new SimpleDateFormat("yyyy-M-d HH:mm:ss");
    private static final SimpleDateFormat SDF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat SDF_YYYY_MM = new SimpleDateFormat("yyyy-MM");
    private static final SimpleDateFormat SDF_es_MX_dd_MMMM_yyyy_HH_mm = new SimpleDateFormat("'al' dd 'de' MMMM 'del' yyyy 'a las' HH:mm 'hrs'", new Locale("es", "MX"));
    private static final SimpleDateFormat SDF_D_MM = new SimpleDateFormat("d-MM");
    private static final SimpleDateFormat SDF_YYYY = new SimpleDateFormat("yyyy");
    private static final SimpleDateFormat SDF_MM = new SimpleDateFormat("MM");
    public static final TimeZone serverTimezone = Calendar.getInstance().getTimeZone();
    public static final String TIME_ZONE = "America/Mazatlan";
    public static final TimeZone timezone = TimeZone.getTimeZone(TIME_ZONE);

    /**
     * Return date with time zone offset
     * @return
     */
    public static Date getDateConvertedTimeZone(TimeZone timezone, Date date) throws ParseException{
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        df.setTimeZone(timezone);
        return SDF_DATABASE.parse(df.format(cal.getTime()));
    }

    public static String getTimeZoneValue(){
        int millis = timezone.getRawOffset();
        int hours   = (millis / (1000*60*60)) % 24;
        int minutes = (millis / (1000*60)) % 60;

        if (millis >= 0){
            return "+".concat(String.format("%02d", hours) + ":"+ String.format("%02d", minutes));
        } else {
            return "-".concat(String.format("%02d", hours * -1) + ":"+ String.format("%02d", minutes));
        }
    }

    /**
     * sumatoria de tiempo a la fecha actual
     *
     * @param reference Calendar.(medida de tiempo)
     * @param quantity Cantidad de unidades
     * @return Partiendo de la fecha actual mas el resultado de la operación en formato Date
     */
    public static Date summCalendar(Integer reference, Integer quantity){
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(reference, cal.get(reference) + quantity);
        return cal.getTime();
    }

    public static Date summCalendar(Date dateReference, Integer reference, Integer quantity){
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateReference);
        cal.set(reference, cal.get(reference) + quantity);
        return cal.getTime();
    }

    public static Date getStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 0, 0, 0);
        return calendar.getTime();
    }

    public static Date getEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }

    /**
     * sumatoria de tiempo en formato texto HH:mm
     *
     * @param tiempos lista de tiempos a sumar
     * @return la cantidad de horas y minutos en formato texto que resulta de
     * sumar el parametro tiempos
     */
    public static String sumatoriaDeTiempos(List<String> tiempos) {
        String res = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("d/MM/yyyy HH:mm");
            SimpleDateFormat sdfh = new SimpleDateFormat("HH:mm");
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(sdf.parse("1/01/2000 00:00:00"));
            for (String tiempo : tiempos) {
                String[] horasMinutos = tiempo.split(":");
                cal.add(Calendar.HOUR_OF_DAY, Integer.valueOf(horasMinutos[0]));
                cal.add(Calendar.MINUTE, Integer.valueOf(horasMinutos[1]));
            }

            res = sdfh.format(cal.getTime());
        } catch (ParseException ex) {
            Logger.getLogger(UtilsDate.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    /**
     * convierte date en su representacion texto en formato d/MM/yyyy
     *
     * @param date fecha a convertir
     * @return texto de la fecha
     */
    public static String format_D_MM_YYYY(Date date) {
        return SDF_D_MM_YYYY.format(date);
    }

    public static String format_DD_MM_YYYY(Date date) {
        return SDF_DD_MM_YYYY.format(date);
    }

    public static String format_MM_DD_YYYY(Date date) {
        return SDF_MM_DD_YYYY.format(date);
    }

    /**
     * convierte date en su representacion texto en formato d/MM/yyyy HH:mm
     *
     * @param date fecha a convertir
     * @return
     */
    public static String format_D_MM_YYYY_HH_MM(Date date) {
        return SDF_D_MM_YYYY_HH_MM.format(date);
    }

    /**
     * convierte date en su representacion texto en formato yyyy-MM-dd HH:mm
     *
     * @param date fecha a convertir
     * @return
     */
    public static String format_YYYY_MM_DD_HH_MM(Date date) {
        return SDF_YYYY_MM_DD_HH_MM.format(date);
    }

    /**
     * convierte date en su representacion texto en formato yyyy-M-dd HH:mm:ss
     *
     * @param date fecha a convertir
     * @return
     */
    public static String format_YYYY_M_DD_HH_MM_SS(Date date) {
        return SDF_YYYY_M_DD_HH_MM_SS.format(date);
    }

    /**
     * convierte date en su representacion texto en formato HH:mm
     *
     * @param date fecha a convertir
     * @return
     */
    public static String format_HH_MM(Date date) {
        return SDF_HM.format(date);
    }

    /**
     * convierte date en su representacion texto en formato HH:mm
     *
     * @param date fecha a convertir
     * @return
     */
    public static String format_HH_MM_SS(Date date) {
        return SDF_HMS.format(date);
    }


    /**
     * convierte date en su representacion texto en formato
     * yyyy-MM-dd'T'HH:mm:ss.SSSZ
     *
     * @param date fecha a convertir
     * @return
     */
    public static String sdfUTC(Date date) {
        return SDF_UTC.format(date);
    }

    /**
     * convierte date en su representacion texto en formato yyyy-MM-dd HH:mm:ss
     *
     * @param date fecha a convertir
     * @return texto representativo de la fecha
     */
    public static String sdfDataBase(Date date) {
        return SDF_DATABASE.format(date);
    }

    /**
     * obtiene el nombre del dia de la semana de la fecha otorgada en el
     * parametro "date" en formato texto "EEEE"
     *
     * @param date fecha de la cuel obtener el día
     * @param DateFormat formato de texto en el que se encuentra la fecha
     * @return texto con el nombre del dáa de la semana
     * @throws ParseException
     */
    public static String nameDayOfWeek(String date, String DateFormat) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
        return nameDayOfWeek(sdf.parse(date));
    }

    /**
     * obtiene el nombre del dia de la semana de la fecha otorgada en el
     * parametro date en formato texto "EEEE"
     *
     * @param date
     * @return
     * @throws ParseException
     */
    public static String nameDayOfWeek(String date) throws ParseException {
        return nameDayOfWeek(SDF_NDOW.parse(date));
    }

    /**
     * obtiene el nombre del dia de la semana de la fecha otorgada en el
     * parametro date en formato texto "EEEE"
     *
     * @param date
     * @return
     */
    public static String nameDayOfWeek(Date date) {
        return SDF_NDOW.format(date);
    }

    /**
     * obtiene la fecha del día que corresponda a el lunes anterior de la fecha
     * proporsionada en texto
     *
     * @param fecha fecha proporsionada en texto en formato d/MM/yyyy
     * @return
     */
    public static Date lunesAnterior(String fecha) {
        GregorianCalendar cal = new GregorianCalendar();
        try {
            cal.setTime(SDF_D_MM_YYYY.parse(fecha));
        } catch (Exception e) {
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { //mientras sea mayor que lunes
            cal.add(Calendar.DATE, -1); //restar un dia
        }
        return cal.getTime();
    }

    /**
     * obtiene la fecha del día que corresponda a el lunes posterior de la fecha
     * proporsionada en texto
     *
     * @param fecha fecha proporsionada en texto en formato d/MM/yyyy
     * @return
     */
    public static Date lunesPosterior(String fecha) {
        GregorianCalendar cal = new GregorianCalendar();
        try {
            cal.setTime(SDF_D_MM_YYYY.parse(fecha));
        } catch (ParseException e) {
            try {
                cal.setTime(SDF_YYYY_MM_DD.parse(fecha));
            } catch (ParseException ex) {

            }
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { //mientras sea mayor que lunes
            cal.add(Calendar.DATE, 1); //restar un dia
        }
        return cal.getTime();
    }

    /**
     * obtiene la fecha del día que corresponda a el lunes anterior de la fecha
     * proporsionada
     *
     * @param fecha fecha proporsionada
     * @return
     */
    public static Date lunesAnterior(Date fecha) {
        GregorianCalendar cal = new GregorianCalendar();
        try {
            cal.setTime(fecha);
        } catch (Exception e) {
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) { //mientras sea mayor que lunes
            cal.add(Calendar.DATE, -1); //restar un dia
        }
        return cal.getTime();
    }

    /**
     * obtiene la fecha del día que corresponda a el domingo posterior de la
     * fecha proporsionada
     *
     * @param fecha
     * @return
     */
    public static Date domingoPosterior(Date fecha) {
        GregorianCalendar cal = new GregorianCalendar();
        try {
            cal.setTime(fecha);
        } catch (Exception e) {
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) { //mientras sea mayor que lunes
            cal.add(Calendar.DATE, 1); //restar un dia
        }
        return cal.getTime();
    }

    /**
     * verifica si una fecha pertenece a la misma semana de otra fecha
     * proporsionada
     *
     * @param date fecha a la cual verficar en formato "d/MM/yyyy"
     * @param week fecha de la cual tomar la semana en formato "d/MM/yyyy"
     * @return true si la fecha pertenece a la misma semana
     */
    public static boolean belongsDateToWeek(String date, String week) {
        boolean res = false;
        try {
            GregorianCalendar calDate = new GregorianCalendar();
            GregorianCalendar calWeek = new GregorianCalendar();

            calDate.setTime(SDF_D_MM_YYYY.parse(date));
            calWeek.setTime(SDF_D_MM_YYYY.parse(week));

            if (calDate.get(Calendar.WEEK_OF_YEAR) == calWeek.get(Calendar.WEEK_OF_YEAR)) {
                res = true;
            }
        } catch (ParseException e) {
        }
        return res;
    }

    /**
     * calculates the age of a person with his birthdate
     *
     * @param birthDate to calculate the age
     * @return number of years
     */
    public static int calculateAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public static Date parse_yyyy_MM_dd(String dateString) throws ParseException {
        return SDF_YYYY_MM_DD.parse(dateString);
    }

    public static Date parse_yyyy_MM(String dateString) throws ParseException {
        return SDF_YYYY_MM.parse(dateString);
    }

    public static Date parseSdfDatabase(String dateString) throws ParseException {
        return SDF_DATABASE.parse(dateString);
    }

    public static Date parse_yyyy_MM_dd_T_HH_mm_ss(String dateString) throws ParseException {
        return SDF_YYYY_MM_DD_T_HH_MM_SS.parse(dateString);
    }

    public static Date parse_yyyy_MM_dd_T_HH_mm(String dateString) throws ParseException {
        return SDF_YYYY_MM_DD_T_HH_MM.parse(dateString);
    }

    public static Date parse_yyyy_MM_dd_HH_mm(String dateString) throws ParseException {
        return SDF_D_MM_YYYY_HH_MM.parse(dateString);
    }

    public static Date parse_HH_mm(String dateString) throws ParseException {
        return SDF_HM.parse(dateString);
    }

    public static Date parse_dd_MM_yyyy(String dateString) throws ParseException {
        return SDF_DD_MM_YYYY.parse(dateString);
    }

    public static Date parse_yyyy_MM_dd_HH_mm_ss(String dateString, String type) throws ParseException {
        Date result;
        if(type.equals("UTC")){
            result = SDF_UTC_2.parse(dateString);
        } else {
            result = SDF_DATABASE.parse(dateString);
        }
        return result;
    }

    public static Date parse_ISO8601(String dateString, TimeZone timezone) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        df.setTimeZone(timezone);
        return df.parse(dateString);
    }

    public static Date parse_ISO8601_invoice_date(String dateString, TimeZone timezone) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = df.parse(dateString);

        // Luego ajustamos a la zona horaria deseada
        df.setTimeZone(timezone);
        String formattedDate = df.format(date);
        return df.parse(formattedDate);
    }

    public static String format_yyyy_MM_dd(Date date) throws ParseException {
        return SDF_YYYY_MM_DD.format(date);
    }

    public static String format_yyyy_MM(Date date) throws ParseException {
        return SDF_YYYY_MM.format(date);
    }

    public static String format_d_MM(Date date) throws ParseException {
        return SDF_D_MM.format(date);
    }
    public static String format_es_MX_dd_MMMM_yyyy_HH_mm(String dateString) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("'al' dd 'de' MMMM 'del' yyyy 'a las' HH:mm 'hrs'", new Locale("es", "MX"));
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String format = df.format(parse_yyyy_MM_dd_T_HH_mm_ss(dateString));
        System.out.println("date => ".concat(dateString).concat(" | format => ").concat(format));
        return format;
    }

    public static String format_yyyy(Date date) throws ParseException {
        return SDF_YYYY.format(date);
    }
    public static String format_MM(Date date) throws ParseException {
        return SDF_MM.format(date);
    }

    public static String format_YYYY_MM_DD_T_HH_MM_SS(Date date, TimeZone timezone) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        df.setTimeZone(timezone);
        return df.format(date);
    }

    public static String format_YYYY_MM_DD_T_HH_MM_SS(Date date) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return df.format(date);
    }

    public static boolean isGreaterThan(Date d1, Date d2) {
        return d1.compareTo(d2) > 0;
    }

    public static boolean isGreaterThanEqual(Date d1, Date d2) {
        return d1.compareTo(d2) >= 0;
    }

    public static boolean isLowerThan(Date d1, Date d2) {
        return d1.compareTo(d2) < 0;
    }

    public static boolean isLowerThanEqual(Date d1, Date d2) {
        return d1.compareTo(d2) <= 0;
    }

    public static boolean isEqual(Date d1, Date d2) {
        return d1.compareTo(d2) == 0;
    }

    public static boolean rangeIntersects(RangeDate a, RangeDate b) {
        return a.intersects(b);
    }

    public static int getDayOfWeek(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public static int getDayOfMonth(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static String getStringDayOfWeek(Date d) {
        int day = getDayOfWeek(d) - 1;
        if(day == 0) {
            return "sun";
        }
        if(day == 1) {
            return "mon";
        }
        if(day == 2) {
            return "tue";
        }
        if(day == 3) {
            return "wen";
        }
        if(day == 4) {
            return "thu";
        }
        if(day == 5) {
            return "fri";
        }
        return "sat";
    }

    /**
     * modelo contenedor de una fecha
     */
    public static class DateClass {

        private final Date date;

        public Date getDate() {
            return date;
        }

        public DateClass() {
            this.date = new Date();
        }

        public DateClass(Date date) {
            this.date = date;
        }
    }

    public static class InvalidRangeDateException extends Exception {

        private static final long serialVersionUID = 1L;

        public InvalidRangeDateException(String message) {
            super(message);
        }

    }

    public static class RangeDate {
        private final Date start;
        private final Date end;
        private final Date today;

        public RangeDate(Date start, Date end) throws InvalidRangeDateException {
            if (UtilsDate.isGreaterThan(start, end)) {
                throw new InvalidRangeDateException("Invalid range: "
                        + UtilsDate.format_D_MM_YYYY_HH_MM(start) + " (start date) is greater than "
                        + UtilsDate.format_D_MM_YYYY_HH_MM(end) + " (end date)"
                );
            }
            this.start = start;
            this.end = end;
            this.today = new Date();
        }
        /**
         * @return the start
         */
        public Date getStart() {
            return start;
        }
        /**
         * @return the end
         */
        public Date getEnd() {
            return end;
        }

        public boolean intersects(RangeDate other) {
            return (isGreaterThanEqual(other.getStart(), this.getStart()) &&
                    isGreaterThanEqual(this.getEnd(), other.getStart()))
                    || (isGreaterThanEqual(this.getStart(), other.getStart()) &&
                    isGreaterThanEqual(other.getEnd(), this.getStart()))
                    || (isGreaterThan(this.getStart(), other.getStart()) &&
                    isLowerThan(this.getEnd(), other.getEnd()))
                    || (isGreaterThan(other.getStart(), this.getStart()) &&
                    isLowerThan(other.getEnd(), this.getEnd()));
        }

        public void endIsGreaterThanNow() throws InvalidRangeDateException {
            if(UtilsDate.isGreaterThan(this.end, this.today)) {
                throw new InvalidRangeDateException("Invalid range: "
                        + UtilsDate.format_D_MM_YYYY_HH_MM(this.end) + " (end date) is greater than "
                        + UtilsDate.format_D_MM_YYYY_HH_MM(this.today) + " (today)"
                );
            }
        }

    }


    public static String addTime(Date dateBase, Integer addHours) {
        String res;
        SimpleDateFormat sdfh = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(dateBase);
        cal.add(Calendar.HOUR, addHours);

        res = sdfDataBase(cal.getTime());

        return res;
    }

    public static Date addHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    public static Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    public static Date addDays(Date dateBase, Integer days){
        Calendar dateExpires = Calendar.getInstance();
        dateExpires.setTime(dateBase);
        dateExpires.add(Calendar.DATE , days);
        dateBase = dateExpires.getTime();

        return dateBase;
    }

    public static Date addYears(Date dateBase, Integer years){
        Calendar dateExpires = Calendar.getInstance();
        dateExpires.setTime(dateBase);
        dateExpires.add(Calendar.YEAR , years);
        dateBase = dateExpires.getTime();

        return dateBase;
    }

    public static Date getLocalDate() {
        long today = new Date().getTime();
        int offset = timezone.getOffset(today);
        return new Date(today+offset);
    }

    public static DateTime getLocalDateTime() {
        long today = new Date().getTime();
        int offset = timezone.getOffset(today);
        return new DateTime(today+offset);
    }

    public static String getLocalOffset() {
        long today = new Date().getTime();
        int offset = timezone.getOffset(today) / 3600000;
        return String.format("%2d:00", offset);
    }

    public static Date toLocalDate(Date date) {
        long time = date.getTime();
        int offset = timezone.getOffset(time);
        return new Date(time+offset);
    }
    public static Date toClientDate(Date date) {
        long time = date.getTime();
        int offset = timezone.getOffset(time) * -1;
        return new Date(time+offset);
    }

    public static Date firstDayOfTheMonth(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.set(Calendar.DAY_OF_MONTH, 1);

        return calendar.getTime();
    }

    public static Date lastDayOfTheMonth(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.DATE, -1);

        return calendar.getTime();
    }

    public static int lastNumberDayOfTheMonth() {
        Calendar calendar = Calendar.getInstance();
        // Establecer el día como el último del mes
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static LocalDateTime convertServerTimeZone(LocalDateTime dateTime) {
        ZoneId zonaOrigen = ZoneId.of(UtilsDate.TIME_ZONE); // Nueva zona horaria
        ZoneId zonaDestino = ZoneId.systemDefault(); // Zona actual

        // Convertir LocalDateTime a ZonedDateTime en la zona original
        ZonedDateTime zonedDateTime = dateTime.atZone(zonaOrigen);

        // Convertirlo a la nueva zona horaria y obtener el LocalDateTime
        return zonedDateTime.withZoneSameInstant(zonaDestino).toLocalDateTime();
    }

    public static String localDateTimeToStringDate(LocalDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }

    public static LocalDateTime stringDateToLocalDateTime(String stringDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(stringDate, formatter);
    }

}
