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
        handler.channelActive(channel);
        while (!this.channel.isClosed()) {
            int read;
            try {
                if ((read = channel.getInputStream().read()) == -1) {
                    System.out.println(handler.getChannel().getRemoteHostAddress() + " ENDED");
                    try {
                        this.channel.flushBuffer();
                    } catch (IOException ex) {
                    }
                    break;
                }
            } catch (IOException ex) {
                System.out.println(handler.getChannel().getRemoteHostAddress() + " DISCONNECTED");
                break;
            }
            handler.channelRead(channel, read);
        }
        System.out.println("线程退出");
        this.handler.channelInactive(this.channel);
        this.channel.close();
    }

}
