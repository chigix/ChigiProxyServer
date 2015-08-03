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
public class Socks5AddressTypeDetect extends BufferProcessor {

    private AddressIpv4Read ipv4Read;

    private AddressIpv6Read ipv6Read;

    private AddressDomainRead domainRead;

    public Socks5AddressTypeDetect() {
        super(1024);
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) {
        if (buffer.size() < 2) {
            return this;
        }
        if (buffer.get(0) != 0) {
            try {
                Socks5Helper.responseForRequest(channel, (byte) 2);
            } catch (IOException ex) {
            } finally {
                channel.close();
            }
            return null;
        }
        Application.getLogger(getClass().getName()).debug("ADDRESS TYPE: " + buffer.get(1));
        switch (buffer.get(1)) {
            case 1:
                buffer.clear();
                return this.ipv4Read;
            case 3:
                buffer.clear();
                return this.domainRead;
            case 4:
                buffer.clear();
                return this.ipv6Read;
            default:
                try {
                    Socks5Helper.responseForRequest(channel, (byte) 1);
                } catch (IOException ex) {
                } finally {
                    channel.close();
                }
        }
        buffer.clear();
        return null;
    }

    public void setIpv4Read(AddressIpv4Read ipv4Read) {
        this.ipv4Read = ipv4Read;
    }

    public void setIpv6Read(AddressIpv6Read ipv6Read) {
        this.ipv6Read = ipv6Read;
    }

    public void setDomainRead(AddressDomainRead domainRead) {
        this.domainRead = domainRead;
    }
    
    

}
