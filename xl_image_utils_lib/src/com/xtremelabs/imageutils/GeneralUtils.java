/*
 * Copyright 2013 Xtreme Labs
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
