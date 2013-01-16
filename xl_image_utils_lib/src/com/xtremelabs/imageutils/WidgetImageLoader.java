package com.xtremelabs.imageutils;

import android.app.Service;

public class WidgetImageLoader extends AbstractImageLoader {
	public WidgetImageLoader(Service service) {
		super(service, service.getApplicationContext());
	}
}
