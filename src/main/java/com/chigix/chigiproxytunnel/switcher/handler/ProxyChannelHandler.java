/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.handler;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.handler.ProcessorHandler;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProxyChannelHandler extends ProcessorHandler {

    public ProxyChannelHandler(SwitcherTable switcher) {
        super(createInitProcessor(switcher));
    }

    private static Processor createInitProcessor(SwitcherTable switcher) {
        return null;
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable cause) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
