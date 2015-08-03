/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.switcher.ChannelExtension;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.event.Event;
import com.chigix.event.EventUtil;
import com.chigix.event.Listenable;
import com.chigix.event.Listener;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProxyConnect implements Command, Dispatchable, Listenable {

    private String id;

    private String commanderChannelName;
    private String commanderGoalName;

    private String targetHost;
    private int targetPort;

    private String proxyChannelName;

    private final Set<Listener> listeners;

    public static final ProxyConnect createInstance(String host, int port, CommandChannelExtension commander, ChannelExtension proxyChannel) {
        ProxyConnect cmd = new ProxyConnect();
        cmd.setTargetHost(host);
        cmd.setTargetPort(port);
        cmd.setId(UUID.randomUUID().toString());
        cmd.setProxyChannelName(proxyChannel.getChannelName());
        cmd.setCommanderChannelName(commander.getChannelName());
        cmd.setCommanderGoalName(commander.getParentGoal().getName());
        return cmd;
    }

    public ProxyConnect() {
        this.listeners = new CopyOnWriteArraySet<>();
    }

    @Override
    public String getName() {
        return ProxyConnect.class.getName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getCommanderGoalName() {
        return commanderGoalName;
    }

    @Override
    public String getCommanderChannelName() {
        return commanderChannelName;
    }

    public String getProxyChannelName() {
        return proxyChannelName;
    }

    public void setProxyChannelName(String proxyChannelName) {
        this.proxyChannelName = proxyChannelName;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCommanderChannelName(String channelName) {
        this.commanderChannelName = channelName;
    }

    public void setCommanderGoalName(String goalName) {
        this.commanderGoalName = goalName;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    @Override
    public void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) throws CommandChannelProcessor.CommandProcessException {
        if (resp.getCode() == 200) {
            EventUtil.castEvent(new ConnectFinishEvent(this), listeners);
            Application.getLogger(getClass().getName()).debug("[" + this.commanderGoalName + ":" + this.proxyChannelName + "] PROXY ESTABLISHED: " + this.targetHost + ":" + this.targetPort + "(" + ProxyConnect.class.getName() + ")");
            return;
        }
        if (resp.getCode() >= 500) {
            EventUtil.castEvent(new ConnectFailEvent(this), listeners);
        }
        throw new CommandChannelProcessor.CommandProcessException(resp.getMessage());
    }

    @Override
    public void processRequest(CommandChannelExtension commander, SwitcherTable switcher, ProcessorsArg processor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    public class ConnectFinishEvent implements Event {

        @Override
        public String getName() {
            return "PROXYCONNECT_PROXY_CONNECTFINISHEVENT";
        }

        private final ProxyConnect connectCmd;

        public ConnectFinishEvent(ProxyConnect connectCmd) {
            this.connectCmd = connectCmd;
        }

        public ProxyConnect getConnectCmd() {
            return connectCmd;
        }

    }

    public class ConnectFailEvent implements Event {

        @Override
        public String getName() {
            return "PROXYCONNECT_PROXY_CONNECTFINISHEVENT";
        }

        private final ProxyConnect connectCmd;

        public ConnectFailEvent(ProxyConnect connectCmd) {
            this.connectCmd = connectCmd;
        }

        public ProxyConnect getConnectCmd() {
            return connectCmd;
        }

    }

}
