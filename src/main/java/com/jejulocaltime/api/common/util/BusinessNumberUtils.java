package com.jejulocaltime.api.common.util;

public final class BusinessNumberUtils {

    private BusinessNumberUtils() {
    }

    public static String normalize(String businessNumber) {
        if (businessNumber == null) {
            return "";
        }
        return businessNumber.replaceAll("\\D", "");
    }

    public static boolean isValidFormat(String businessNumber) {
        String normalized = normalize(businessNumber);
        return normalized.length() == 10;
    }

    public static String normalizeOpenDate(String openDate) {
        if (openDate == null) {
            return "";
        }
        return openDate.replaceAll("\\D", "");
    }

    public static boolean isValidOpenDate(String openDate) {
        String normalized = normalizeOpenDate(openDate);
        return normalized.length() == 8;
    }
}