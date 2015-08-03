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
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class AddressIpv6Read extends BufferProcessor {

    public AddressIpv6Read() {
        super(8);
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        try {
            Socks5Helper.responseForRequest(channel, (byte) 8);
        } catch (IOException ex) {
        }
        Application.getLogger(getClass().getName()).fatal(channel.getRemoteHostAddress() + " IPV6 Target NOT SUPPORTED.");
        return null;
    }

}
