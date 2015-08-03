/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.io;

import com.chigix.chigiproxytunnel.Application;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class BufferInputStream extends InputStream {

    private final BlockingQueue<Integer> buffer;

    private boolean isConnectionClosed;

    public BufferInputStream() {
        this.buffer = new LinkedTransferQueue<>();
        this.isConnectionClosed = false;
    }

    @Override
    public int read() throws IOException {
        Integer read = -1;
        try {
            while ((read = this.buffer.poll(1000, TimeUnit.MILLISECONDS)) == null) {
                Application.getLogger(getClass().getName()).debug("POLL [" + this.buffer.hashCode() + "] " + read);
                if (this.isConnectionClosed) {
                    return -1;
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(BufferInputStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return read;
    }

    public void offer(int read) throws IOException {
        if (isConnectionClosed) {
            throw new IOException("Offering rejected because of buffer closed.");
        }
        Application.getLogger(getClass().getName()).debug("OFFER [" + this.buffer.hashCode() + "] " + this.buffer.offer(read));
    }

    @Override
    public void close() throws IOException {
        this.isConnectionClosed = true;
    }

    public int size() {
        return this.buffer.size();
    }

    public void clear() {
        this.buffer.clear();
    }

}
