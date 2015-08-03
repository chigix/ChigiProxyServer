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
import com.chigix.chigiproxytunnel.handler.ChannelBoundProcessor;
import com.chigix.chigiproxytunnel.handler.ChannelBoundleException;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import com.chigix.event.Event;
import com.chigix.event.Listenable;
import com.chigix.event.Listener;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProxyChannelExtension extends ChannelExtension implements Listenable {

    public static final String NAME = "ProxyChannelExtension";

    private final AtomicReference<Channel> proxiedChannel;

    private final ConcurrentLinkedQueue<Listener> listeners;

    public ProxyChannelExtension(Goal parentGoal, Channel channel, String channelName) {
        super(parentGoal, channel, channelName);
        this.proxiedChannel = new AtomicReference<>();
        this.listeners = new ConcurrentLinkedQueue<>();
    }

    public static final void addChannelBoundListener(ProxyChannelExtension channel, final ChannelBoundProcessor processor) {
        channel.addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof UpdateProxyEvent) {
                    UpdateProxyEvent event = (UpdateProxyEvent) e;
                    if (event.getNewChannel() == null) {
                        processor.removeChannel(event.getOrig().getChannel(), event.getOldChannel());
                    } else {
                        try {
                            processor.registerBound(event.getOrig().getChannel(), event.getNewChannel());
                        } catch (ChannelBoundleException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
            }
        });
    }

    public boolean removeListener(Listener l) {
        return this.listeners.remove(l);
    }

    public void setProxiedChannel(Channel proxiedChannel) throws ProxyChannelModificationException {
        if (!this.isLocked()) {
            throw new ProxyChannelModificationException(ChannelNameMap.getName(this.getChannel()) + " isn't locked.");
        }
        if (this.proxiedChannel.compareAndSet(null, proxiedChannel) == false) {
            throw new ProxyChannelModificationException("[" + this.getParentGoal().getName() + ":" + this.getChannelName() + "] This channel has been proxied for CHANNEL/" + this.proxiedChannel.get());
        }
        this.castEvent(new UpdateProxyEvent(this, null, proxiedChannel));
    }

    public Channel getProxiedChannel() {
        return proxiedChannel.get();
    }

    @Override
    public void setLocked(boolean lock) {
        super.setLocked(lock);
        if (lock == false) {
            Channel old = this.proxiedChannel.getAndSet(null);
            this.castEvent(new UpdateProxyEvent(this, old, null));
            this.castEvent(new UnlockEvent(this));
        } else {
            this.castEvent(new LockEvent());
        }
    }

    @Override
    public void addListener(Listener l) {
        this.listeners.offer(l);
    }

    private void castEvent(Event e) {
        Iterator<Listener> it = this.listeners.iterator();
        while (it.hasNext()) {
            Listener listener = it.next();
            listener.performEvent(e);
        }
    }

    public class UnlockEvent implements Event {

        private final ProxyChannelExtension channelToUnlock;

        @Override
        public String getName() {
            return "CHIGIX_PROXYCHANNELEXTENSION_UNLOCK_EVENT";
        }

        public UnlockEvent(ProxyChannelExtension chnEx) {
            this.channelToUnlock = chnEx;
        }

        public ProxyChannelExtension getChannelToUnlock() {
            return channelToUnlock;
        }

    }

    public class LockEvent implements Event {

        @Override
        public String getName() {
            return "CHIGIX_PROXYCHANNELEXTENSION_LOCK_EVENT";
        }

    }

    public class UpdateProxyEvent implements Event {

        private final Channel oldChannel;
        private final Channel newChannel;
        private final ProxyChannelExtension orig;

        public UpdateProxyEvent(ProxyChannelExtension orig, Channel o, Channel n) {
            this.oldChannel = o;
            this.newChannel = n;
            this.orig = orig;
        }

        @Override
        public String getName() {
            return "CHIGIX_PROXYCHANNELEXTENSION_UPDATE_PROXY_EVENT";
        }

        public Channel getNewChannel() {
            return newChannel;
        }

        public Channel getOldChannel() {
            return oldChannel;
        }

        public ProxyChannelExtension getOrig() {
            return orig;
        }

    }

}
