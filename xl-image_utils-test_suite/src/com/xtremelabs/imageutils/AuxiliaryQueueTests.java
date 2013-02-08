package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class AuxiliaryQueueTests extends AndroidTestCase {
	private AuxiliaryQueue mQueue;
	private final Prioritizable mValidPrioritizable1 = new Prioritizable() {
		@Override
		public void run() {
		}

		@Override
		public int getTargetPriorityAccessorIndex() {
			return 0;
		}
	};

	private final Prioritizable mValidPrioritizable2 = new Prioritizable() {
		@Override
		public void run() {
		}

		@Override
		public int getTargetPriorityAccessorIndex() {
			return 0;
		}
	};

	private final Prioritizable mInvalidPrioritizable = new Prioritizable() {
		@Override
		public void run() {
		}

		@Override
		public int getTargetPriorityAccessorIndex() {
			return -1;
		}
	};

	public void testSinglePriorityQueue() {
		mQueue = new AuxiliaryQueue(new PriorityAccessor[] { new StackPriorityAccessor() });

		mQueue.add(mValidPrioritizable1);
		mQueue.add(mValidPrioritizable2);

		assertEquals(mValidPrioritizable2, mQueue.removeHighestPriorityRunnable());
		assertEquals(mValidPrioritizable1, mQueue.removeHighestPriorityRunnable());
		assertNull(mQueue.removeHighestPriorityRunnable());
	}

	public void testAddingInvalidPrioritizable() {
		mQueue = new AuxiliaryQueue(new PriorityAccessor[] { new StackPriorityAccessor() });

		boolean passed = false;
		try {
			mQueue.add(mInvalidPrioritizable);
		} catch (IndexOutOfBoundsException e) {
			passed = true;
		}
		assertTrue(passed);
	}
}
