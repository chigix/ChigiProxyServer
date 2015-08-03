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
public class HandleChannelException extends Exception {

    private final Channel channel;
    private final ChannelHandler currentlyHandled;

    public HandleChannelException(Channel channel, ChannelHandler currentlyHandled) {
        super("[@" + Integer.toHexString(channel.hashCode()) + "/" + channel.getRemoteHostAddress() + "] CHANNEL Handled by another handler: " + currentlyHandled.getHandlerName());
        this.channel = channel;
        this.currentlyHandled = currentlyHandled;
    }

    public Channel getChannel() {
        return channel;
    }

    public ChannelHandler getCurrentlyHandled() {
        return currentlyHandled;
    }

}
