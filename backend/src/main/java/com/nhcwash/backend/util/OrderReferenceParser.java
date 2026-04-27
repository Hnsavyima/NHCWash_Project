package com.nhcwash.backend.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses public order references such as {@code CMD-018} (from QR codes / emails) to numeric order ids.
 */
public final class OrderReferenceParser {

    private static final Pattern CMD = Pattern.compile("CMD-([0-9]+)", Pattern.CASE_INSENSITIVE);

    private OrderReferenceParser() {
    }

    /**
     * @param raw scanned or typed text (may contain extra whitespace or surrounding noise)
     * @return order id, or {@code null} if not parseable
     */
    public static Long tryParseOrderId(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        try {
            t = URLDecoder.decode(t, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // keep original
        }
        t = t.trim();
        Matcher m = CMD.matcher(t);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (t.matches("[0-9]+")) {
            try {
                return Long.parseLong(t);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
