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
import com.chigix.chigiproxytunnel.handler.ChannelBoundleException;
import com.chigix.chigiproxytunnel.handler.ChannelHandler;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.handler.ProcessorHandler;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelModificationException;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.Command;
import com.chigix.chigiproxytunnel.switcher.command.CommandResponse;
import com.chigix.chigiproxytunnel.switcher.command.ProxyClose;
import com.chigix.chigiproxytunnel.switcher.command.ProxyConnect;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.chigiproxytunnel.switcher.processors.CommandDispatcher;
import com.chigix.chigiproxytunnel.switcher.processors.ProcessorsPackage;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class SlaveCommanderHandler extends ProcessorHandler {

    private final SwitcherTable switcher;

    public SlaveCommanderHandler(SwitcherTable switcher) {
        super(getInitProcessor(switcher));
        this.switcher = switcher;
    }

    @Override
    public String getHandlerName() {
        return "SlaveCommanderHandler";
    }

    private static Processor getInitProcessor(SwitcherTable switcher) {
        // Here is not singleton, because only one handler object to be used through multiple thread.
        final ProcessorsPackage pkg = new ProcessorsPackage();
        final ChannelBoundProcessor channelBounder = new ChannelBoundProcessor();
        final ChannelBoundHandler channelBoundleHandler = new ChannelBoundHandler() {

            @Override
            public void channelActive(Channel channel) throws Exception {
                super.channelActive(channel);
                Application.getLogger(SlaveCommanderHandler.class.getName()).info(ChannelNameMap.getName(channel) + "\tBoundler Handled.");
            }

            @Override
            public void exceptionCaught(Channel channel, Throwable cause) {
                if (cause instanceof ChannelBoundNotExistException) {
                    ChannelBoundNotExistException boundNull = (ChannelBoundNotExistException) cause;
                    Channel channelFrom = boundNull.getChannelSearch();
                    Application.getLogger(ChannelBoundHandler.class.getName()).info(ChannelNameMap.getName(channelFrom) + "\tBound REMOVED BUT RECEIVED INPUT." + "{" + SlaveCommanderHandler.class.getName() + "}");
                    return;
                }
                Application.getLogger(ChannelNameMap.getInstance().get(channel)).fatal(cause.getMessage(), cause);
            }
        };
        CommandDispatcher dispatcher;
        dispatcher = new CommandDispatcher(switcher, pkg) {

            @Override
            protected Processor processCommand(final CommandChannelExtension commander, Command command) throws CommandChannelProcessor.CommandProcessException {
                if (command instanceof ProxyConnect) {
                    this.asyncProcessCommand(new AsyncCommandThread(command) {

                        @Override
                        public void run() {
                            final ProxyConnect connectCmd = (ProxyConnect) this.getCommand();
                            Socket socket;
                            try {
                                socket = new Socket(connectCmd.getTargetHost(), connectCmd.getTargetPort());
                            } catch (IOException ex) {
                                Application.getLogger(getClass().getName()).fatal(ex.getMessage() + "{" + connectCmd.getTargetHost() + ":" + connectCmd.getTargetPort() + "}", ex);
                                try {
                                    commander.sendCommand(CommandResponse.createInstance(500, ex.getMessage(), null, connectCmd.getId(), commander));
                                } catch (IOException ex1) {
                                    commander.getChannel().close();
                                }
                                return;
                            }
                            Channel socket_channel = new Channel(socket);
                            final ProxyChannelExtension proxyChannelFrom = (ProxyChannelExtension) getSwitcher().getChannelsManager().getRegisteredChannel(connectCmd.getProxyChannelName());
                            Application.getLogger(getClass().getName()).debug(ChannelNameMap.getInstance().get(proxyChannelFrom.getChannel()) + "\tPROYX CHANNEL STATE#1: " + proxyChannelFrom.getChannelName() + "LOCK:" + proxyChannelFrom.isLocked());
                            proxyChannelFrom.setLocked(true);
                            socket_channel.addListener(new Listener() {

                                @Override
                                public void performEvent(Event e) {
                                    if (e instanceof Channel.CloseEvent) {
                                        Channel.CloseEvent closeEvent = (Channel.CloseEvent) e;
                                        try {
                                            commander.sendCommand(CommandResponse.createInstance(200, ChannelNameMap.getName(proxyChannelFrom.getChannel()) + " PROXY LAUNCHED <--> " + connectCmd.getTargetHost() + ":" + connectCmd.getTargetPort(), null, connectCmd.getId(), commander));
                                        } catch (IOException ex) {
                                            Application.getLogger(SlaveCommanderHandler.class.getName()).fatal(ex.getMessage(), ex);
                                        }
                                        proxyChannelFrom.setLocked(false);
                                    }
                                }
                            });
                            try {
                                channelBoundleHandler.registerBound(socket_channel, proxyChannelFrom.getChannel());
                                proxyChannelFrom.setProxiedChannel(socket_channel);
                            } catch (ChannelBoundleException | ProxyChannelModificationException ex) {
                                try {
                                    commander.sendCommand(CommandResponse.createInstance(500, ex.getMessage(), null, connectCmd.getId(), commander));
                                } catch (IOException ex1) {
                                    throw new RuntimeException(ex1);
                                }
                                Application.getLogger(getClass().getName()).fatal(ex.getMessage(), ex);
                                return;
                            }
                            ChannelHandler.handleChannel(channelBoundleHandler, socket_channel);
                            Application.getLogger(getClass().getName()).debug(ChannelNameMap.getInstance().get(proxyChannelFrom.getChannel()) + "\tPROYX CHANNEL STATE#2: " + proxyChannelFrom.getChannelName() + "LOCK:" + proxyChannelFrom.isLocked());
                            try {
                                commander.sendCommand(CommandResponse.createInstance(200, ChannelNameMap.getName(proxyChannelFrom.getChannel()) + " PROXY LAUNCHED <--> " + connectCmd.getTargetHost() + ":" + connectCmd.getTargetPort(), null, connectCmd.getId(), commander));
                                Application.getLogger(getClass().getName()).info(ChannelNameMap.getInstance().get(proxyChannelFrom.getChannel()) + "\tPROXY LAUNCHED <--> " + ChannelNameMap.getName(socket_channel) + "/" + connectCmd.getTargetHost() + ":" + connectCmd.getTargetPort());
                            } catch (IOException ex) {
                                Application.getLogger(getClass().getName()).fatal(ex.getMessage(), ex);
                            }
                        }
                    });
                    return this;
                } else if (command instanceof ProxyClose) {
                    this.asyncProcessCommand(new AsyncCommandThread(command) {

                        @Override
                        public void run() {
                            ProxyClose closeCmd = (ProxyClose) getCommand();
                            ProxyChannelExtension proxyChannelFrom = (ProxyChannelExtension) getSwitcher().getChannelsManager().getRegisteredChannel(closeCmd.getProxyChannelName());
                            final Channel channelTo = proxyChannelFrom.getProxiedChannel();
                            if (channelTo == null) {
                                try {
                                    commander.sendCommand(CommandResponse.createInstance(200, proxyChannelFrom.getChannelName() + " BOUNDLE REMOVED.", null, getCommand().getId(), commander));
                                } catch (IOException ex) {
                                    Application.getLogger(SlaveCommanderHandler.class.getName()).fatal(ex.getMessage(), ex);
                                }
                                return;
                            }
                            CommandChannelExtension responseCommander = commander;
                            try {
                                responseCommander.sendCommand(CommandResponse.createInstance(200, proxyChannelFrom.getChannelName() + " BOUNDLE REMOVED.", null, closeCmd.getId(), responseCommander));
                            } catch (IOException ex) {
                                Logger.getLogger(SlaveCommanderHandler.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            proxyChannelFrom.setLocked(false);
                            channelTo.close();
                        }
                    });
                    return this;
                }
                return super.processCommand(commander, command);
            }

        };
        pkg.offerProcessor(dispatcher);
        pkg.offerProcessor(channelBounder);
        return dispatcher;
    }

    @Override
    public void channelActive(Channel channel) throws Exception {
        super.channelActive(channel);
        Application.getLogger(SlaveCommanderHandler.class.getName()).info(ChannelNameMap.getName(channel) + " CHANNEL ACTIVE.");
    }

    @Override
    public void channelInactive(Channel channel, CloseReason reason) throws Exception {
        super.channelInactive(channel, reason);
        switch (reason) {
            case RemoteClose:
                Application.getLogger(SlaveCommanderHandler.class.getName()).info(ChannelNameMap.getName(channel) + " CLOSED by master.");
                break;
            case RemoteEnd:
                Application.getLogger(SlaveCommanderHandler.class.getName()).info(ChannelNameMap.getName(channel) + " MASTER END.");
                break;
            case SelfClose:
                Application.getLogger(SlaveCommanderHandler.class.getName()).info(ChannelNameMap.getName(channel) + " CLOSED selfly.");
                break;
        }
    }

    public SwitcherTable getSwitcher() {
        return this.switcher;
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable cause) {
        Application.getLogger(SlaveCommanderHandler.class.getName()).fatal(cause.getMessage(), cause);
    }

}
