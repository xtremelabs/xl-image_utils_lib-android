package com.xtremelabs.imageutils;

class GeneralUtils {
	public static boolean isStringBlank(String s) {
		return s == null || s.length() == 0;
	}
	
	public static boolean isStringNotBlank(String s) {
		return !isStringBlank(s);
	}
}
