package com.xtremelabs.imageutils;

public class ImageSystemPrioritizable extends Prioritizable {

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
}
