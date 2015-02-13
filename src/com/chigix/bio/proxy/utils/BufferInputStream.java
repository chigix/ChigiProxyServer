package com.chigix.bio.proxy.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class BufferInputStream extends InputStream {

    private final ConcurrentLinkedQueue<Integer> buffer;
    private boolean isEnded = false;

    public BufferInputStream() {
        this.buffer = new ConcurrentLinkedQueue<Integer>();
    }

    @Override
    public int read() throws IOException {
        Integer read = -1;
        while ((read = this.buffer.poll()) == null) {
            if (this.isEnded) {
                return -1;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
        return read;
    }

    public void push(int read) {
        this.buffer.add(read);
    }

    public void end() {
        this.isEnded = true;
    }

    public int size() {
        return this.buffer.size();
    }

    /**
     * Removes all of the elements from this buffer. The buffer will be empty
     * after this call returns.
     */
    public void clear() {
        this.buffer.clear();
    }
}
