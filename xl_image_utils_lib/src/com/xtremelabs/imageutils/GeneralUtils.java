package com.xtremelabs.imageutils;

import java.net.URI;
import java.net.URISyntaxException;

class GeneralUtils {
	public static boolean isStringBlank(String s) {
		return s == null || s.length() == 0;
	}

	public static boolean isStringNotBlank(String s) {
		return !isStringBlank(s);
	}

	public static boolean isFileSystemUri(String uri) {
		try {
			URI testUri = new URI(uri.replace(' ', '+'));
			String scheme = testUri.getScheme();
			if (scheme != null && scheme.equalsIgnoreCase("file")) {
				return true;
			}
		} catch (URISyntaxException e) {
		}
		return false;
	}
}
