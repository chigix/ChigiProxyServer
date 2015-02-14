package com.chigix.bio.proxy.buffer;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class FixedBuffer<T> {

    private final Object[] buffer;

    private final int capacity;

    private int toWritePointer;

    public FixedBuffer(int capacity) {
        this.buffer = new Object[capacity];
        this.capacity = capacity;
        this.toWritePointer = 0;
    }

    public void offer(T e) {
        synchronized (this.buffer) {
            if (this.toWritePointer == this.capacity) {
                this.toWritePointer = 0;
            }
            this.buffer[this.toWritePointer++] = e;
        }
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element);The runtime type of the returned
     * array is that of the specified array.
     *
     * @param a
     * @return
     */
    public T[] toArray(T[] a) {
        int firstElementPos = this.toWritePointer;
        if (firstElementPos == this.capacity) {
            firstElementPos = 0;
        }
        for (int i = 0; i < this.buffer.length; i++) {
            int from_index = i;
            if (from_index < firstElementPos) {
                from_index = from_index + this.capacity;
            }
            if (from_index - firstElementPos >= a.length) {
                continue;
            }
            a[from_index - firstElementPos] = (T) this.buffer[i];
        }
        return (T[]) a;
    }

}
