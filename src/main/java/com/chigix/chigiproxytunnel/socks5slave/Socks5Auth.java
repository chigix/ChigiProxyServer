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
import java.util.List;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Socks5Auth extends BufferProcessor {

    private Processor requestDetector;

    public Socks5Auth() {
        super(1024);
    }

    public void setRequestDetector(Processor requestDetector) {
        this.requestDetector = requestDetector;
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        if (true) {
            return this.requestDetector;
        }
        if (buffer.size() < 5) {
            return this;
        }
        if (buffer.get(0) != 1) {
            Application.getLogger(getClass().getName()).fatal(channel.getRemoteHostAddress() + " INVALID SOCKS5 AUTH VERSION." + buffer);
            channel.close();
            return null;
        }
        if (buffer.size() < (3 + buffer.get(1))) {
            return this;
        }
        if (buffer.size() < (3 + buffer.get(1) + buffer.get(2 + buffer.get(1)))) {
            return this;
        }
        Application.getLogger(getClass().getName()).debug(buffer);
        return null;
    }

}
