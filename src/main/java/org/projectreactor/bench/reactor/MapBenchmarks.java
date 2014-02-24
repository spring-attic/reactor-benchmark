package org.projectreactor.bench.reactor;

import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jon Brisbin
 */
@State(Scope.Thread)
public class MapBenchmarks {

	static int ITEMS      = 1000000;
	static int PARTITIONS = 1000;

	int[]                          nums;
	Map<Integer, Object>           intMap;
	ObservableMap<Integer, Object> observableMap;
	int[][]                        partitions;

	@SuppressWarnings("unchecked")
	@Setup
	public void setup() {
		nums = new int[ITEMS];
		intMap = new HashMap<Integer, Object>();
		observableMap = new ObservableMap<Integer, Object>();
		partitions = new int[PARTITIONS][ITEMS / PARTITIONS];

		for(int i = 0; i < ITEMS; i++) {
			intMap.put(i, new Object());
			observableMap.put(i, new Object());

			int partitionId = i % PARTITIONS;
			int valueIdx = i % (ITEMS / PARTITIONS);
			partitions[partitionId][valueIdx] = i;
		}
		observableMap.flush();
	}

	@GenerateMicroBenchmark
	public void hashMapIntegerKey() {
		for(int i = 0; i < ITEMS; i++) {
			Integer key = i;
			Object obj = intMap.get(key);
		}
	}

	@GenerateMicroBenchmark
	public void observableMapIntegerKey() {
		for(int i = 0; i < ITEMS; i++) {
			Integer key = i;
			Object obj = intMap.get(key);
		}
	}

	@GenerateMicroBenchmark
	public void bogStandardBinarySearch() {
		for(int i = 0; i < ITEMS; i++) {
			Arrays.binarySearch(nums, i);
		}
	}

	@GenerateMicroBenchmark
	public void optimizedBinarySearch() {
		for(int i = 0; i < ITEMS; i++) {
			binarySearch(nums, i, 0, ITEMS);
		}
	}

	@GenerateMicroBenchmark
	public void partitionedOptimizedBinarySearch() {
		int size = ITEMS / PARTITIONS;
		for(int i = 0; i < ITEMS; i++) {
			int partitionId = i % PARTITIONS;
			binarySearch(partitions[partitionId], i, 0, size);
		}
	}

	/**
	 * Optimized binarySearch algorithm originally authored by:
	 * <a href="http://ochafik.com/blog/?p=106">Ché zOlive</a>
	 * <p>
	 * Searches a sorted int array for a specified value,
	 * using an optimized binary search algorithm (which tries to guess
	 * smart pivots).<br />
	 * The result is unspecified if the array is not sorted.<br />
	 * The method returns an index where key was found in the array.
	 * If the array contains duplicates, this might not be the first occurrence.
	 * </p>
	 *
	 * @param array
	 * 		sorted array of integers
	 * @param key
	 * 		value to search for in the array
	 * @param offset
	 * 		index of the first valid value in the array
	 * @param length
	 * 		number of valid values in the array
	 *
	 * @return index of an occurrence of key in array,
	 * or -(insertionIndex + 1) if key is not contained in array (<i>insertionIndex</i> is then the index at which key
	 * could be inserted).
	 *
	 * @see java.util.Arrays#sort(byte[])
	 * @see java.util.Arrays#binarySearch(byte[], byte)
	 */
	public static int binarySearch(int[] array, int key, int offset, int length) {//min, int max) {
		if(length == 0) {
			return -1 - offset;
		}
		int min = offset, max = offset + length - 1;
		int minVal = array[min], maxVal = array[max];

		int nPreviousSteps = 0;

		// Uncomment these two lines to get statistics about the average number of steps in the test report :
		//totalCalls++;
		for(; ; ) {
			//totalSteps++;

			// be careful not to compute key - minVal, for there might be an integer overflow.
			if(key <= minVal) return key == minVal ? min : -1 - min;
			if(key >= maxVal) return key == maxVal ? max : -2 - max;

			assert min != max;

			int pivot;
			// A typical binarySearch algorithm uses pivot = (min + max) / 2.
			// The pivot we use here tries to be smarter and to choose a pivot close to the expected location of the key.
			// This reduces dramatically the number of steps needed to get to the key.
			// However, it does not work well with a logarithmic distribution of values, for instance.
			// When the key is not found quickly the smart way, we switch to the standard pivot.
			if(nPreviousSteps > 2) {
				pivot = (min + max) >> 1;
				// stop increasing nPreviousSteps from now on
			} else {
				// NOTE: We cannot do the following operations in int precision, because there might be overflows.
				//       long operations are slower than float operations with the hardware this was tested on (intel core duo
				// 2, JVM 1.6.0).
				//       Overall, using float proved to be the safest and fastest approach.
				pivot = min + (int)((key - (float)minVal) / (maxVal - (float)minVal) * (max - min));
				nPreviousSteps++;
			}

			int pivotVal = array[pivot];

			// NOTE: do not store key - pivotVal because of overflows
			if(key > pivotVal) {
				min = pivot + 1;
				max--;
			} else if(key == pivotVal) {
				return pivot;
			} else {
				min++;
				max = pivot - 1;
			}
			maxVal = array[max];
			minVal = array[min];
		}
	}

	static class ObservableMap<K, V> implements Map<K, V> {

		final List<Node<K, V>> nodes   = new ArrayList<Node<K, V>>();
		final AtomicLong       version = new AtomicLong();
		final AtomicBoolean    bulkPut = new AtomicBoolean(false);

		int[] hashes;
		int   size;
		K[]   keys;
		V[]   values;

		volatile long lastVersion = Long.MIN_VALUE;

		@SuppressWarnings("unchecked")
		ObservableMap() {
			clear();
		}

		public boolean startBulkPut() {
			return this.bulkPut.compareAndSet(false, true);
		}

		public boolean commitBulkPut() {
			if(this.bulkPut.compareAndSet(true, false)) {
				flush();
				return true;
			} else {
				return false;
			}
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean isEmpty() {
			return size == 0;
		}

		@Override
		public boolean containsKey(Object key) {
			int hashCode = key.hashCode();
			return binarySearch(hashes, hashCode, 0, size) > -1;
		}

		@Override
		public boolean containsValue(Object value) {
			return Arrays.binarySearch(values, value) > -1;
		}

		@SuppressWarnings("unchecked")
		@Override
		public V get(Object key) {
			if(lastVersion < version.get()) {
				flush();
			}
			int hashCode = key.hashCode();
			int idx = binarySearch(hashes, hashCode, 0, size);
			return (idx > -1 ? values[idx] : null);
		}

		@Override
		public V put(K key, V value) {
			version.incrementAndGet();
			Node<K, V> node = findNode(key.hashCode());
			if(null != node) {
				V oldVal = node.getValue();
				node.setValue(value);
				return oldVal;
			} else {
				nodes.add(new Node<K, V>(key.hashCode(), key, value));
				return null;
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public V remove(Object key) {
			version.incrementAndGet();
			Node<K, V> node = findNode(key.hashCode());
			if(null != node) {
				node.delete();
				return node.getValue();
			} else {
				return null;
			}
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> m) {
			for(Entry<? extends K, ? extends V> entry : m.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void clear() {
			nodes.clear();
			version.set(0);
			lastVersion = Long.MIN_VALUE;
			hashes = new int[0];
			keys = (K[])new Object[0];
			values = (V[])new Object[0];
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<K> keySet() {
			return new HashSet<K>(Arrays.asList(keys));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Collection<V> values() {
			return new HashSet<V>(Arrays.asList(values));
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<Entry<K, V>> entrySet() {
			return new HashSet<Entry<K, V>>(nodes);
		}

		private Node<K, V> findNode(int hashCode) {
			int idx = binarySearch(hashes, hashCode, 0, size);
			return (idx < 0 ? null : nodes.get(idx));
		}

		@SuppressWarnings("unchecked")
		private void flush() {
			Collections.sort(nodes);
			this.size = nodes.size();

			hashes = new int[size];
			keys = (K[])new Object[size];
			values = (V[])new Object[size];
			for(int i = 0; i < this.size; i++) {
				Node<K, V> node = nodes.get(i);
				if(node.isDeleted()) {
					continue;
				}
				if(i > 0 && hashes[i - 1] == node.getHashCode()) {
					continue;
				}
				hashes[i] = node.getHashCode();
				keys[i] = node.getKey();
				values[i] = node.getValue();
			}

			lastVersion = version.get();
		}

		private class Node<K, V> implements Entry<K, V>, Comparable<Node<K, V>> {
			private final    Integer hashCode;
			private final    K       key;
			private volatile V       value;
			private volatile boolean deleted = false;

			private Node(int hashCode, K key, V value) {
				this.hashCode = hashCode;
				this.key = key;
				this.value = value;
			}

			private void delete() {
				deleted = true;
			}

			public boolean isDeleted() {
				return deleted;
			}

			public Integer getHashCode() {
				return hashCode;
			}

			@Override
			public K getKey() {
				return key;
			}

			@Override
			public V getValue() {
				return (deleted ? null : value);
			}

			@Override
			public V setValue(V value) {
				this.value = value;
				return value;
			}

			@Override
			public int hashCode() {
				return hashCode;
			}

			@SuppressWarnings("unchecked")
			@Override
			public boolean equals(Object obj) {
				if(!(obj instanceof Node)) {
					return false;
				}
				return hashCode.equals(((Node)obj).getHashCode());
			}

			@Override
			public int compareTo(Node<K, V> n) {
				if(deleted) {
					return -1;
				} else {
					return hashCode.compareTo(n.hashCode);
				}
			}
		}
	}

}
