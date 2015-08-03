/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.goal;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.ChannelHandler;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelExtension;
import com.chigix.chigiproxytunnel.switcher.handler.SlaveCommanderHandler;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.ChannelHookNameConflictedException;
import com.chigix.chigiproxytunnel.switcher.command.CommandHookChannel;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProxyGoal extends ForwardGoal {

    private final SlaveCommanderHandler handler;

    public ProxyGoal(String name, SwitcherTable parentSwitcher) {
        super(name, parentSwitcher);
        this.handler = new SlaveCommanderHandler(parentSwitcher);
    }

    @Override
    public void createChannel(final ReturnAction a) throws IOException {
        Channel goalChn = this.getCommandChannel().getChannel();
        final ProxyChannelExtension chn;
        chn = new ProxyChannelExtension(this, new Channel(new Socket(goalChn.getRemoteHostAddress(), goalChn.getRemotePort())), ChannelNameMap.generateChannelName(this));
        ChannelNameMap.recordChannel(chn);
        ChannelHandler.handleChannel(this.handler, chn.getChannel());
        Application.getLogger(getClass().getName()).warn("[" + getName() + ":" + chn.getChannelName() + "] HANDLED in ProxyGoal [" + ProxyGoal.class.getName() + "]");
        final CommandChannelExtension hookCommander = new CommandChannelExtension(this, chn.getChannel(), chn.getChannelName());
        hookCommander.setLocked(true);
        Application.getLogger(ProxyGoal.class.getName()).debug(ChannelNameMap.getName(chn.getChannel()) + "LOCK IN ProxyGoal");
        CommandHookChannel hookCmd;
        try {
            hookCmd = CommandHookChannel.createInstance(chn, hookCommander);
        } catch (ChannelHookNameConflictedException ex) {
            Application.getLogger(getClass().getName()).fatal(ex.getMessage(), ex);
            return;
        }
        hookCmd.addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof CommandHookChannel.HookFinishEvent) {
                    a.setNewChannel(chn);
                    a.run();
                    Application.getLogger(ProxyGoal.class.getName()).info(ChannelNameMap.getInstance().get(chn.getChannel()) + "]\tProxy CHANNEL HOOKED.");
                }
            }
        });
        hookCommander.sendCommand(hookCmd);
    }

}
