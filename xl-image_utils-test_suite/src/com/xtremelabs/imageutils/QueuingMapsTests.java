package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class QueuingMapsTests extends AndroidTestCase {
	private QueuingMaps mMaps;

	private Prioritizable mTestPrioritizable1;
	private Prioritizable mTestPrioritizable2;
	private Prioritizable mTestPrioritizable3;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		mMaps = new QueuingMaps();
		mTestPrioritizable1 = generatePrioritizable("request1");
		mTestPrioritizable2 = generatePrioritizable("request1");
		mTestPrioritizable3 = generatePrioritizable("request2");
	}

	public void testPrioritizableStuff() {
		Request<?> r1 = new Request<String>("hello");
		Request<?> r2 = new Request<String>("hello");

		assertFalse(r1 == r2);
		assertTrue(r1.equals(r2));
		assertTrue(r1.hashCode() == r2.hashCode());
	}

	public void testingNotifySingleRequest() {
		mMaps.put(mTestPrioritizable1);
		mMaps.put(mTestPrioritizable2);
		mMaps.put(mTestPrioritizable3);

		mMaps.notifyExecuting(mTestPrioritizable3);

		assertFalse(mTestPrioritizable1.isCancelled());
		assertFalse(mTestPrioritizable2.isCancelled());
		assertFalse(mTestPrioritizable3.isCancelled());
	}

	public void testingNotifyMultipleRequests() {
		mMaps.put(mTestPrioritizable1);
		mMaps.put(mTestPrioritizable2);
		mMaps.put(mTestPrioritizable3);

		mMaps.notifyExecuting(mTestPrioritizable1);

		assertFalse(mTestPrioritizable1.isCancelled());
		assertTrue(mTestPrioritizable2.isCancelled());
		assertFalse(mTestPrioritizable3.isCancelled());
	}

	public void testingExecutionOfPendingRequest() {
		mMaps.put(mTestPrioritizable1);
		mMaps.notifyExecuting(mTestPrioritizable1);

		mMaps.put(mTestPrioritizable2);
		assertFalse(mTestPrioritizable1.isCancelled());
		assertTrue(mTestPrioritizable2.isCancelled());
	}

	public void testingExecutionOfPostCompletionRequest() {
		mMaps.put(mTestPrioritizable1);
		mMaps.notifyExecuting(mTestPrioritizable1);
		mMaps.onComplete(new Request<String>("request1"));

		mMaps.put(mTestPrioritizable2);

		assertFalse(mTestPrioritizable2.isCancelled());
	}

	public void testingRemovalOfPrioritizables() {
		mMaps.put(mTestPrioritizable1);
		mMaps.cancel(mTestPrioritizable1);

		assertTrue(mTestPrioritizable1.isCancelled());
	}

	private static Prioritizable generatePrioritizable(final String request) {
		return new Prioritizable() {

			@Override
			public int getTargetPriorityAccessorIndex() {
				return 0;
			}

			@Override
			public Request<?> getRequest() {
				return new Request<String>(request);
			}

			@Override
			public void execute() {
			}
		};
	}
}
