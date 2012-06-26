package com.xtremelabs.imageutils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class LIFOThreadPool {
	private LIFOBlockingQueue mStack;
	private ThreadPoolExecutor mThreadPool;
	
	public LIFOThreadPool(int poolSize, int keepAliveTime, TimeUnit unit, int maximumStackSize) {
		mStack = new LIFOBlockingQueue(maximumStackSize);
		mThreadPool = new ThreadPoolExecutor(poolSize, poolSize, keepAliveTime, unit, mStack);
	}
	
	public void execute(Runnable runnable) {
		mThreadPool.execute(runnable);
	}
	
	private class LIFOBlockingQueue implements BlockingQueue<Runnable> {
		private final int MAX_SIZE;
		private LinkedList<Runnable> stack;
		
		public LIFOBlockingQueue(int size) {
			MAX_SIZE = size;
			stack = new LinkedList<Runnable>();
		}
		
		@Override
		public synchronized Runnable element() {
			return stack.element();
		}

		@Override
		public synchronized Runnable peek() {
			return stack.peek();
		}

		@Override
		public synchronized Runnable poll() {
			return stack.poll();
		}

		@Override
		public synchronized Runnable remove() {
			return stack.remove();
		}

		@SuppressWarnings("unchecked")
		@Override
		public synchronized boolean addAll(Collection<? extends Runnable> collection) {
			return stack.addAll((Collection<? extends Runnable>)collection);
		}

		@Override
		public synchronized void clear() {
			stack.clear();
		}

		@Override
		public synchronized boolean containsAll(Collection<?> collection) {
			return stack.containsAll(collection);
		}

		@Override
		public synchronized boolean isEmpty() {
			return stack.isEmpty();
		}

		@Override
		public synchronized Iterator<Runnable> iterator() {
			return null;
		}

		@Override
		public synchronized boolean removeAll(Collection<?> collection) {
			return stack.removeAll(collection);
		}

		@Override
		public synchronized boolean retainAll(Collection<?> collection) {
			return stack.retainAll(collection);
		}

		@Override
		public synchronized int size() {
			return stack.size();
		}

		@Override
		public synchronized Object[] toArray() {
			return stack.toArray();
		}

		@SuppressWarnings("hiding")
		@Override
		public synchronized <Runnable> Runnable[] toArray(Runnable[] array) {
			return stack.toArray(array);
		}

		@Override
		public synchronized boolean add(Runnable e) {
			return stack.add((Runnable) e);
		}

		@Override
		public synchronized boolean contains(Object o) {
			return stack.contains(o);
		}

		@Override
		public synchronized int drainTo(Collection<? super Runnable> collection) {
			return 0;
		}

		@Override
		public int drainTo(Collection<? super Runnable> arg0, int arg1) {
			return 0;
		}

		@Override
		public boolean offer(Runnable e) {
			return stack.offer((Runnable)e);
		}

		@Override
		public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
			return stack.offer((Runnable) e);
		}

		@Override
		public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
			return stack.poll();
		}

		@Override
		public synchronized void put(Runnable e) throws InterruptedException {
		}

		@Override
		public int remainingCapacity() {
			return Integer.MAX_VALUE;
		}

		@Override
		public synchronized boolean remove(Object o) {
			return stack.remove(o);
		}

		@Override
		public Runnable take() throws InterruptedException {
			return null;
		}
	}
}
