package com.chigix.bio.proxy;

import com.chigix.bio.proxy.channel.Channel;
import com.chigix.bio.proxy.handler.ChannelHandler;
import com.chigix.bio.proxy.handler.ChannelHandlerThread;
import com.chigix.bio.proxy.handler.Socks5ServerChannelHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChigiProxy {

    public static void main(String[] args) {
        ExecutorService threadpool = Executors.newCachedThreadPool();
        ServerSocket bndSocket = null;
        try {
            bndSocket = new ServerSocket(8080);
        } catch (IOException ex) {
            Logger.getLogger(ChigiProxy.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (true) {
            Socket channelSocket = null;
            try {
                channelSocket = bndSocket.accept();
            } catch (IOException ex) {
                Logger.getLogger(ChigiProxy.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }
            Channel channel = new Channel(channelSocket);
            ChannelHandler handler = new Socks5ServerChannelHandler(channel);
            threadpool.execute(new ChannelHandlerThread(handler));
        }
    }
}
