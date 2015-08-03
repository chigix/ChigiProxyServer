/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.event.DefaultEvent;
import com.chigix.event.EventUtil;
import com.chigix.event.Listener;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProxyClose implements Command, Dispatchable, Asyncable {

    private final CopyOnWriteArrayList<Listener> onClosedListeners;

    public static final ProxyClose createInstance(ProxyChannelExtension proxyChannel) {
        ProxyClose closeCmd = new ProxyClose();
        closeCmd.setId(UUID.randomUUID().toString());
        closeCmd.setProxyChannel(proxyChannel);
        return closeCmd;
    }

    public ProxyClose() {
        this.onClosedListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public String getName() {
        return ProxyClose.class.getName();
    }

    private ProxyChannelExtension proxyChannel;

    private String uuid;

    @Override
    public String getId() {
        return this.uuid;
    }

    public void setId(String uuid) {
        this.uuid = uuid;
    }

    private String goalName;

    @Override
    public String getCommanderGoalName() {
        return this.goalName;
    }

    public void setCommanderGoalName(String goalName) {
        this.goalName = goalName;
    }

    private String channelName;

    @Override
    public String getCommanderChannelName() {
        return this.channelName;
    }

    public void setCommanderChannelName(String channelName) {
        this.channelName = channelName;
    }

    private String proxyChannelName;

    public String getProxyChannelName() {
        return proxyChannelName;
    }

    public void setProxyChannelName(String proxyChannelName) {
        this.proxyChannelName = proxyChannelName;
    }

    public void setProxyChannel(ProxyChannelExtension proxyChannel) {
        this.proxyChannel = proxyChannel;
        this.proxyChannelName = proxyChannel.getChannelName();
        this.setCommanderChannelName(proxyChannel.getParentGoal().getCommandChannel().getChannelName());
        this.setCommanderGoalName(proxyChannel.getParentGoal().getName());
    }

    @Override
    public void processResponse(CommandResponse resp, ProcessorsArg processorArg) {
        EventUtil.castEvent(new DefaultEvent(), onClosedListeners);
    }

    @Override
    public void processRequest(CommandChannelExtension commander, SwitcherTable switcher, ProcessorsArg processor) throws CommandChannelProcessor.CommandProcessException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addOnClosedListener(Listener l) {
        this.onClosedListeners.add(l);
    }

}
