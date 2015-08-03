/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.socks5slave;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.BufferProcessor;
import com.chigix.chigiproxytunnel.handler.Processor;
import java.util.List;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class AddressDomainRead extends BufferProcessor {

    private PortRead portReader;

    public AddressDomainRead() {
        super(1024);
    }

    public PortRead getPortReader() {
        return portReader;
    }

    public void setPortReader(PortRead portReader) {
        this.portReader = portReader;
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        if (buffer.size() < (1 + buffer.get(0))) {
            return this;
        }
        byte[] address = new byte[buffer.get(0)];
        for (int i = 1; i < buffer.size(); i++) {
            address[i - 1] = buffer.get(i).byteValue();
        }
        this.portReader.targetHost.set(new String(address));
        buffer.clear();
        return this.portReader;
    }

}
