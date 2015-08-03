/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.socks5slave;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.BufferProcessor;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Socks5Init extends BufferProcessor {

    private Processor next;

    public Socks5Init() {
        super(100);
        this.initProcessorsChain();
    }

    protected void initProcessorsChain() {
    }

    public void setNext(Processor next) {
        this.next = next;
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        if (buffer.size() < 3) {
            return this;
        }
        if (buffer.get(0) != 5) {
            Application.getLogger(getClass().getName()).fatal(ChannelNameMap.getName(channel) + "\tINVALID SOCKS5 GREETING.");
            channel.close();
            return null;
        }
        if (buffer.size() < (2 + buffer.get(1))) {
            return this;
        }
        try {
            channel.getOutputStream().write(new byte[]{5, 0});
            channel.getOutputStream().flush();
        } catch (IOException ex) {
            Application.getLogger(getClass().getName()).fatal(ChannelNameMap.getName(channel) + "\tCLOSED BY CLIENT.");
            channel.close();
            return null;
        } finally {
            buffer.clear();
        }
        return this.next;
    }

}
