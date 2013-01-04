package com.xtremelabs.imageutils;

import java.util.List;

interface ReferenceManager {

	void getBitmap(Object key, ImageRequest imageRequest, ImageManagerListener imageManagerListener);

	List<ImageManagerListener> removeListenersForKey(Object key);

	void cancelRequest(ImageManagerListener imageManagerListener);

}
