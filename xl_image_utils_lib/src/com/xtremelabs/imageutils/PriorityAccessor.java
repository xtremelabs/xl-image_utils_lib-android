package com.xtremelabs.imageutils;

public interface PriorityAccessor {

	public void attach(Prioritizable prioritizable);

	public boolean detach(Prioritizable p);

	public Prioritizable detachHighestPriorityItem();

	public int size();

	public Prioritizable peek();

	public void clear();

	boolean contains(Prioritizable prioritizable);
}
