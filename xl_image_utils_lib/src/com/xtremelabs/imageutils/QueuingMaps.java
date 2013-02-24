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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.os.Build;

@SuppressLint("NewApi")
class QueuingMaps {
	private static final int GINGERBREAD = 9;
	private final Set<Request<?>> mRunningRequests = Build.VERSION.SDK_INT >= GINGERBREAD ? Collections.newSetFromMap(new ConcurrentHashMap<Request<?>, Boolean>()) : new HashSet<Request<?>>();
	private final Map<Request<?>, List<Prioritizable>> mRequestListeners = new ConcurrentHashMap<Request<?>, List<Prioritizable>>();

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
		Request<?> request = prioritizable.getRequest();
		List<Prioritizable> list = mRequestListeners.get(request);
		if (list != null) {
			list.remove(prioritizable);
			if (list.size() == 0)
				mRequestListeners.remove(request);
		}
		prioritizable.cancel();
		return mRunningRequests.contains(prioritizable.getRequest());
	}
}
