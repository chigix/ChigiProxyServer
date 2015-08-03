/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.event.Event;
import com.chigix.event.Listenable;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelsManager implements Listenable {

    private final ConcurrentHashMap<String, ChannelExtension> channels_exname;

    private final ConcurrentSkipListSet<Listener> listeners;

    private ChannelsManager() {
        this.channels_exname = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentSkipListSet<>();
    }

    public static ChannelsManager create() {
        ChannelsManager manager = new ChannelsManager();
        return manager;
    }

    public void putExtendedChannel(final ChannelExtension newChannelEx) throws ChannelRegisterException {
        while (true) {
            ChannelExtension cachedChannelExtension = this.channels_exname.putIfAbsent(newChannelEx.getChannelName(), newChannelEx);
            if (cachedChannelExtension != null) {
                if (cachedChannelExtension == newChannelEx) {
                    return;
                }
                if (cachedChannelExtension.getChannel() == newChannelEx.getChannel()) {
                    if (this.channels_exname.replace(newChannelEx.getChannelName(), cachedChannelExtension, newChannelEx)) {
                        break;
                    }
                } else {
                    throw new ChannelRegisterException(newChannelEx.getChannelName() + " HAS BEEN REGISTERED.");
                }
            }
        }
        newChannelEx.getChannel().addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof Channel.CloseEvent) {
                    if (!newChannelEx.getChannel().removeListener(this)) {
                        return;
                    }
                    channels_exname.remove(newChannelEx.getChannelName(), newChannelEx);
                }
            }
        });
    }

    public boolean checkNameHooked(String name) {
        return this.channels_exname.containsKey(name);
    }

    @Override
    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public Collection<ChannelExtension> getRegisteredChannels() {
        return this.channels_exname.values();
    }

    public ChannelExtension getRegisteredChannel(String channelname) {
        return this.channels_exname.get(channelname);
    }

    public class ChannelRegisterException extends Exception {

        public ChannelRegisterException() {
        }

        public ChannelRegisterException(String message) {
            super(message);
        }

        public ChannelRegisterException(Throwable cause) {
            super(cause);
        }

        public ChannelRegisterException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
