package com.xtremelabs.imageutils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.widget.ImageView;

import com.xtremelabs.imageutils.ThreadChecker.CalledFromWrongThreadException;

// TODO: Apply the plugin that has ifdef.

/**
 * This class simplifies the task of loading images from a URL into an {@link ImageView} on Android.
 * 
 * HOW TO USE: For use without the support library Every {@link Activity} or {@link Fragment} that needs images must instantiate its own {@link ImageLoader} instance.
 * 
 * When used with an {@link Activity}, instantiate a new {@link ImageLoader} in the Activity's onCreate() method. Make sure you call the ImageLoader's onDestroy method from within the Activity's onDestroy method.
 * 
 * When used with a {@link Fragment}, instantiate your ImageLoader in the onCreateView() method. Make sure you call the ImageLoader's onDestroy method in the onDestroyView method.
 */
@TargetApi(11)
public class ImageLoader extends AbstractImageLoader {

	/**
	 * Instantiates a new {@link ImageLoader} that maps all requests to the provided {@link Activity}.
	 * 
	 * This should be called from your Activity's onCreate method.
	 * 
	 * @param activity
	 *            All requests to the {@link ImageLoader} will be mapped to this {@link Activity}. All references are released when the ImageLoader's onDestroy() method is called.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	public ImageLoader(Activity activity) {
		super(activity, activity.getApplicationContext());
	}

	/**
	 * Instantiate a new {@link ImageLoader} that maps all requests to the provided {@link Fragment}.
	 * 
	 * This should be called from your Fragment's onCreateView method.
	 * 
	 * @param fragment
	 *            All requests to the {@link ImageLoader} will be mapped to this {@link Fragment}. All references are released when the ImageLoader's onDestroy() method is called.
	 * 
	 * @throws CalledFromWrongThreadException
	 *             This constructor must be called from the UI thread.
	 */
	public ImageLoader(Fragment fragment) {
		super(fragment, fragment.getActivity().getApplicationContext());
	}

}
