/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.handler;

import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.channel.CloseReason;
import com.chigix.chigiproxytunnel.handler.ChannelBoundHandler;
import com.chigix.chigiproxytunnel.handler.ChannelBoundNotExistException;
import com.chigix.chigiproxytunnel.handler.ChannelBoundProcessor;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.handler.ProcessorHandler;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ConnectRouterException;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelModificationException;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.Command;
import com.chigix.chigiproxytunnel.switcher.command.CommandResponse;
import com.chigix.chigiproxytunnel.switcher.command.ProxyClose;
import com.chigix.chigiproxytunnel.switcher.command.ProxyConnect;
import com.chigix.chigiproxytunnel.switcher.processors.Authenticator;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.chigiproxytunnel.switcher.processors.CommandDispatcher;
import com.chigix.chigiproxytunnel.switcher.processors.ProcessorsPackage;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class MasterCommanderHandler extends ProcessorHandler {

    private final SwitcherTable switcher;

    private static final ConcurrentMap<Channel, ProxyChannelExtension> proxyChannelExtensionIndex = new ConcurrentHashMap<>();

    public MasterCommanderHandler(SwitcherTable switcher) {
        super(getInitProcessor(switcher));
        this.switcher = switcher;
    }

    @Override
    public String getHandlerName() {
        return "MasterCommanderHandler";
    }

    private static Processor getInitProcessor(final SwitcherTable switcher) {
        final ProcessorsPackage pkg = new ProcessorsPackage();
        Authenticator auth = new Authenticator(switcher, pkg);
        CommandDispatcher dispatcher = new CommandDispatcher(switcher, pkg) {

            @Override
            protected Processor processCommand(final CommandChannelExtension channel, Command command) throws CommandChannelProcessor.CommandProcessException {
                if (command instanceof ProxyConnect) {
                    this.asyncProcessCommand(new AsyncCommandThread(command) {

                        @Override
                        public void run() {
                            ProxyConnect connectCmd = (ProxyConnect) this.getCommand();
                            ProxyChannelExtension proxyChannelFrom = (ProxyChannelExtension) switcher.getChannelsManager().getRegisteredChannel(connectCmd.getProxyChannelName());
                            ProxyChannelExtension proxyChannelTo;
                            try {
                                proxyChannelTo = switcher.connect(connectCmd.getTargetHost(), connectCmd.getTargetPort());
                            } catch (ConnectRouterException ex) {
                                Logger.getLogger(MasterCommanderHandler.class.getName()).log(Level.SEVERE, null, ex);
                                return;
                            }
                            proxyChannelFrom.setLocked(true);
                            proxyChannelTo.setLocked(true);
                            proxyChannelExtensionIndex.putIfAbsent(proxyChannelFrom.getChannel(), proxyChannelFrom);
                            proxyChannelExtensionIndex.putIfAbsent(proxyChannelTo.getChannel(), proxyChannelTo);
                            try {
                                proxyChannelFrom.setProxiedChannel(proxyChannelTo.getChannel());
                                proxyChannelTo.setProxiedChannel(proxyChannelFrom.getChannel());
                            } catch (ProxyChannelModificationException ex) {
                                throw new RuntimeException(ex);
                            }
                            try {
                                channel.sendCommand(CommandResponse.createInstance(200, ChannelNameMap.getName(proxyChannelFrom.getChannel()) + " PROXY LAUNCHED <--> " + "[" + proxyChannelTo.getChannelName() + "]", proxyChannelTo.getChannelName(), connectCmd.getId(), channel));
                                Application.getLogger(MasterCommanderHandler.class.getName()).info(ChannelNameMap.getName(proxyChannelFrom.getChannel()) + "\tPROXY LAUNCHED <--> " + "[" + proxyChannelTo.getChannelName() + "]");
                                Application.getLogger(MasterCommanderHandler.class.getName()).debug(ChannelNameMap.getName(channel.getChannel()) + "] ProxyConnect Response Sent: PROXY TARGET:" + connectCmd.getTargetHost() + ":" + connectCmd.getTargetPort());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            Listener l = new Listener() {

                                @Override
                                public void performEvent(Event e) {
                                    if (e instanceof ProxyChannelExtension.UnlockEvent) {
                                        ProxyChannelExtension.UnlockEvent event = (ProxyChannelExtension.UnlockEvent) e;
                                        event.getChannelToUnlock().removeListener(this);
                                        proxyChannelExtensionIndex.remove(event.getChannelToUnlock().getChannel(), event.getChannelToUnlock());
                                    }
                                }
                            };
                            proxyChannelFrom.addListener(l);
                            proxyChannelTo.addListener(l);
                        }
                    });
                    return this;
                } else if (command instanceof ProxyClose) {
                    this.asyncProcessCommand(new AsyncCommandThread(command) {

                        @Override
                        public void run() {
                            ProxyClose currentCloseCmd = (ProxyClose) getCommand();
                            final ProxyChannelExtension proxyChannelFrom = (ProxyChannelExtension) getSwitcher().getChannelsManager().getRegisteredChannel(currentCloseCmd.getProxyChannelName());
                            final Channel proxiedChannel = proxyChannelFrom.getProxiedChannel();
                            if (proxiedChannel == null) {
                                try {
                                    channel.sendCommand(CommandResponse.createInstance(200, proxyChannelFrom.getChannelName() + " BOUNDLE REMOVED.", null, getCommand().getId(), channel));
                                } catch (IOException ex) {
                                    channel.getChannel().close();
                                }
                                return;
                            }
                            final ProxyChannelExtension proxyChannelTo = proxyChannelExtensionIndex.get(proxyChannelFrom.getProxiedChannel());
                            final ProxyClose nextCloseCmd = ProxyClose.createInstance(proxyChannelTo);
                            nextCloseCmd.addOnClosedListener(new Listener() {

                                @Override
                                public void performEvent(Event e) {
                                    synchronized (nextCloseCmd) {
                                        nextCloseCmd.notify();
                                    }
                                }
                            });
                            CommandChannelExtension requestCommander;
                            while (true) {
                                requestCommander = proxyChannelTo.getParentGoal().getCommandChannel();
                                if (requestCommander == null) {
                                    //@TODO FIX this rude exception.
                                    throw new RuntimeException(ChannelNameMap.getInstance().get(proxyChannelTo.getChannel()) + " Commander has been shut down.");
                                }
                                try {
                                    requestCommander.sendCommand(nextCloseCmd);
                                    break;
                                } catch (IOException ex) {
                                    //@TODO ADD commander RECONNECT SUPPORT.
                                    Logger.getLogger(MasterCommanderHandler.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            synchronized (nextCloseCmd) {
                                try {
                                    nextCloseCmd.wait();
                                } catch (InterruptedException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                            proxyChannelTo.setLocked(false);
                            CommandChannelExtension responseCommander = channel;
                            try {
                                responseCommander.sendCommand(CommandResponse.createInstance(200, proxyChannelFrom.getChannelName() + " BOUNDLE REMOVED.", null, getCommand().getId(), responseCommander));
                            } catch (IOException ex) {
                                //@TODO ADD commander RECONNECT SUPPORT.
                                throw new RuntimeException(ex);
                            }
                            proxyChannelFrom.setLocked(false);
                        }
                    });
                    return this;
                }
                return super.processCommand(channel, command);
            }

        };
        ChannelBoundProcessor proxy = new ChannelBoundProcessor();
        pkg.offerProcessor(auth);
        pkg.offerProcessor(dispatcher);
        pkg.offerProcessor(proxy);
        return auth;
    }

    public SwitcherTable getSwitcher() {
        return switcher;
    }

    @Override
    public void channelActive(Channel channel) throws Exception {
        super.channelActive(channel);
        ChannelNameMap.getInstance().putIfAbsent(channel, "[@" + Integer.toHexString(channel.hashCode()) + "/" + channel.getRemoteHostAddress() + "] CONNECT IN");
        Application.getLogger(MasterCommanderHandler.class.getName()).debug(channel.getRemoteHostAddress() + " CONNECT IN.");
    }

    @Override
    public void channelInactive(Channel channel, CloseReason reason) throws Exception {
        super.channelInactive(channel, reason);
        proxyChannelExtensionIndex.remove(channel);
        Application.getLogger(MasterCommanderHandler.class.getName()).info(ChannelNameMap.getInstance().get(channel) + "CHANNEL INACTIVED.");
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable cause) {
        if (cause instanceof ChannelBoundNotExistException) {
            ChannelBoundNotExistException notExist = (ChannelBoundNotExistException) cause;
            Channel channelFrom = notExist.getChannelSearch();
            Application.getLogger(ChannelBoundHandler.class.getName()).info(ChannelNameMap.getName(channelFrom) + "\tBound REMOVED BUT RECEIVED INPUT." + "{" + MasterCommanderHandler.class.getName() + "}");
            return;
        }
        Application.getLogger(ChannelNameMap.getInstance().get(channel)).fatal(cause.getMessage(), cause);
    }

}
