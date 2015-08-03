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

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public abstract class ChannelBoundHandler extends ProcessorHandler {

    public ChannelBoundHandler() {
        super(new ChannelBoundProcessor());
    }

    public void registerBound(Channel from, Channel to) throws ChannelBoundleException {
        try {
            ((ChannelBoundProcessor) this.getInitProcessor()).registerBound(from, to);
        } catch (ChannelBoundleException channelBoundleException) {
            throw channelBoundleException;
        }
    }

    @Override
    public void channelActive(Channel channel) throws Exception {
        super.channelActive(channel);
    }
    

}
