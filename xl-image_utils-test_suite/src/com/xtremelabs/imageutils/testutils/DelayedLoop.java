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

package com.xtremelabs.imageutils.testutils;

import junit.framework.Assert;

/**
 * This class simplifies the process of testing asynchronous code within unit tests.<br>
 * <br>
 * Usage:<br>
 * 1. Instantiate this class with your desired "timeout" interval.<br>
 * 2. Call {@link #startLoop()} after you have triggered your asynchronous event.<br>
 * 3. Immediately after the call to {@link #startLoop()}, assert that the test did not fail by calling assertFalse({@link #isFailure()}), and immediately after that check that the test passed by calling assertTrue(
 * {@link #isSuccess()}). Alternatively, you can assert that {@link #hasPassed()} is true.<br>
 * 4. Within your asynchronous callback, call {@link #flagSuccess()} or {@link #flagFailure()} to indicate success and failure respectively.<br>
 */
public class DelayedLoop {
	private static final long DELAY = 10;

	private boolean mSuccess = false;
	private boolean mFailure = false;

	private final long mTimeout;

	/**
	 * Initializes the delayed loop with the specified timeout interval. The loop will automatically break if the timeout interval is exceeded after a call to {@link #startLoop()}.
	 * 
	 * @param timeout
	 */
	public DelayedLoop(long timeout) {
		mTimeout = timeout;
	}

	/**
	 * This method will cause the current thread to repeatedly sleep until the timeout duration has passed, the test succeeded ({@link #flagSuccess()}) or the test failed ({@link #flagFailure()}).
	 */
	public void startLoop() {
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;

		while (elapsedTime < mTimeout && !shouldBreak()) {
			sleep(DELAY);
			elapsedTime = System.currentTimeMillis() - startTime;
		}
	}

	/**
	 * Marks the asynchronous event as successful and breaks out of the loop.
	 */
	public void flagSuccess() {
		mSuccess = true;
	}

	/**
	 * Marks the asynchronous event as a failure and breaks out of the loop.
	 */
	public void flagFailure() {
		mFailure = true;
	}

	public void assertPassed() {
		Assert.assertTrue(!mFailure && mSuccess);
	}

	/**
	 * This value should be asserted to be true after the call to {@link #startLoop()} and after {@link #isFailure()} is asserted to be false.
	 * 
	 * @return The "successful" completion of the asynchronous task. WARNING: This may indicate "true" even if the test failed. This value must be checked <i>after</i> the failure condition.
	 */
	public boolean isSuccess() {
		return mSuccess;
	}

	/**
	 * This value should be asserted to be false after the call to {@link #startLoop()} and before asserting the value from {@link #isSuccess()}.
	 * 
	 * @return The "failure" status of the asynchronous task.This value must be checked <i>before</i> the success condition.
	 */
	public boolean isFailure() {
		return mFailure;
	}

	private boolean shouldBreak() {
		return mSuccess || mFailure;
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}
}
