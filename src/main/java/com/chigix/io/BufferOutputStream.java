/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class BufferOutputStream extends OutputStream {

    private final ConcurrentLinkedQueue<Integer> buffer;

    private boolean isConnectionClosed;

    private final Object bufferOffered;

    public BufferOutputStream() {
        this.isConnectionClosed = false;
        this.bufferOffered = new Object();
        this.buffer = new ConcurrentLinkedQueue<Integer>() {
            @Override
            public boolean offer(Integer e) {
                synchronized (bufferOffered) {
                    if (!super.offer(e)) {
                        return false;
                    }
                    bufferOffered.notifyAll();
                }
                return true;
            }
        };
    }

    @Override
    public void write(int b) throws IOException {
        if (isConnectionClosed) {
            throw new IOException("Writing rejection because of buffer closed.");
        }
        this.buffer.offer(b);
    }

    public int poll() {
        Integer read = -1;
        synchronized (this.bufferOffered) {
            while ((read = this.buffer.poll()) == null) {
                if (this.isConnectionClosed) {
                    return -1;
                }
                try {
                    this.bufferOffered.wait();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        return read;
    }

    public int size() {
        return this.buffer.size();
    }

    @Override
    public void close() throws IOException {
        this.isConnectionClosed = true;
    }

    public void clear() {
        this.buffer.clear();
    }

}
