/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelExtension {

    private final Goal parentGoal;

    private final Channel channel;

    private final String channelName;

    private final AtomicBoolean locked;

    public ChannelExtension(Goal parentGoal, Channel channel, String channelName) {
        this.parentGoal = parentGoal;
        this.channel = channel;
        this.channelName = channelName;
        this.locked = new AtomicBoolean(false);
    }

    public Channel getChannel() {
        return channel;
    }

    public String getChannelName() {
        return channelName;
    }

    public Goal getParentGoal() {
        return parentGoal;
    }

    public boolean isLocked() {
        return this.locked.get();
    }

    public void setLocked(boolean lock) {
        while (!this.locked.compareAndSet(this.isLocked(), lock)) {
        }
        if (lock) {
            Application.getLogger(getClass().getName()).debug(ChannelNameMap.getName(this.getChannel()) + " LOCK ON");
        } else {
            Application.getLogger(getClass().getName()).debug(ChannelNameMap.getName(this.getChannel()) + " LOCK OFF");
        }
    }

}
