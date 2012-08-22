package com.xtremelabs.imageutils.testutils;

public class GeneralTestUtils {
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}

	}

	public static void delayedLoop(long timeOut, DelayedLoopListener delayedLoopListener) {
		delayedLoopWithDelay(timeOut, 10, delayedLoopListener);
	}

	public static void delayedLoopWithDelay(long timeOut, long delay, DelayedLoopListener delayedLoopListener) {
		long startTime = System.currentTimeMillis();
		long elapsedTime = 0;

		while (elapsedTime < timeOut && !delayedLoopListener.shouldBreak()) {
			sleep(delay);
			elapsedTime = System.currentTimeMillis() - startTime;
		}
	}

	public interface DelayedLoopListener {
		public boolean shouldBreak();
	}
}
