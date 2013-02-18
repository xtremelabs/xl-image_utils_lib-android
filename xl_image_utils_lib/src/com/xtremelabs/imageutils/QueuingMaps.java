package com.xtremelabs.imageutils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class QueuingMaps {
	private final Set<Request<?>> mRunningRequests = new HashSet<Request<?>>();
	private final Map<Request<?>, List<Prioritizable>> mRequestListeners = new HashMap<Request<?>, List<Prioritizable>>();

	public synchronized void put(Prioritizable prioritizable) {
		Request<?> request = prioritizable.getRequest();

		if (mRunningRequests.contains(request)) {
			prioritizable.cancel();
			return;
		}

		List<Prioritizable> list = mRequestListeners.get(request);
		if (list == null) {
			list = new ArrayList<Prioritizable>();
			mRequestListeners.put(request, list);
		}
		list.add(prioritizable);
	}

	public synchronized void onComplete(Request<?> request) {
		mRunningRequests.remove(request);
	}

	public synchronized void notifyExecuting(Prioritizable prioritizable) {
		Request<?> request = prioritizable.getRequest();

		if (mRunningRequests.contains(request)) {
			prioritizable.cancel();
		} else {
			mRunningRequests.add(request);
			List<Prioritizable> prioritizables = mRequestListeners.remove(request);
			if (prioritizables != null) {
				for (Prioritizable p : prioritizables) {
					if (p != prioritizable)
						p.cancel();
				}
			}
		}
	}

	public synchronized boolean cancel(Prioritizable prioritizable) {
		List<Prioritizable> list = mRequestListeners.get(prioritizable.getRequest());
		if (list != null) {
			list.remove(prioritizable);
		}
		prioritizable.cancel();
		return mRunningRequests.contains(prioritizable.getRequest());
	}
}
