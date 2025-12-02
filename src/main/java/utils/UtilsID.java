package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class UtilsID {
    private static final int LAST_CHARS = 6;
    private static final int GUIAPP_LAST_CHARS = 7;
    private static final int LAST_CHARS_SHORT = 5;

    private UtilsID() { }

    public static String generateID(String Tag) {
        StringBuilder ID = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyMM");

        ID.append(Tag.toUpperCase());
        ID.append(format.format(new Date()));
        ID.append(lastCharsID());

        return ID.toString();
    }

    public static String generateShortID(String Tag) {
        StringBuilder ID = new StringBuilder();

        ID.append(Tag.toUpperCase());
        ID.append(lastCharsShortID());

        return ID.toString();
    }

    private static String lastCharsID() {
        final String random = UUID.randomUUID().toString();
        return random.substring(random.length() - LAST_CHARS).toUpperCase();
    }

    private static String lastCharsShortID() {
        final String random = UUID.randomUUID().toString();
        return random.substring(random.length() - LAST_CHARS_SHORT).toUpperCase();
    }

    private static String guiaPPLastCharsID() {
        final String random = UUID.randomUUID().toString();
        return random.substring(random.length() - GUIAPP_LAST_CHARS).toUpperCase();
    }

    public static String generateGuiaPpID(String Tag) {
        StringBuilder ID = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyMM");

        ID.append(Tag.toUpperCase());
        ID.append(format.format(new Date()));
        ID.append(guiaPPLastCharsID());

        return ID.toString();
    }

    public static String generateRechargeID(String Tag) {
        StringBuilder ID = new StringBuilder();
        SimpleDateFormat format = new SimpleDateFormat("yyMM");

        ID.append(Tag.toUpperCase());
        ID.append(format.format(new Date()));
        ID.append(guiaPPLastCharsID());

        return ID.toString();
    }

    public static String generateEwalletCode(String Tag, Integer user_id) {
        StringBuilder ID = new StringBuilder();
        ID.append(Tag.toUpperCase());
        ID.append(fillZeros(user_id, 6));
        ID.append(lastCharsShortID());
        return ID.toString();
    }

    public static String fillZeros(int number, int length) {
        String idStr = String.valueOf(number);
        int diff = length - idStr.length();

        if (diff > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < diff; i++) {
                sb.append('0');
            }
            sb.append(idStr);
            return sb.toString();
        } else if (diff == 0) {
            return idStr;
        } else {
            return idStr.substring(0, length);
        }
    }

    public static String generateIdCCP() {
        UUID uuid = UUID.randomUUID();
        String uuidString = uuid.toString();
        return "CCC" + uuidString.substring(3);
    }
}
