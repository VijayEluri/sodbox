
package info.freelibrary.sodbox.impl;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;

import info.freelibrary.sodbox.EmbeddedLink;
import info.freelibrary.sodbox.ICloneable;
import info.freelibrary.sodbox.Link;
import info.freelibrary.sodbox.PersistentIterator;

public class LinkImpl<T> implements EmbeddedLink<T>, ICloneable {

    class LinkIterator implements PersistentIterator, ListIterator<T> {

        private int i;

        private int last;

        LinkIterator(final int index) {
            i = index;
            last = -1;
        }

        @Override
        public void add(final T o) {
            insert(i++, o);
            last = -1;
        }

        @Override
        public boolean hasNext() {
            return i < size();
        }

        @Override
        public boolean hasPrevious() {
            return i > 0;
        }

        @Override
        public T next() throws NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            last = i;
            return get(i++);
        }

        @Override
        public int nextIndex() {
            return i;
        }

        @Override
        public int nextOid() throws NoSuchElementException {
            if (!hasNext()) {
                return 0;
            }
            return db.getOid(getRaw(i++));
        }

        @Override
        public T previous() throws NoSuchElementException {
            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            return get(last = --i);
        }

        @Override
        public int previousIndex() {
            return i - 1;
        }

        @Override
        public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            removeObject(last);
            if (last < i) {
                i -= 1;
            }
        }

        @Override
        public void set(final T o) {
            if (last < 0) {
                throw new IllegalStateException();
            }
            setObject(last, o);
        }
    }

    static class SubList<T> extends AbstractList<T> implements RandomAccess {

        private final LinkImpl<T> l;

        private final int offset;

        private int size;

        SubList(final LinkImpl<T> list, final int fromIndex, final int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if (toIndex > list.size()) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            }
            l = list;
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        @Override
        public void add(final int index, final T element) {
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException();
            }
            l.add(index + offset, element);
            size++;
        }

        @Override
        public boolean addAll(final Collection<? extends T> c) {
            return addAll(size, c);
        }

        @Override
        public boolean addAll(final int index, final Collection<? extends T> c) {
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            final int cSize = c.size();
            if (cSize == 0) {
                return false;
            }
            l.addAll(offset + index, c);
            size += cSize;
            return true;
        }

        @Override
        public T get(final int index) {
            rangeCheck(index);
            return l.get(index + offset);
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        @Override
        public ListIterator<T> listIterator(final int index) {
            if (index < 0 || index > size) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
            }
            return new ListIterator<T>() {

                private final ListIterator<T> i = l.listIterator(index + offset);

                @Override
                public void add(final T o) {
                    i.add(o);
                    size++;
                }

                @Override
                public boolean hasNext() {
                    return nextIndex() < size;
                }

                @Override
                public boolean hasPrevious() {
                    return previousIndex() >= 0;
                }

                @Override
                public T next() {
                    if (hasNext()) {
                        return i.next();
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public int nextIndex() {
                    return i.nextIndex() - offset;
                }

                @Override
                public T previous() {
                    if (hasPrevious()) {
                        return i.previous();
                    } else {
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public int previousIndex() {
                    return i.previousIndex() - offset;
                }

                @Override
                public void remove() {
                    i.remove();
                    size--;
                }

                @Override
                public void set(final T o) {
                    i.set(o);
                }
            };
        }

        private void rangeCheck(final int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index: " + index + ",Size: " + size);
            }
        }

        @Override
        public T remove(final int index) {
            rangeCheck(index);
            final T result = l.remove(index + offset);
            size--;
            return result;
        }

        @Override
        protected void removeRange(final int fromIndex, final int toIndex) {
            l.removeRange(fromIndex + offset, toIndex + offset);
            size -= toIndex - fromIndex;
        }

        @Override
        public T set(final int index, final T element) {
            rangeCheck(index);
            return l.set(index + offset, element);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public List<T> subList(final int fromIndex, final int toIndex) {
            return new SubList<T>(l, offset + fromIndex, offset + toIndex);
        }
    }

    Object[] arr;

    int used;

    transient Object owner;

    transient StorageImpl db;

    LinkImpl() {
    }

    public LinkImpl(final StorageImpl db, final int initSize) {
        this.db = db;
        arr = new Object[initSize];
    }

    public LinkImpl(final StorageImpl db, final Link link, final Object owner) {
        this.db = db;
        used = link.size();
        arr = new Object[used];
        System.arraycopy(arr, 0, link.toRawArray(), 0, used);
        this.owner = owner;
    }

    public LinkImpl(final StorageImpl db, final T[] arr, final Object owner) {
        this.db = db;
        this.arr = arr;
        this.owner = owner;
        used = arr.length;
    }

    @Override
    public void add(final int i, final T obj) {
        insert(i, obj);
    }

    @Override
    public boolean add(final T obj) {
        reserveSpace(1);
        arr[used++] = obj;
        modify();
        return true;
    }

    @Override
    public boolean addAll(final Collection<? extends T> c) {
        boolean modified = false;
        final Iterator<? extends T> e = c.iterator();
        while (e.hasNext()) {
            if (add(e.next())) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean addAll(int index, final Collection<? extends T> c) {
        boolean modified = false;
        final Iterator<? extends T> e = c.iterator();
        while (e.hasNext()) {
            add(index++, e.next());
            modified = true;
        }
        return modified;
    }

    @Override
    public boolean addAll(final Link<T> link) {
        final int n = link.size();
        reserveSpace(n);
        for (int i = 0, j = used; i < n; i++, j++) {
            arr[j] = link.getRaw(i);
        }
        used += n;
        modify();
        return true;
    }

    @Override
    public void addAll(final T[] a) {
        addAll(a, 0, a.length);
    }

    @Override
    public void addAll(final T[] a, final int from, final int length) {
        reserveSpace(length);
        System.arraycopy(a, from, arr, used, length);
        used += length;
        modify();
    }

    @Override
    public void clear() {
        for (int i = used; --i >= 0;) {
            arr[i] = null;
        }
        used = 0;
        modify();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();

    }

    @Override
    public boolean contains(final Object obj) {
        return indexOf(obj) >= 0;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        final Iterator<?> e = c.iterator();
        while (e.hasNext()) {
            if (!contains(e.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsElement(final int i, final T obj) {
        final Object elem = arr[i];
        return elem == obj || elem != null && db.getOid(elem) == db.getOid(obj);
    }

    @Override
    public boolean containsObject(final T obj) {
        return indexOfObject(obj) >= 0;
    }

    @Override
    public void deallocateMembers() {
        for (int i = used; --i >= 0;) {
            db.deallocate(arr[i]);
            arr[i] = null;
        }
        used = 0;
        modify();
    }

    @Override
    public T get(final int i) {
        if (i < 0 || i >= used) {
            throw new IndexOutOfBoundsException();
        }
        return loadElem(i);
    }

    @Override
    public Object getOwner() {
        return owner;
    }

    @Override
    public Object getRaw(final int i) {
        if (i < 0 || i >= used) {
            throw new IndexOutOfBoundsException();
        }
        return arr[i];
    }

    @Override
    public int indexOf(final Object obj) {
        if (obj == null) {
            for (int i = 0, n = used; i < n; i++) {
                if (arr[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0, n = used; i < n; i++) {
                if (obj.equals(loadElem(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int indexOfObject(final Object obj) {
        final Object[] a = arr;
        final int oid = db.getOid(obj);
        if (oid != 0) {
            for (int i = 0, n = used; i < n; i++) {
                if (db.getOid(a[i]) == oid) {
                    return i;
                }
            }
        } else {
            for (int i = 0, n = used; i < n; i++) {
                if (a[i] == obj) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void insert(final int i, final T obj) {
        if (i < 0 || i > used) {
            throw new IndexOutOfBoundsException();
        }
        reserveSpace(1);
        System.arraycopy(arr, i, arr, i + 1, used - i);
        arr[i] = obj;
        used += 1;
        modify();
    }

    @Override
    public boolean isEmpty() {
        return used == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new LinkIterator(0);
    }

    @Override
    public int lastIndexOf(final Object obj) {
        if (obj == null) {
            for (int i = used; --i >= 0;) {
                if (arr[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = used; --i >= 0;) {
                if (obj.equals(loadElem(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOfObject(final Object obj) {
        final Object[] a = arr;
        final int oid = db.getOid(obj);
        if (oid != 0) {
            for (int i = used; --i >= 0;) {
                if (db.getOid(a[i]) == oid) {
                    return i;
                }
            }
        } else {
            for (int i = used; --i >= 0;) {
                if (a[i] == obj) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(final int index) {
        return new LinkIterator(index);
    }

    private final T loadElem(final int i) {
        Object elem = arr[i];
        if (elem != null && db.isRaw(elem)) {
            elem = db.lookupObject(db.getOid(elem), null);
        }
        return (T) elem;
    }

    private final void modify() {
        if (owner != null) {
            db.modify(owner);
        }
    }

    @Override
    public void pin() {
        for (int i = 0, n = used; i < n; i++) {
            arr[i] = loadElem(i);
        }
    }

    @Override
    public T remove(final int i) {
        if (i < 0 || i >= used) {
            throw new IndexOutOfBoundsException();
        }
        final T obj = loadElem(i);
        used -= 1;
        System.arraycopy(arr, i + 1, arr, i, used - i);
        arr[used] = null;
        modify();
        return obj;
    }

    @Override
    public boolean remove(final Object o) {
        final int i = indexOf(o);
        if (i >= 0) {
            remove(i);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        boolean modified = false;
        final Iterator<?> e = iterator();
        while (e.hasNext()) {
            if (c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void removeObject(final int i) {
        if (i < 0 || i >= used) {
            throw new IndexOutOfBoundsException();
        }
        used -= 1;
        System.arraycopy(arr, i + 1, arr, i, used - i);
        arr[used] = null;
        modify();
    }

    protected void removeRange(final int fromIndex, final int toIndex) {
        int size = used;
        final int numMoved = size - toIndex;
        System.arraycopy(arr, toIndex, arr, fromIndex, numMoved);

        // Let gc do its work
        final int newSize = size - (toIndex - fromIndex);
        while (size != newSize) {
            arr[--size] = null;
        }
        used = size;
        modify();
    }

    void reserveSpace(final int len) {
        if (used + len > arr.length) {
            final Object[] newArr = new Object[used + len > arr.length * 2 ? used + len : arr.length * 2];
            System.arraycopy(arr, 0, newArr, 0, used);
            arr = newArr;
        }
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        boolean modified = false;
        final Iterator<T> e = iterator();
        while (e.hasNext()) {
            if (!c.contains(e.next())) {
                e.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public T set(final int i, final T obj) {
        if (i < 0 || i >= used) {
            throw new IndexOutOfBoundsException();
        }
        final T prev = loadElem(i);
        arr[i] = obj;
        modify();
        return prev;
    }

    @Override
    public void setObject(final int i, final T obj) {
        if (i < 0 || i >= used) {
            throw new IndexOutOfBoundsException();
        }
        arr[i] = obj;
        modify();
    }

    @Override
    public void setOwner(final Object obj) {
        owner = obj;
    }

    @Override
    public void setSize(final int newSize) {
        if (newSize < used) {
            for (int i = used; --i >= newSize; arr[i] = null) {
                ;
            }
        } else {
            reserveSpace(newSize - used);
        }
        used = newSize;
        modify();
    }

    @Override
    public int size() {
        return used;
    }

    @Override
    public List<T> subList(final int fromIndex, final int toIndex) {
        return new SubList<T>(this, fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        final Object[] a = new Object[used];
        for (int i = used; --i >= 0;) {
            a[i] = loadElem(i);
        }
        return a;
    }

    @Override
    public <T> T[] toArray(T[] arr) {
        if (arr.length < used) {
            arr = (T[]) Array.newInstance(arr.getClass().getComponentType(), used);
        }
        for (int i = used; --i >= 0;) {
            arr[i] = (T) loadElem(i);
        }
        if (arr.length > used) {
            arr[used] = null;
        }
        return arr;
    }

    @Override
    public Object[] toRawArray() {
        return arr;
    }

    @Override
    public void unpin() {
        for (int i = 0, n = used; i < n; i++) {
            final Object elem = arr[i];
            if (elem != null && db.isLoaded(elem)) {
                arr[i] = new PersistentStub(db, db.getOid(elem));
            }
        }
    }
}
