/*
 * Copyright 2012 Xtreme Labs
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.widget.ImageView;

import com.xtremelabs.imageutils.ThreadChecker.CalledFromWrongThreadException;

// TODO: Apply the plugin that has ifdef.

/**
 * This class simplifies the task of loading images from a URL into an
 * {@link ImageView} on Android.
 * 
 * HOW TO USE: For use without the support library Every {@link Activity} or
 * {@link Fragment} that needs images must instantiate its own
 * {@link ImageLoader} instance.
 * 
 * When used with an {@link Activity}, instantiate a new {@link ImageLoader} in
 * the Activity's onCreate() method. Make sure you call the ImageLoader's
 * onDestroy method from within the Activity's onDestroy method.
 * 
 * When used with a {@link Fragment}, instantiate your ImageLoader in the
 * onCreateView() method. Make sure you call the ImageLoader's onDestroy method
 * in the onDestroyView method.
 */
@TargetApi(11)
public class ImageLoader extends AbstractImageLoader {

	/**
	 * Instantiates a new {@link ImageLoader} that maps all requests to the
	 * provided {@link Activity}.
	 * 
	 * This should be called from your Activity's onCreate method.
	 * 
	 * @param activity
	 *            All requests to the {@link ImageLoader} will be mapped to this
	 *            {@link Activity}. All references are released when the
	 *            ImageLoader's onDestroy() method is called.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	public ImageLoader(Activity activity) {
		super(activity, activity.getApplicationContext());
	}

	/**
	 * Instantiate a new {@link ImageLoader} that maps all requests to the
	 * provided {@link Fragment}.
	 * 
	 * This should be called from your Fragment's onCreateView method.
	 * 
	 * @param fragment
	 *            All requests to the {@link ImageLoader} will be mapped to this
	 *            {@link Fragment}. All references are released when the
	 *            ImageLoader's onDestroy() method is called.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	public ImageLoader(Fragment fragment) {
		super(fragment, fragment.getActivity().getApplicationContext());
	}
	
	public ImageLoader(Service service) {
		super(service, service.getApplicationContext());
	}

}
