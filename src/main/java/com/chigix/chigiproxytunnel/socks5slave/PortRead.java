/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.socks5slave;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.BufferProcessException;
import com.chigix.chigiproxytunnel.handler.BufferProcessor;
import com.chigix.chigiproxytunnel.handler.ChannelBoundProcessor;
import com.chigix.chigiproxytunnel.handler.ChannelBoundleException;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ConnectRouterException;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelModificationException;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.ProxyClose;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class PortRead extends BufferProcessor {

    public ThreadLocal<String> targetHost;

    public ThreadLocal<Integer> targetPort;

    private ChannelBoundProcessor transferer;

    private final SwitcherTable switcher;

    public PortRead(SwitcherTable switcher) {
        super(3);
        this.targetHost = new ThreadLocal<>();
        this.targetPort = new ThreadLocal<>();
        this.switcher = switcher;
    }

    public ChannelBoundProcessor getTransferer() {
        return transferer;
    }

    public void setTransferer(ChannelBoundProcessor transferer) {
        this.transferer = transferer;
    }

    @Override
    protected Processor processBuffer(Channel channel, List<Integer> buffer) throws BufferProcessException {
        if (buffer.size() < 2) {
            return this;
        }
        Application.getLogger(PortRead.class.getName()).debug(buffer);
        int port = 256 * buffer.get(0) + buffer.get(1);
        this.targetPort.set(port);
        Application.getLogger(getClass().getName()).debug(ChannelNameMap.getName(channel) + "\tTARGET: " + this.targetHost.get() + ":" + this.targetPort.get());
        final ProxyChannelExtension proxyChannel;
        try {
            proxyChannel = this.switcher.connect(this.targetHost.get(), this.targetPort.get());
        } catch (ConnectRouterException ex) {
            try {
                Socks5Helper.responseForRequest(channel, (byte) 3);
            } catch (IOException ex1) {
                channel.close();
            }
            return null;
        }
        try {
            this.transferer.registerBound(channel, proxyChannel.getChannel());
            // Cast UpdateProxyEvent to call Channel Bound in addChannelBoundleListener
            proxyChannel.setProxiedChannel(channel);
        } catch (ChannelBoundleException | ProxyChannelModificationException ex) {
            throw new BufferProcessException(ex);
        }
        final ChannelBoundProcessor selfTransfer = this.transferer;
        channel.addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof Channel.CloseEvent) {
                    Channel.CloseEvent event = (Channel.CloseEvent) e;
                    event.getChannelToClose().removeListener(this);
                    final ProxyClose closeCmd = ProxyClose.createInstance(proxyChannel);
                    closeCmd.addOnClosedListener(new Listener() {

                        @Override
                        public void performEvent(Event e) {
                            synchronized (closeCmd) {
                                closeCmd.notify();
                            }
                        }
                    });
                    while (true) {
                        CommandChannelExtension closeCommander = proxyChannel.getParentGoal().getCommandChannel();
                        if (closeCommander == null) {
                            //@TODO FIX AUTO RECONNECT.
                            Socks5SlaveServer.logInfo(event.getChannelToClose(), "Commander Has Been Shutdown.");
                            System.exit(1);
                        }
                        try {
                            closeCommander.sendCommand(closeCmd);
                            break;
                        } catch (IOException ex) {
                            //@TODO FIX AUTO RECONNECT.
                            Socks5SlaveServer.logInfo(event.getChannelToClose(), "Commander Has Been Shutdown.");
                            System.exit(1);
                        }
                    }
                    synchronized (closeCmd) {
                        try {
                            closeCmd.wait();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(PortRead.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    proxyChannel.setLocked(false);
                    selfTransfer.removeChannel(event.getChannelToClose(), proxyChannel.getChannel());
                }
            }
        });
        try {
            Socks5Helper.responseForRequest(channel, (byte) 0);
        } catch (IOException ex) {
            channel.close();
        }
        return this.transferer;
    }

}
