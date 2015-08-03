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
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelBoundProcessor implements Processor {

    private final ConcurrentMap<Channel, Future<Channel>> channelMaps;

    private final ConcurrentMap<Channel, Listener> channelListenerMap;

    public ChannelBoundProcessor() {
        this.channelMaps = new ConcurrentHashMap<>();
        this.channelListenerMap = new ConcurrentHashMap<>();
    }

    @Override
    public Processor update(Channel channelFrom, int input) throws ProcessorUpdateException {
        //Application.getLogger(getClass().getName()).warn((char) input);
        Future<Channel> futureTo = channelMaps.get(channelFrom);
        Channel channelTo;
        try {
            channelTo = futureTo.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        } catch (NullPointerException ex) {
            if (futureTo == null) {
                throw new ProcessorUpdateException(new ChannelBoundNotExistException(channelFrom));
            }
            throw ex;
        }
        try {
            channelTo.getOutputStream().write(input);
        } catch (IOException ex) {
            channelTo.close();
        }
        return this;
    }

    /**
     * Remove this channels' mapping atomically.
     *
     * @param from
     * @param to
     */
    public void removeChannel(Channel from, Channel to) {
        if (from == null || to == null) {
            return;
        }
        Future<Channel> futureTo = this.channelMaps.get(from);
        Future<Channel> futureFrom = this.channelMaps.get(to);
        try {
            if (futureTo != null && futureTo.get() == to) {
                this.channelMaps.remove(from, futureTo);
                //@TODO RECHECK is it necessary for the listener remove here.
                from.removeListener(this.channelListenerMap.get(from));
            }
            if (futureFrom != null && futureFrom.get() == from) {
                this.channelMaps.remove(to, futureFrom);
                //@TODO RECHECK if it necessary for the listener remove here.
                to.removeListener(this.channelListenerMap.get(to));
            }
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Add a new mapping for two channel for the future bound atomically.
     *
     * @param from
     * @param to
     * @throws ChannelBoundleException
     */
    public void registerBound(final Channel from, final Channel to) throws ChannelBoundleException {
        if (from == null || to == null) {
            return;
        }
        Future<Channel> futureFrom = this.channelMaps.get(from);
        Future<Channel> futureTo = this.channelMaps.get(to);
        if (futureFrom != null || futureTo != null) {
            try {
                if (futureFrom != null && futureFrom.get() == to && futureTo != null && futureTo.get() == from) {
                    return;
                }
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
            throw new ChannelBoundleException("CHANNEL [" + String.valueOf(from) + "-->" + String.valueOf(to) + "] HAS BEEN USED.");
        }
        final FutureTask<Listener> taskListenerGenerator = new FutureTask<>(new Callable<Listener>() {

            @Override
            public Listener call() throws Exception {
                return new Listener() {

                    @Override
                    public void performEvent(Event e) {
                        if (e instanceof Channel.CloseEvent && (from.removeListener(this) || to.removeListener(this))) {
                            Future<Channel> futureTo = channelMaps.get(from);
                            Future<Channel> futureFrom = channelMaps.get(to);
                            try {
                                if (futureTo != null && futureTo.get() == to) {
                                    channelMaps.remove(from, futureTo);
                                }
                                if (futureFrom != null && futureFrom.get() == from) {
                                    channelMaps.remove(to, futureFrom);
                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                };
            }
        });
        FutureTask<Channel> taskFrom = new FutureTask(new Callable<Channel>() {

            @Override
            public Channel call() throws Exception {
                from.removeListener(channelListenerMap.put(from, taskListenerGenerator.get()));
                from.addListener(taskListenerGenerator.get());
                return from;
            }
        });
        FutureTask<Channel> taskTo = new FutureTask(new Callable<Channel>() {

            @Override
            public Channel call() throws Exception {
                to.removeListener(channelListenerMap.put(to, taskListenerGenerator.get()));
                to.addListener(taskListenerGenerator.get());
                return to;
            }
        });
        futureFrom = this.channelMaps.putIfAbsent(from, taskTo);
        futureTo = this.channelMaps.putIfAbsent(to, taskFrom);
        if (futureFrom == null && futureTo == null) {
            taskListenerGenerator.run();
            taskFrom.run();
            taskTo.run();
        } else {
            this.channelMaps.remove(from, taskTo);
            this.channelMaps.remove(to, taskFrom);
            throw new ChannelBoundleException("CHANNEL [" + from.toString() + "-->" + to.toString() + "] HAS BEEN USED.");
        }
    }

}
