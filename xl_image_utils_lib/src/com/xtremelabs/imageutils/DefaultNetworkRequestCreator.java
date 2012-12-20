package com.xtremelabs.imageutils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

class DefaultNetworkRequestCreator implements NetworkRequestCreator {
	@Override
	public void getInputStream(String url, InputStreamListener listener) {
		HttpEntity entity = null;
		InputStream inputStream = null;

		HttpClient client = new DefaultHttpClient();
		client.getConnectionManager().closeExpiredConnections();
		HttpUriRequest request = new HttpGet(url);
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
