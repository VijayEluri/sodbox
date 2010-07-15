package info.freelibrary.sodbox;

import java.util.Iterator;
import java.util.ArrayList;

/**
 * Interface combining both Iterable and Iterator functionality
 */
public abstract class IterableIterator<T> implements Iterable<T>, Iterator<T> {
	/**
	 * This class itself is iterator
	 */
	public Iterator<T> iterator() {
		return this;
	}

	/**
	 * Get first selected object. This method can be used when single selected
	 * object is needed. Please notice, that this method doesn't check if
	 * selection contain more than one object
	 * 
	 * @return first selected object or null if selection is empty
	 */
	public T first() {
		return hasNext() ? next() : null;
	}

	/**
	 * Get number of selected objects
	 * 
	 * @return selection size
	 */
	public int size() {
		int count = 0;

		for (@SuppressWarnings("unused")
		T object : this) {
			count += 1;
		}

		return count;
	}

	/**
	 * Convert selection to array list
	 * 
	 * @return array list with the selected objects
	 */
	ArrayList<T> toList() {
		ArrayList<T> list = new ArrayList<T>();

		for (T obj : this) {
			list.add(obj);
		}

		return list;
	}
}
