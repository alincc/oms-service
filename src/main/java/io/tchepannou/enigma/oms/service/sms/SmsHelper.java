package io.tchepannou.enigma.oms.service.sms;

public class SmsHelper {
    private static final int MESSAGE_MAX_LEN = 160;

    public static String formatPhone(String phone) {
        if (phone == null){
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phone.length(); i++) {
            char ch = phone.charAt(i);
            if (Character.isDigit(ch)) {
                sb.append(ch);
            }
        }
        return "+1" + sb.toString();
    }

    public static String formatMessage(String message){
        if (message == null){
            return null;
        }
        return message.length() > MESSAGE_MAX_LEN ? message.substring(0, MESSAGE_MAX_LEN) : message;
    }

}
