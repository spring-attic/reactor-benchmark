package org.projectreactor.bench.reactor;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jon Brisbin
 */
@State(Scope.Thread)
public class IterationBenchmarks {

	static int ITEMS = 2000000;

	List<Object>       objList;
	Object[]           objArray;
	LinkedNode         startNode;
	LinkedNode         objNode;
	ArrayBasedIterator objIter;
	LinkedNodeIterator linkedNodeIter;

	Iterable<Object> linkedNodeIterable = new Iterable<Object>() {
		@Override
		public Iterator<Object> iterator() {
			return new LinkedNodeIterator(startNode);
		}
	};
	Iterable<Object> arrayIterable      = new Iterable<Object>() {
		@Override
		public Iterator<Object> iterator() {
			return new ArrayBasedIterator(objArray);
		}
	};

	@Setup
	public void setup() {
		objList = new ArrayList<Object>();
		objArray = new Object[ITEMS];

		for(int i = 0; i < ITEMS; i++) {
			objList.add(new Object());
			objArray[i] = new Object();
			LinkedNode node = new LinkedNode(new Object(), objNode);
			if(null != objNode) {
				objNode.next = node;
			} else {
				startNode = node;
			}
			objNode = node;
		}
		//objList = Arrays.asList(objArray);

		objIter = new ArrayBasedIterator(objArray);
		linkedNodeIter = new LinkedNodeIterator(startNode);
	}

	@GenerateMicroBenchmark
	public void listOptimizedForLoop() {
		for(Object obj : objList) {}
	}

	@GenerateMicroBenchmark
	public void listIndexedForLoop() {
		for(int i = 0; i < ITEMS; i++) { Object obj = objList.get(i); }
	}

	@GenerateMicroBenchmark
	public void listIteratorWhileLoop() {
		Iterator<Object> iter = objList.iterator();
		while(iter.hasNext()) { Object obj = iter.next(); }
	}

	@GenerateMicroBenchmark
	public void arrayStandardForLoop() {
		for(int i = 0; i < ITEMS; i++) { Object obj = objArray[i]; }
	}

	@GenerateMicroBenchmark
	public void arrayBasedIteratorOptimizedForLoop() {
		for(Object obj : arrayIterable) {}
	}

	@GenerateMicroBenchmark
	public void arrayBasedIteratorResettingWhileLoop() {
		objIter.reset();
		while(objIter.hasNext()) { Object obj = objIter.next(); }
	}

	@GenerateMicroBenchmark
	public void linkedNodeForLoop() {
		for(LinkedNode node = startNode; null != node.next; node = node.next) { Object obj = node.obj; }
	}

	@GenerateMicroBenchmark
	public void linkedNodeDoWhile() {
		LinkedNode node = startNode;
		do { Object obj = node.obj; } while(null != (node = node.next));
	}

	@GenerateMicroBenchmark
	public void linkedNodeIteratorOptimizedForLoop() {
		for(Object obj : linkedNodeIterable) {}
	}

	@GenerateMicroBenchmark
	public void linkedNodeResettingIteratorWhileLoop() {
		linkedNodeIter.reset();
		while(linkedNodeIter.hasNext()) { Object obj = linkedNodeIter.next(); }
	}

	static class LinkedNode {
		final Object     obj;
		final LinkedNode prev;
		LinkedNode next;

		LinkedNode(Object obj, LinkedNode prev) {
			this.obj = obj;
			this.prev = prev;
		}
	}

	static class LinkedNodeIterator implements Iterator<Object> {
		final    LinkedNode startNode;
		volatile LinkedNode currentNode;

		LinkedNodeIterator(LinkedNode startNode) {
			this.startNode = startNode;
			this.currentNode = startNode;
		}

		public void reset() {
			currentNode = startNode;
		}

		@Override
		public boolean hasNext() {
			return null != currentNode.next;
		}

		@Override
		public Object next() {
			currentNode = currentNode.next;
			return currentNode.obj;
		}
	}

	static class ArrayBasedIterator implements Iterator<Object> {
		private final Object[] values;
		private final int      length;
		private int idx = 0;

		ArrayBasedIterator(Object[] values) {
			this.values = values;
			this.length = values.length;
		}

		public void reset() {
			idx = 0;
		}

		@Override
		public boolean hasNext() {
			return idx < length;
		}

		@Override
		public Object next() {
			try {
				return values[nextIdx()];
			} catch(ArrayIndexOutOfBoundsException ignored) {
				return null;
			}
		}

		private int nextIdx() {
			return idx > -1 ? idx++ : -1;
		}
	}

}

