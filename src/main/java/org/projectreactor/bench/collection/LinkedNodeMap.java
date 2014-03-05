/*
 * Copyright (c) 2011-2014 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.projectreactor.bench.collection;

import reactor.data.core.collection.UnsafeUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Implementation of {@link java.util.Map} that uses a spine of a given length that keeps track of key-value pairs by
 * linking nodes together along the spine.
 *
 * @author Jon Brisbin
 */
class LinkedNodeMap<K, V> extends AbstractMap<K, V> {

	private static final Unsafe U = UnsafeUtils.getUnsafe();

	private final int                                 spineLength;
	private final int                                 spineLengthMinus1;
	private final AtomicReferenceArray<Nodelet<K, V>> spine;

	private volatile int size = 0;

	public LinkedNodeMap(int spineLength) {
		this.spineLength = spineLength;
		this.spineLengthMinus1 = spineLength - 1;
		spine = new AtomicReferenceArray<>(spineLength);
		for(int i = 0; i < spine.length(); i++) {
			spine.set(i, new Nodelet<K, V>());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		int h = key.hashCode();
		Nodelet<K, V> n = spine.get(spineLengthMinus1 & h);
		LinkedNodeEntry<K, V> e, prev;
		if(null == (prev = e = n.head)) { return null; }

		while(h > e.hashCode
				&& null != (e = (LinkedNodeEntry<K, V>)U.getObjectVolatile(e, LinkedNodeEntry.NEXT_OFFSET))) {
			prev = e;
		}

		return (prev.hashCode == h ? prev.value : null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public V put(K key, V value) {
		int h = key.hashCode();
		Nodelet<K, V> n = spine.get(spineLengthMinus1 & h);
		LinkedNodeEntry<K, V> e, prev;
		if(null == (prev = e = n.head)) {
			n.head = n.tail = new LinkedNodeEntry<>(key, value);
			size++;
			return null;
		}

		if(n.tail.hashCode < h) {
			n.tail.next = n.tail = new LinkedNodeEntry<>(key, value);
			size++;
			return null;
		}

		while(h > e.hashCode
				&& null != (e = (LinkedNodeEntry<K, V>)U.getObjectVolatile(e, LinkedNodeEntry.NEXT_OFFSET))) {
			prev = e;
		}
		V oldVal = null;
		if(prev.hashCode == h) {
			oldVal = prev.value;
			prev.value = value;
		} else {
			LinkedNodeEntry<K, V> newEntry = new LinkedNodeEntry<>(key, value);
			newEntry.next = prev.next;
			prev.next = newEntry;
			size++;
		}
		return oldVal;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return new AbstractSet<Entry<K, V>>() {
			@Override
			public Iterator<Entry<K, V>> iterator() {
				return new Iterator<Entry<K, V>>() {
					@SuppressWarnings("unchecked")
					LinkedNodeEntry<K, V>[] currNodes = new LinkedNodeEntry[spineLength];
					long count = 0;
					int currIdx = 0;

					@Override
					public boolean hasNext() {
						return (null != currNodes[currIdx].next);
					}

					@Override
					public Entry<K, V> next() {
						LinkedNodeEntry<K, V> e = currNodes[currIdx].next;
						currNodes[currIdx] = e;
						nextIdx();
						return e;
					}

					@Override
					public void remove() {

					}

					private int nextIdx() {
						return (int)(spineLengthMinus1 & count++);
					}
				};
			}

			@Override
			public int size() {
				return size;
			}
		};
	}

	private static class Nodelet<K, V> {
		volatile LinkedNodeEntry<K, V> head;
		volatile LinkedNodeEntry<K, V> tail;
	}

	private static class LinkedNodeEntry<K, V> implements Entry<K, V> {
		private static final Field NEXT;
		private static final long  NEXT_OFFSET;

		static {
			try {
				NEXT = LinkedNodeEntry.class.getDeclaredField("next");
				NEXT_OFFSET = U.objectFieldOffset(NEXT);
			} catch(NoSuchFieldException e) {
				throw new IllegalStateException(e);
			}
		}

		private final    int                   hashCode;
		private final    K                     key;
		private volatile V                     value;
		private volatile LinkedNodeEntry<K, V> next;

		private LinkedNodeEntry(K key, V value) {
			this.hashCode = key.hashCode();
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			V oldVal = this.value;
			this.value = value;
			return oldVal;
		}
	}

}
