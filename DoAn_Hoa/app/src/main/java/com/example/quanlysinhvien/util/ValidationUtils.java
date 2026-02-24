package com.example.quanlysinhvien.util;

import java.util.regex.Pattern;

public class ValidationUtils {

    // 8-16 characters, at least one uppercase, one number, and one special
    // character
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,16}$";

    public static boolean isValidPassword(String password) {
        if (password == null)
            return false;
        return Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
    }

    public static String getPasswordRequirements() {
        return "Mật khẩu phải từ 8-16 ký tự, bao gồm ít nhất 1 chữ hoa, 1 số và 1 ký tự đặc biệt.";
    }
}
