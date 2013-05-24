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

import java.io.InputStream;

/**
 * Allows developers to replace the default network implementation with a custom implementation. You can pass your own custom {@link NetworkRequestCreator} to the method
 * {@link ImageLoader#setNetworkRequestCreator(android.content.Context, NetworkRequestCreator)}, which will cause the image system to always use the custom network system.
 */
public interface NetworkRequestCreator {
	/**
	 * <b>IMPORTANT:</b> All implementations of this method must <b><i>always report back to the listener with the correct status</i></b>. A failure to report back will result in a memory leak, and may cause other
	 * unforeseen problems.
	 * 
	 * @param url
	 *            The URL from which we are fetching an image.
	 * @param listener
	 *            On success, this listener's "onInputStreamReady" method must be called with the input stream. On failure, this listener's "onFailure" method must be called.
	 */
	public void getInputStream(String url, InputStreamListener listener);

	public static interface InputStreamListener {
		public void onInputStreamReady(InputStream inputStream);

		public void onFailure(String errorMessage);
	}
}
