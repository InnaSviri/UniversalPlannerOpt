package ru.programpark.entity.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ConcatIterator<T> implements Iterator<T> {

    private int offset;
    private Iterator<T>[] parts;

    public ConcatIterator(Iterator<T>... parts) {
        this.parts = parts;
        this.offset = 0;
    }

    @Override
    public boolean hasNext() {
        while (offset < parts.length) {
            if (parts[offset].hasNext()) return true;
            ++offset;
        }
        return false;
    }

    @Override
    public T next() {
        if (hasNext()) {
            return parts[offset].next();
        } else {
            throw new NoSuchElementException("All part iterators exhausted");
        }
    }
    
    @Override public void remove() {
        throw new UnsupportedOperationException();
    }

}
