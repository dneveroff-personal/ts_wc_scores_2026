package com.tswcscores.bot.keyboard;

import java.util.HashMap;
import java.util.Map;

/**
 * Конвертер TLA команды → флаг эмодзи.
 */
public class FlagEmoji {

    // HashMap вместо Map.ofEntries — не падает на дублях при опечатке
    private static final Map<String, String> TLA_TO_ISO = new HashMap<>();

    static {
        TLA_TO_ISO.put("ARG", "AR"); TLA_TO_ISO.put("AUS", "AU"); TLA_TO_ISO.put("BEL", "BE");
        TLA_TO_ISO.put("BRA", "BR"); TLA_TO_ISO.put("CAN", "CA"); TLA_TO_ISO.put("CHN", "CN");
        TLA_TO_ISO.put("COL", "CO"); TLA_TO_ISO.put("CRC", "CR"); TLA_TO_ISO.put("CRO", "HR");
        TLA_TO_ISO.put("DEN", "DK"); TLA_TO_ISO.put("ECU", "EC"); TLA_TO_ISO.put("EGY", "EG");
        TLA_TO_ISO.put("ENG", "GB-ENG"); TLA_TO_ISO.put("ESP", "ES"); TLA_TO_ISO.put("FRA", "FR");
        TLA_TO_ISO.put("GER", "DE"); TLA_TO_ISO.put("GHA", "GH"); TLA_TO_ISO.put("HON", "HN");
        TLA_TO_ISO.put("IRN", "IR"); TLA_TO_ISO.put("IRQ", "IQ"); TLA_TO_ISO.put("ITA", "IT");
        TLA_TO_ISO.put("JAM", "JM"); TLA_TO_ISO.put("JPN", "JP"); TLA_TO_ISO.put("KOR", "KR");
        TLA_TO_ISO.put("KSA", "SA"); TLA_TO_ISO.put("MAR", "MA"); TLA_TO_ISO.put("MEX", "MX");
        TLA_TO_ISO.put("NED", "NL"); TLA_TO_ISO.put("NGA", "NG"); TLA_TO_ISO.put("NZL", "NZ");
        TLA_TO_ISO.put("PAN", "PA"); TLA_TO_ISO.put("PER", "PE"); TLA_TO_ISO.put("POL", "PL");
        TLA_TO_ISO.put("POR", "PT"); TLA_TO_ISO.put("QAT", "QA"); TLA_TO_ISO.put("SEN", "SN");
        TLA_TO_ISO.put("SRB", "RS"); TLA_TO_ISO.put("SUI", "CH"); TLA_TO_ISO.put("TUN", "TN");
        TLA_TO_ISO.put("URU", "UY"); TLA_TO_ISO.put("USA", "US"); TLA_TO_ISO.put("WAL", "GB");
        TLA_TO_ISO.put("SCO", "GB"); TLA_TO_ISO.put("CMR", "CM"); TLA_TO_ISO.put("CIV", "CI");
        TLA_TO_ISO.put("MLI", "ML"); TLA_TO_ISO.put("VEN", "VE"); TLA_TO_ISO.put("BOL", "BO");
        TLA_TO_ISO.put("CHL", "CL"); TLA_TO_ISO.put("CHI", "CL"); TLA_TO_ISO.put("PAR", "PY");
        TLA_TO_ISO.put("ALG", "DZ"); TLA_TO_ISO.put("CGO", "CG"); TLA_TO_ISO.put("ZIM", "ZW");
        TLA_TO_ISO.put("UZB", "UZ"); TLA_TO_ISO.put("KAZ", "KZ"); TLA_TO_ISO.put("AZE", "AZ");
        TLA_TO_ISO.put("SLO", "SI"); TLA_TO_ISO.put("SVK", "SK"); TLA_TO_ISO.put("CZE", "CZ");
        TLA_TO_ISO.put("AUT", "AT"); TLA_TO_ISO.put("HUN", "HU"); TLA_TO_ISO.put("GRE", "GR");
        TLA_TO_ISO.put("TUR", "TR"); TLA_TO_ISO.put("UKR", "UA"); TLA_TO_ISO.put("NOR", "NO");
        TLA_TO_ISO.put("SWE", "SE"); TLA_TO_ISO.put("FIN", "FI"); TLA_TO_ISO.put("ISL", "IS");
        TLA_TO_ISO.put("IRL", "IE"); TLA_TO_ISO.put("BUL", "BG"); TLA_TO_ISO.put("ROU", "RO");
        TLA_TO_ISO.put("EST", "EE"); TLA_TO_ISO.put("LAT", "LV"); TLA_TO_ISO.put("LTU", "LT");
        TLA_TO_ISO.put("RUS", "RU"); TLA_TO_ISO.put("RSA", "ZA"); TLA_TO_ISO.put("SYR", "SY");
        TLA_TO_ISO.put("JOR", "JO"); TLA_TO_ISO.put("LBN", "LB"); TLA_TO_ISO.put("PHI", "PH");
        TLA_TO_ISO.put("THA", "TH"); TLA_TO_ISO.put("VIE", "VN"); TLA_TO_ISO.put("IDN", "ID");
    }

    public static String fromTla(String tla) {
        if (tla == null) return "🏳";
        String iso = TLA_TO_ISO.get(tla.toUpperCase());
        if (iso == null) return "🏳";
        // GB-ENG и подобные subdivision коды не поддерживаются как emoji — используем родительский
        if (iso.contains("-")) iso = iso.substring(0, iso.indexOf("-"));
        return fromIso(iso);
    }

    public static String fromIso(String iso) {
        if (iso == null || iso.length() != 2) return "🏳";
        int first  = iso.toUpperCase().charAt(0) + 0x1F1A5;
        int second = iso.toUpperCase().charAt(1) + 0x1F1A5;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }
}
