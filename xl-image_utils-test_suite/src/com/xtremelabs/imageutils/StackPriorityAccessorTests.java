package com.xtremelabs.imageutils;

import android.test.AndroidTestCase;

public class StackPriorityAccessorTests extends AndroidTestCase {
	private StackPriorityAccessor mAccessor;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		mAccessor = new StackPriorityAccessor();
	}

	public void testStack() {
		Prioritizable p1 = generatePrioritizable();
		Prioritizable p2 = generatePrioritizable();

		mAccessor.attach(p1);
		assertEquals(1, mAccessor.size());

		mAccessor.attach(p2);
		assertEquals(2, mAccessor.size());

		Prioritizable p = mAccessor.detachHighestPriorityItem();
		assertEquals(p2, p);

		p = mAccessor.detachHighestPriorityItem();
		assertEquals(p1, p);
	}

	private static Prioritizable generatePrioritizable() {
		return new Prioritizable() {
			@Override
			public int getTargetPriorityAccessorIndex() {
				return 0;
			}

			@Override
			public Request<?> getRequest() {
				return null;
			}

			@Override
			public void execute() {
			}
		};
	}
}
