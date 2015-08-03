/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.switcher.command.CommandResponse;
import com.chigix.chigiproxytunnel.switcher.command.ProxyConnect;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import com.chigix.chigiproxytunnel.switcher.goal.GoalChannelNotRegisteredException;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class SwitcherTable extends HashMap<String, String> {

    private final ChannelsManager channelsManager;

    private Goal defaultGoal;

    private final Map<String, Goal> goals;

    private final Map<String, Goal> historyCache;

    private boolean isOpen;

    public SwitcherTable(ChannelsManager manager) {
        super();
        this.channelsManager = manager;
        this.historyCache = new ConcurrentHashMap<>();
        this.goals = new ConcurrentHashMap<>();
        this.defaultGoal = null;
        this.isOpen = false;
    }

    @Override
    public void clear() {
        super.clear();
        this.historyCache.clear();
        this.goals.clear();
        this.defaultGoal = null;
    }

    public void setDefaultGoal(Goal defaultGoal) {
        this.defaultGoal = defaultGoal;
    }

    public Goal getDefaultGoal() {
        return defaultGoal;
    }

    /**
     * This is a block method. It won't return until CommandHookChannel and
     * ProxyConnect Returned.
     *
     * @param host
     * @param port
     * @return
     * @throws com.chigix.chigiproxytunnel.switcher.ConnectRouterException
     */
    public ProxyChannelExtension connect(String host, int port) throws ConnectRouterException {
        //@TODO ADD ROUTER THROUGH goals switcher.
        ProxyChannelExtension proxyChannel = this.findUnlockChannelandLock(ProxyChannelExtension.class);
        final ProxyConnect connectCmd = ProxyConnect.createInstance(host, port, proxyChannel.getParentGoal().getCommandChannel(), proxyChannel);
        final CommandResponse resp = new CommandResponse();
        connectCmd.addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof ProxyConnect.ConnectFinishEvent) {
                    ProxyConnect.ConnectFinishEvent event = (ProxyConnect.ConnectFinishEvent) e;
                    synchronized (connectCmd) {
                        resp.setCode(200);
                        connectCmd.notify();
                    }
                }
            }
        });
        connectCmd.addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof ProxyConnect.ConnectFailEvent) {
                    ProxyConnect.ConnectFailEvent event = (ProxyConnect.ConnectFailEvent) e;
                    synchronized (connectCmd) {
                        resp.setCode(500);
                        connectCmd.notify();
                    }
                }
            }
        });
        try {
            proxyChannel.getParentGoal().getCommandChannel().sendCommand(connectCmd);
        } catch (IOException ex) {
            Application.getLogger(getClass().getName()).info("GOAL:[" + proxyChannel.getParentGoal().getName() + "] DISCONNECTED UNEXPECTEDLY.");
            return null;
        }
        synchronized (connectCmd) {
            try {
                connectCmd.wait();
            } catch (InterruptedException ex) {
            }
        }
        if (resp.getCode() == 200) {
            Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(proxyChannel.getChannel()) + "\tswitcher CONNECT FINISH -->" + host + ":" + port);
        } else {
            throw new ConnectRouterException(host + ":" + port + " CONNECT ERROR.");
        }
        return proxyChannel;
    }

    /**
     * Atomically return a idle channel match the specifiec class type and lock
     * it.
     *
     * @param <T>
     * @param type
     * @return
     */
    public <T> T findUnlockChannelandLock(Class<T> type) {
        ChannelExtension channelExtension = null;
        for (ChannelExtension registeredChannel : this.channelsManager.getRegisteredChannels()) {
            if (registeredChannel.getParentGoal() != this.defaultGoal) {
                continue;
            }
            synchronized (registeredChannel) {
                Application.getLogger(getClass().getName()).debug("SWITCHERTABLE FIND CHANNEL: Channel: [" + registeredChannel.getChannelName() + "], Class: " + registeredChannel.getClass().getName() + ", LOCK: " + registeredChannel.isLocked());
                if (!(type.isAssignableFrom(registeredChannel.getClass()))) {
                    continue;
                }
                if (registeredChannel.isLocked()) {
                    continue;
                }
                registeredChannel.setLocked(true);
                channelExtension = (ProxyChannelExtension) registeredChannel;
            }
            break;
        }
        while (channelExtension == null) {
            final List<ProxyChannelExtension> tmpProxy = new ArrayList<>(1);
            try {
                this.defaultGoal.createChannel(new Goal.ReturnAction() {

                    @Override
                    public void setNewChannel(ChannelExtension channel) {
                        channel.setLocked(true);
                        tmpProxy.add((ProxyChannelExtension) channel);
                    }

                    @Override
                    public void run() {
                        synchronized (tmpProxy) {
                            tmpProxy.notify();
                        }
                    }
                });
            } catch (IOException ex) {
                Application.getLogger(getClass().getName()).debug(ex.getMessage(), ex);
                return null;
            }
            synchronized (tmpProxy) {
                try {
                    tmpProxy.wait();
                } catch (InterruptedException ex) {
                }
            }
            Application.getLogger(getClass().getName()).debug(ChannelNameMap.getName(tmpProxy.get(0).getChannel()) + "\tLOCK ON #1");
            channelExtension = tmpProxy.get(0);
        }
        return (T) channelExtension;
    }

    public void addGoal(Goal g) {
        this.goals.put(g.getName(), g);
        if (this.defaultGoal == null) {
            this.defaultGoal = g;
        }
    }

    public void removeGoal(Goal g) {
        this.goals.remove(g.getName());
        Iterator<ChannelExtension> it = this.getChannelsManager().getRegisteredChannels().iterator();
        while (it.hasNext()) {
            ChannelExtension channelExtension = it.next();
            if (channelExtension.getParentGoal() == g) {
                channelExtension.getChannel().close();
            }
        }
    }

    public void open() throws GoalChannelNotRegisteredException {
        for (Goal goal : this.goals.values()) {
            goal.setHooked(false);
            try {
                goal.hookRemote();
            } catch (GoalChannelNotRegisteredException ex) {
                throw ex;
            } catch (ChannelsManager.ChannelRegisterException | IOException ex) {
                Logger.getLogger(SwitcherTable.class.getName()).log(Level.SEVERE, null, ex);
            }
            Application.getLogger(getClass().getName()).debug("GOAL [" + goal.getName() + "] HOOK REQUEST SENT.");
        }
        this.isOpen = true;
    }

    public void close() {
        this.clear();
        this.isOpen = false;
    }

    public ChannelsManager getChannelsManager() {
        return channelsManager;
    }

    public Collection<Goal> getGoals() {
        return this.goals.values();
    }

    public Goal getGoal(String goalName) {
        return this.goals.get(goalName);
    }

}
