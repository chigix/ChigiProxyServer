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
public class ChannelBoundNotExistException extends Exception {

    private final Channel channelSearch;

    public ChannelBoundNotExistException(Channel channelSearch) {
        this.channelSearch = channelSearch;
    }

    public Channel getChannelSearch() {
        return channelSearch;
    }

}
