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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

class DefaultNetworkRequestCreator implements NetworkRequestCreator {
	@Override
	public void getInputStream(String url, InputStreamListener listener) {
		HttpEntity entity = null;
		InputStream inputStream = null;

		HttpClient client = new DefaultHttpClient();
		client.getConnectionManager().closeExpiredConnections();
		HttpUriRequest request;
		try {
			request = new HttpGet(url);
		} catch (IllegalArgumentException e) {
			try {
				request = new HttpGet(URLEncoder.encode(url, "UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				String errorMessage = "Unable to download image. Reason: Bad URL. URL: " + url;
				Log.w(ImageLoader.TAG, errorMessage);
				listener.onFailure(errorMessage);
				return;
			}
		}
		HttpResponse response;
		try {
			response = client.execute(request);

			entity = response.getEntity();
			if (entity == null) {
				listener.onFailure("Was unable to retrieve an HttpEntity for the image!");
				return;
			}

			inputStream = new BufferedInputStream(entity.getContent());
			listener.onInputStreamReady(inputStream);
		} catch (IOException e) {
			listener.onFailure("IOException caught when attempting to download an image! Stack trace below. URL: " + url + ", Message: " + e.getMessage());
			e.printStackTrace();
		}

		try {
			if (entity != null) {
				entity.consumeContent();
			}
		} catch (IOException e) {
		}

		client.getConnectionManager().closeExpiredConnections();
	}
}
