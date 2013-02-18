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
		Prioritizable p1 = new ImageSystemPrioritizable();
		Prioritizable p2 = new ImageSystemPrioritizable();

		mAccessor.attach(p1);
		assertEquals(1, mAccessor.size());

		mAccessor.attach(p2);
		assertEquals(2, mAccessor.size());

		Prioritizable p = mAccessor.detachHighestPriorityItem();
		assertEquals(p2, p);

		p = mAccessor.detachHighestPriorityItem();
		assertEquals(p1, p);
	}
}
