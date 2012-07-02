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

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.os.Build;

class LifoThreadPool {
	private ThreadPoolExecutor mThreadPool;
	private LifoBlockingStack mStack;

	public LifoThreadPool(int poolSize) {
		if (Build.VERSION.SDK_INT >= 9) {
			mStack = new LifoBlockingStack();
			mThreadPool = new ThreadPoolExecutor(poolSize, poolSize, 120, TimeUnit.SECONDS, mStack);
		} else {
			mThreadPool = new ThreadPoolExecutor(poolSize, poolSize, 120, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		}
	}

	public void execute(final Runnable runnable) {
		/*
		 * This is a performance hack.
		 * 
		 * ThreadPoolExecutors, when queuing a runnable, can take up to 100ms on the UI thread. Since we need the ThreadPoolExecutor to support the LIFO system,
		 * we still need to post our runnables to the executor, but we can use an AsyncTask to kick off the laggy execute call off the UI thread.
		 */
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				mThreadPool.execute(runnable);
				return null;
			}
		}.execute();
	}

	public void bump(Runnable runnable) {
		if (mStack != null) {
			mStack.bump(runnable);
		}
	}

	private class LifoBlockingStack extends LinkedBlockingDeque<Runnable> {
		private static final long serialVersionUID = -4854985351588039351L;

		@Override
		public boolean offer(Runnable runnable) {
			return super.offerFirst(runnable);
		}

		@Override
		public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
			return super.offerFirst(runnable, timeout, unit);
		}

		@Override
		public boolean add(Runnable runnable) {
			return super.offerFirst(runnable);
		}

		@Override
		public void put(Runnable runnable) throws InterruptedException {
			super.putFirst(runnable);
		}

		public void bump(Runnable runnable) {
			if (runnable != null && super.remove(runnable)) {
				super.offerFirst(runnable);
			}
		}
	};
}
