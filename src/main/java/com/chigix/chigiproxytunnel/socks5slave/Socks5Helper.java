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
import java.io.IOException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Socks5Helper {

    public static void responseForRequest(Channel channel, byte status) throws IOException {
        Application.getLogger(Socks5Helper.class.getName()).debug("RESPONSE REQUEST: 5," + status + ",0,1");
        channel.getOutputStream().write(new byte[]{5, status, 0, 1});
        channel.getOutputStream().write(channel.getLocalAddress());
        int pos_1 = channel.getLocalPort() / 256;
        int pos_2 = channel.getLocalPort() - pos_1 * 256;
        channel.getOutputStream().write(pos_1);
        channel.getOutputStream().write(pos_2);
    }
}
