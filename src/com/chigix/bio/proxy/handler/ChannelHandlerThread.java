package com.chigix.bio.proxy.handler;

import com.chigix.bio.proxy.channel.Channel;
import java.io.IOException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelHandlerThread implements Runnable {

    private final Channel channel;
    private final ChannelHandler handler;

    public ChannelHandlerThread(ChannelHandler handler) {
        this.channel = handler.getChannel();
        this.handler = handler;
    }

    @Override
    public void run() {
        new Thread() {
            @Override
            public void run() {
                handler.channelActive(channel);
            }
        }.start();
        while (!this.channel.isClosed()) {
            int read = -1;
            try {
                handler.channelRead(channel, (read = channel.getInputStream().read()));
            } catch (IOException ex) {
                System.out.println(handler.getChannel().getRemoteHostAddress() + " DISCONNECTED");
                break;
            }
            if (read == -1) {
                System.out.println(handler.getChannel().getRemoteHostAddress() + " ENDED");
                try {
                    this.channel.flushBuffer();
                } catch (IOException ex) {
                }
                break;
            }
        }
        System.out.println("线程退出");
        this.handler.channelInactive(this.channel);
        this.channel.close();
    }

}
