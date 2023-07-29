package com.ibm.mqmonitor;

public class Utils {
    
    public static String maskString(String s, int x) {
        int n = s.length()/x;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (n >= 1 && (i < n || i >= (s.length() - n))) {
                sb.append(s.charAt(i));
            }
            else {
                sb.append("*");
            }
        }
        return sb.toString();
    }
}
