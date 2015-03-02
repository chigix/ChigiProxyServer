package com.chigix.bio.proxy;

import com.chigix.bio.proxy.channel.Channel;
import com.chigix.bio.proxy.handler.ChannelHandler;
import com.chigix.bio.proxy.handler.ChannelHandlerThread;
import com.chigix.bio.proxy.handler.http.HttpProxyServerChannelHandler;
import com.chigix.bio.proxy.handler.Socks4aServerChannelHandler;
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

    private static ExecutorService THREAD_POOL = null;

    static {
        ChigiProxy.THREAD_POOL = Executors.newCachedThreadPool();
    }

    public static ExecutorService getThreadPool() {
        return ChigiProxy.THREAD_POOL;
    }

    public static void main(String[] args) {
        new Thread() {

            @Override
            public void run() {
                ServerSocket bndSocket = null;
                try {
                    bndSocket = new ServerSocket(8080);
                } catch (IOException ex) {
                    Logger.getLogger(ChigiProxy.class.getName()).log(Level.SEVERE, "SOCKS 5 SERVER PORT ALREADY BE USED:8080", ex);
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
                    ChigiProxy.getThreadPool().execute(new ChannelHandlerThread(handler));
                }
            }

        }.start();
        new Thread() {

            @Override
            public void run() {
                ServerSocket bndSocket = null;
                try {
                    bndSocket = new ServerSocket(8081);
                } catch (IOException ex) {
                    Logger.getLogger(ChigiProxy.class.getName()).log(Level.SEVERE, "SOCKS 4a SERVER PORT ALREADY BE USED:8081", ex);
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
                    ChannelHandler handler = new Socks4aServerChannelHandler(channel);
                    ChigiProxy.getThreadPool().execute(new ChannelHandlerThread(handler));
                }
            }

        }.start();
        new Thread() {

            @Override
            public void run() {
                ServerSocket bndSocket = null;
                try {
                    bndSocket = new ServerSocket(8082);
                } catch (IOException ex) {
                    Logger.getLogger(ChigiProxy.class.getName()).log(Level.SEVERE, "HTTPPROXY SERVER PORT ALREADY BE USED:8081", ex);
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
                    ChannelHandler handler = new HttpProxyServerChannelHandler(channel);
                    ChigiProxy.getThreadPool().execute(new ChannelHandlerThread(handler));
                }
            }

        }.start();
    }
}
