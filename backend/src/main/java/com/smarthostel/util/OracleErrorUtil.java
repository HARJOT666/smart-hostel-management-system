package com.smarthostel.util;

import java.sql.SQLException;

/**
 * Helper to read the Oracle error code (e.g. 20011 for ORA-20011) from an exception
 * and turn it into a message that can be shown to the user.
 */
public final class OracleErrorUtil {

    private OracleErrorUtil() {
    }

    /** Finds the SQLException inside the exception chain and returns its Oracle error code (0 if none). */
    public static int getOracleErrorCode(Throwable ex) {
        SQLException sql = findSqlException(ex);
        return sql == null ? 0 : sql.getErrorCode();
    }

    public static String toFriendlyMessage(Throwable ex) {
        SQLException sql = findSqlException(ex);
        if (sql == null) {
            return ex.getMessage();
        }
        int code = sql.getErrorCode();
        String first = firstLine(sql.getMessage());

        if (code >= 20000 && code <= 20999) {
            // "ORA-20011: Student already has an active room allocation" -> text after the code
            return first.replaceFirst("^ORA-\\d+:\\s*", "");
        }
        return switch (code) {
            case 1 -> "Duplicate value: " + constraintName(first) + " must be unique";
            case 2290 -> "Invalid value: check constraint " + constraintName(first) + " failed";
            case 2291 -> "Referenced record does not exist (" + constraintName(first) + ")";
            case 2292 -> "Record is still referenced by other records (" + constraintName(first) + ")";
            case 1400, 1407 -> "A required field is missing";
            default -> first;
        };
    }

    private static SQLException findSqlException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sql) {
                return sql;
            }
            current = current.getCause();
        }
        return null;
    }

    private static String firstLine(String message) {
        if (message == null) return "";
        int newLine = message.indexOf('\n');
        return newLine < 0 ? message.trim() : message.substring(0, newLine).trim();
    }

    /** Extracts "UQ_STUDENT_EMAIL" from "... constraint (USER1.UQ_STUDENT_EMAIL) violated ..." */
    private static String constraintName(String message) {
        int start = message.indexOf('(');
        int end = message.indexOf(')', start + 1);
        if (start < 0 || end < 0) return "constraint";
        String name = message.substring(start + 1, end);
        int dot = name.indexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : name;
    }
}
