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
public class Socks5RequestTypeDetect extends BufferProcessor {

    private Socks5AddressTypeDetect addressTypeDetector;

    public Socks5RequestTypeDetect() {
        super(1024);
    }

    public void setAddressTypeDetector(Socks5AddressTypeDetect addressTypeDetector) {
        this.addressTypeDetector = addressTypeDetector;
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        if (buffer.size() < 2) {
            return this;
        }
        if (buffer.get(0) != 5) {
            Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(channel) + "\tINVALID SOCKS5 VERSION." + buffer);
            channel.close();
            return null;
        }
        if (buffer.get(1) == 2) {
            Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(channel) + "\tTCP/IP port binding NOT SUPPORTED.");
        }
        if (buffer.get(1) == 3) {
            Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(channel) + "\tUDP port NOT SUPPORTED.");
        }
        if (buffer.get(1) != 1) {
            try {
                Socks5Helper.responseForRequest(channel, (byte) 7);
            } catch (IOException ex) {
                channel.close();
            }
        }
        buffer.clear();
        return this.addressTypeDetector;
    }

}
