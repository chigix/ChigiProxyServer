/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.handler;

import com.chigix.chigiproxytunnel.channel.Channel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public abstract class BufferProcessor implements Processor {

    private final ThreadLocal<List<Integer>> buffer;

    public BufferProcessor(int capacity) {
        this.buffer = new BufferLocal(capacity);
    }

    @Override
    public final Processor update(Channel channel, int input) throws ProcessorUpdateException {
        List buf = this.buffer.get();
        buf.add(input);
        try {
            return this.processBuffer(channel, buf);
        } catch (BufferProcessException ex) {
            throw new ProcessorUpdateException(ex.getCause());
        }
    }

    protected abstract Processor processBuffer(Channel channel, List<Integer> buffer) throws BufferProcessException;

    private class BufferLocal extends ThreadLocal<List<Integer>> {

        private final int initCapacity;

        public BufferLocal(int initCapacity) {
            super();
            this.initCapacity = initCapacity;
        }

        @Override
        protected List<Integer> initialValue() {
            return new ArrayList<>(initCapacity);
        }

    }

}
