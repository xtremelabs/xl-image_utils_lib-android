package com.xtremelabs.imageutils;

import java.util.List;

interface ReferenceManager {

	void getBitmap(Object key, String url, ImageManagerListener imageManagerListener, ScalingInfo scalingInfo);

	List<ImageManagerListener> removeListenersForKey(Object key);

	void cancelRequest(ImageManagerListener imageManagerListener);

}
