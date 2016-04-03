package com.photostalk.utils;

/**
 * Created by mohammed on 2/19/16.
 */
public class Validations {

    public static boolean noSpaces(String str) {
        return !str.contains(" ");
    }

    public static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z0-9]{2,6}$");
    }

    public static boolean notEmptyOrWhiteSpaces(String str) {
        return !str.matches("^$|\\s+");
    }

}
