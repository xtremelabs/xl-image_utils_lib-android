package com.xtremelabs.imageutils;

import android.app.Activity;
import android.support.v4.app.Fragment;

@Deprecated
public class ImageLoaderSupport extends ImageLoader {

	public ImageLoaderSupport(Activity activity) {
		super(activity);
	}

	public ImageLoaderSupport(Fragment fragment) {
		super(fragment, fragment.getActivity().getApplicationContext());
	}
}
