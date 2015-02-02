package com.chigix.bio.proxy.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ReadBuffer<T> {

    private int length = 0;
    private LinkedList<T> queue;

    public ReadBuffer() {
        this.clear();
    }

    public void push(T value) {
        this.length++;
        this.queue.add(value);
    }

    public final void clear() {
        this.length = 0;
        this.queue = new LinkedList<>();
    }

    public final List<T> toArrayCopy() {
        return new ArrayList<>(this.queue);
    }

}
