package com.xtremelabs.imageutils;

public abstract class PriorityAccessor {

	public abstract Prioritizable pop();

	public abstract void attach(Prioritizable prioritizable);
}
