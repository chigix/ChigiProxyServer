package com.chigix.bio.proxy.handler;

import com.chigix.bio.proxy.channel.Channel;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
abstract public class ChannelHandler {

    private final Channel channel;

    public ChannelHandler(Channel channel) {
        this.channel = channel;
    }

    /**
     *
     * @param channel
     */
    public void channelActive(Channel channel) {
    }

    /**
     *
     * @param channel
     */
    public void channelInactive(Channel channel) {
    }

    /**
     *
     * @param channel
     * @param input
     */
    public void channelRead(Channel channel, int input) {
    }

    public final Channel getChannel() {
        return channel;
    }

}
