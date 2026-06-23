package com.tswcscores.service.impl;

import com.tswcscores.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class TimezoneService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Moscow");

    public static ZoneId getUserZone(User user) {
        if (user == null || user.getTimezone() == null || user.getTimezone().isBlank()) {
            return DEFAULT_ZONE;
        }
        try {
            return ZoneId.of(user.getTimezone());
        } catch (Exception e) {
            log.warn("Invalid timezone '{}' for user {}, falling back to default", user.getTimezone(), user.getTelegramId());
            return DEFAULT_ZONE;
        }
    }

    public static String formatForUser(User user, LocalDateTime utcDateTime) {
        ZoneId zone = getUserZone(user);
        return utcDateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(zone)
                .format(FMT);
    }

    public static String formatForUserWithAbbr(User user, LocalDateTime utcDateTime) {
        ZoneId zone = getUserZone(user);
        String abbr = getZoneAbbreviation(zone);
        return utcDateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(zone)
                .format(FMT) + " (" + abbr + ")";
    }

    public static String getZoneAbbreviation(ZoneId zone) {
        // Return a short abbreviation for display
        String id = zone.getId();
        if (id.equals("Europe/Moscow")) return "МСК";
        if (id.equals("Europe/Kiev") || id.equals("Europe/Kyiv")) return "Киев";
        if (id.equals("Europe/Minsk")) return "Минск";
        if (id.equals("Europe/London")) return "GMT";
        if (id.equals("Europe/Berlin")) return "CET";
        if (id.equals("Europe/Paris")) return "CET";
        if (id.equals("Asia/Almaty")) return "Алматы";
        if (id.equals("Asia/Tashkent")) return "Ташкент";
        if (id.equals("Asia/Dubai")) return "Дубай";
        // Generic: return the zone ID last part
        String[] parts = id.split("/");
        return parts[parts.length - 1].replace('_', ' ');
    }

    public static List<String> getCommonTimezones() {
        return List.of(
                "Europe/Moscow",
                "Europe/Kiev",
                "Europe/Minsk",
                "Europe/London",
                "Europe/Berlin",
                "Europe/Paris",
                "Asia/Almaty",
                "Asia/Tashkent",
                "Asia/Dubai",
                "Asia/Tbilisi",
                "Asia/Yerevan",
                "Europe/Istanbul"
        );
    }
}
