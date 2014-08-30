package org.jrenner.fps;

import com.badlogic.gdx.utils.Array;

/** An Array that will automatically remove oldest items when beyond max size */
public class RollingArray {

	public RollingArray() {

	}

	public RollingArray(int maxSize) {
		this.maxSize = maxSize;
	}

	Array<Integer> items = new Array<>();
	public int maxSize = 60;

	public void clear() {
		items.clear();
	}

	public Array<Integer> getItems() {
		return items;
	}

	public void add(int item) {
		items.add(item);
		if (items.size > maxSize) {
			items.removeIndex(0);
		}
	}

	public float getAverage() {
		float total = 0;
		for (int item : items) {
			total += item;
		}
		return total / items.size;
	}
}
