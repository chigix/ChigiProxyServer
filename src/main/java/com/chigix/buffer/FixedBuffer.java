/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.buffer;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 * @param <T>
 */
public class FixedBuffer<T> {

    private final Object[] buffer;

    private final int capacity;

    private int toWritePointer;

    private int offerCount;

    public FixedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.toWritePointer = 0;
        this.offerCount = 0;
    }

    public void offer(T e) {
        synchronized (this.buffer) {
            if (this.toWritePointer == this.capacity) {
                this.toWritePointer = 0;
            }
            this.buffer[this.toWritePointer++] = e;
            this.offerCount++;
        }
    }

    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); The runtime type of the returned
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

    public int getCapacity() {
        return capacity;
    }

    public int getOfferCount() {
        return offerCount;
    }

}
