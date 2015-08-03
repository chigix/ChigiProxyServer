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
public class AddressIpv4Read extends BufferProcessor {

    public PortRead portReader;

    public AddressIpv4Read() {
        super(5);
    }

    public PortRead getPortReader() {
        return portReader;
    }

    public void setPortReader(PortRead portReader) {
        this.portReader = portReader;
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        if (buffer.size() < 4) {
            return this;
        }
        this.portReader.targetHost.set((buffer.get(0) + "." + buffer.get(1) + "." + buffer.get(2) + "." + buffer.get(3)));
        buffer.clear();
        return this.portReader;
    }

}
