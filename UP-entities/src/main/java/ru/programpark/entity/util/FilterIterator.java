package ru.programpark.entity.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class FilterIterator<T> implements Iterator<T> {

    private Iterator<T> baseIterator;
    private T next;

    public FilterIterator(Iterator<T> baseIterator) {
        this.baseIterator = baseIterator;
        this.next = null;
    }

    public FilterIterator(Iterable<T> base) {
        this.baseIterator = base.iterator();
        this.next = null;
    }

    public boolean test(T element) {
        return true;
    }


    private boolean getNext() {
        while (baseIterator.hasNext()) {
            T next = baseIterator.next();
            if (test(next)) {
                this.next = next;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        return (this.next != null) || getNext();
    }

    @Override
    public T next() throws NoSuchElementException {
        if (next != null || getNext()) {
            T next = this.next;
            this.next = null;
            return next;
        } else {
            throw new NoSuchElementException("Base iterator exhausted");
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
