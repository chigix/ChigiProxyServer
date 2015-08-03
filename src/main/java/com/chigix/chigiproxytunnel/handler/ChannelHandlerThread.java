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
import com.chigix.chigiproxytunnel.channel.CloseReason;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelHandlerThread implements Runnable {

    private static final ConcurrentMap<Channel, ChannelHandlerThread> CHANNEL_THREAD;
    private static final Field fieldActiveCheck;
    private static final Field fieldInactiveCheck;

    static {
        CHANNEL_THREAD = new ConcurrentHashMap();
        try {
            fieldActiveCheck = ChannelHandler.class.getDeclaredField("__active_check");
            fieldInactiveCheck = ChannelHandler.class.getDeclaredField("__inactive_check");
            fieldActiveCheck.setAccessible(true);
            fieldInactiveCheck.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final ChannelHandler findHandlerByChannel(Channel channel) {
        return CHANNEL_THREAD.get(channel).getHandler();
    }

    private final ChannelHandler handler;
    private final Channel channel;

    public ChannelHandlerThread(ChannelHandler handler, Channel channel) {
        this.handler = handler;
        this.channel = channel;
    }

    @Override
    public void run() {
        ChannelHandlerThread thread = CHANNEL_THREAD.putIfAbsent(channel, this);
        if (thread != null) {
            throw new RuntimeException(new HandleChannelException(channel, thread.getHandler()));
        }
        channel.addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof Channel.CloseEvent) {
                    CHANNEL_THREAD.remove(((Channel.CloseEvent) e).getChannelToClose());
                }
            }
        });
        try {
            handler.channelActive(channel);
        } catch (Exception ex) {
            handler.exceptionCaught(channel, ex);
        }
        CloseReason closeReason = CloseReason.SelfClose;
        try {
            if (!fieldActiveCheck.getBoolean(handler)) {
                handler.exceptionCaught(channel, new ChannelHandlerDefinationException(handler));
                System.exit(1);
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        while (!channel.isClosedFlag()) {
            int read;
            try {
                if ((read = channel.getInputStream().read()) == -1) {
                    closeReason = CloseReason.RemoteEnd;
                    break;
                }
            } catch (IOException ex) {
                closeReason = CloseReason.RemoteClose;
                break;
            }
            try {
                handler.channelRead(channel, read);
            } catch (Exception ex) {
                handler.exceptionCaught(channel, ex);
                break;
            }
        }
        try {
            this.handler.channelInactive(channel, closeReason);
        } catch (Exception ex) {
            this.handler.exceptionCaught(channel, ex);
        }
        try {
            if (!fieldInactiveCheck.getBoolean(this.handler)) {
                this.handler.exceptionCaught(channel, new ChannelHandlerDefinationException(handler));
                System.exit(1);
            }
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException();
        }
        channel.close();
        CHANNEL_THREAD.remove(channel);
    }

    public ChannelHandler getHandler() {
        return handler;
    }

    public Channel getChannel() {
        return channel;
    }

}
