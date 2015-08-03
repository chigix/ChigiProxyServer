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
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.switcher.ChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ProxyChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.event.Event;
import com.chigix.event.Listenable;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class CommandHookChannel implements Command, Listenable {

    private String id;

    private String goalName;

    private String channelName;

    private String channelType;

    /**
     * ONLY BE ALLOWED USAGE IN processResponse.
     */
    private ChannelExtension channelToHook;

    /**
     * ONLY BE ALLOWED usage in processResponse.
     */
    private CommandChannelExtension hookCommander;

    private final CopyOnWriteArrayList<Listener> listeners;

    public static CommandHookChannel createInstance(ChannelExtension channelToHook, CommandChannelExtension commander) throws ChannelHookNameConflictedException {
        if (commander.getChannelName().equals(channelToHook.getChannelName()) && commander.getChannel() == channelToHook.getChannel()) {
            if (channelToHook.getParentGoal().getParentSwitcher().getChannelsManager().checkNameHooked(channelToHook.getChannelName())) {
                throw new ChannelHookNameConflictedException(channelToHook.getChannelName() + " Has been hooked.");
            }
            commander.setLocked(true);
            channelToHook.setLocked(true);
            try {
                channelToHook.getParentGoal().getParentSwitcher().getChannelsManager().putExtendedChannel(commander);
            } catch (ChannelsManager.ChannelRegisterException ex) {
                throw new ChannelHookNameConflictedException(ex);
            }
            ChannelNameMap.getInstance().put(commander.getChannel(), "[@" + Integer.toHexString(commander.getChannel().hashCode()) + "/" + commander.getChannel().getRemoteHostAddress() + ":" + channelToHook.getChannelName() + "]");
            Application.getLogger(CommandHookGoal.class.getName()).info("Channel creater: " + channelToHook.getChannelName());
            CommandHookChannel cmd = new CommandHookChannel();
            cmd.setChannelToHook(channelToHook);
            cmd.setHookCommander(commander);
            cmd.setId(UUID.randomUUID().toString());
            return cmd;
        } else {
            throw new RuntimeException("commander take different channelname or channel object with the channel to hook.");
        }
    }

    public CommandHookChannel() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public String getName() {
        return CommandHookChannel.class.getName();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getCommanderGoalName() {
        return this.goalName;
    }

    @Override
    public String getCommanderChannelName() {
        return this.channelName;
    }

    @Override
    public void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) throws CommandChannelProcessor.CommandProcessException {
        Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(this.channelToHook.getChannel()) + "\tCHANNEL HOOK RESPONSE RECEIVED: " + resp.getId() + ", " + resp.getMessage());
        if (resp.getCode() == 200) {
            try {
                this.channelToHook.getParentGoal().getParentSwitcher().getChannelsManager().putExtendedChannel(channelToHook);
            } catch (ChannelsManager.ChannelRegisterException ex) {
                throw new CommandChannelProcessor.CommandProcessException(ex);
            }
            channelToHook.setLocked(false);
            Application.getLogger(getClass().getName()).debug(ChannelNameMap.getInstance().get(this.channelToHook.getChannel()) + " LOCK OFF in CommandHookChannel#1");
            if (this.channelToHook instanceof ProxyChannelExtension) {
                processorArg.setResponse(processorArg.getPkg().getProxyTransferer());
                ProxyChannelExtension.addChannelBoundListener((ProxyChannelExtension) this.channelToHook, processorArg.getPkg().getProxyTransferer());
            } else if (this.channelToHook instanceof CommandChannelExtension) {
                processorArg.setResponse(processorArg.getPkg().getDispatcher());
            }
            try {
                this.castEvent(new HookFinishEvent(channelToHook, hookCommander));
            } catch (Exception ex) {
                throw new CommandChannelProcessor.CommandProcessException(ex);
            }
            return;
        }
        this.channelToHook.getChannel().close();
        //e.setEventName("CLOSE_CHANNEL");
        //for (Listener listener : listeners) {
        //    listener.performEvent(e);
        //}
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCommanderGoalName(String goalName) {
        this.goalName = goalName;
    }

    public void setCommanderChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public void processRequest(final CommandChannelExtension hookCommander, final SwitcherTable switcher, Dispatchable.ProcessorsArg arg) {
        if (hookCommander.getParentGoal() == null) {
            try {
                hookCommander.sendCommand(CommandResponse.createInstance(500, "[" + hookCommander.getParentGoal().getName() + ":" + hookCommander.getChannelName() + "] GOAL NOT HOOKED.", null, getId(), hookCommander));
                Application.getLogger(getClass().getName()).info("[" + hookCommander.getParentGoal().getName() + ":" + hookCommander.getChannelName() + "] Invalid Channel Hook on unknown goal.");
            } catch (IOException ex) {
            }
            hookCommander.getChannel().close();
            return;
        }
        if (switcher.getChannelsManager().checkNameHooked(hookCommander.getChannelName())) {
            try {
                hookCommander.sendCommand(CommandResponse.createInstance(500, "[" + hookCommander.getParentGoal().getName() + ":" + hookCommander.getChannelName() + "] Has been hooked, this hook request is denied.", null, getId(), hookCommander));
                Application.getLogger(getClass().getName()).fatal("[" + hookCommander.getParentGoal().getName() + ":" + hookCommander.getChannelName() + "] Has been hooked, this hook request is denied.");
            } catch (IOException ex) {
            }
            hookCommander.getChannel().close();
            return;
        }
        ChannelNameMap.getInstance().put(hookCommander.getChannel(), "[@" + Integer.toHexString(hookCommander.getChannel().hashCode()) + "/" + hookCommander.getChannel().getRemoteHostAddress() + ":" + getCommanderChannelName() + "]");
        hookCommander.getChannel().addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof Channel.CloseEvent && hookCommander.getChannel().removeListener(this)) {
                    Application.getLogger(getClass().getName()).info("[" + hookCommander.getParentGoal().getName() + ":" + hookCommander.getChannelName() + "] DISCONNECTED.");
                }
            }
        });
        ChannelExtension channelToHook;
        if (getChannelType().equals(ProxyChannelExtension.class.getName())) {
            channelToHook = new ProxyChannelExtension(hookCommander.getParentGoal(), hookCommander.getChannel(), hookCommander.getChannelName());
            ProxyChannelExtension.addChannelBoundListener((ProxyChannelExtension) channelToHook, arg.getPkg().getProxyTransferer());
            arg.setResponse(arg.getPkg().getProxyTransferer());
        } else if (getChannelType().equals(CommandChannelExtension.class.getName())) {
            channelToHook = new CommandChannelExtension(hookCommander.getParentGoal(), hookCommander.getChannel(), hookCommander.getChannelName());
            arg.setResponse(arg.getPkg().getDispatcher());
        } else {
            try {
                hookCommander.sendCommand(CommandResponse.createInstance(500, "[" + hookCommander.getParentGoal().getName() + ":" + hookCommander.getChannelName() + "] Unknown Channel Type for the channel to hook.", null, getId(), hookCommander));
                Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(hookCommander.getChannel()) + "\tUnknown Channel Type for the channel to hook.");
            } catch (IOException ex) {
            }
            hookCommander.getChannel().close();
            return;
        }
        channelToHook.setLocked(true);
        Application.getLogger(getClass().getName()).debug("CHANNELMANAGER VIEW#1: " + switcher.getChannelsManager().getRegisteredChannels());
        try {
            switcher.getChannelsManager().putExtendedChannel(channelToHook);
        } catch (ChannelsManager.ChannelRegisterException ex) {
            throw new RuntimeException(ex);
        }
        Application.getLogger(getClass().getName()).debug("CHANNELMANAGER VIEW#2: " + switcher.getChannelsManager().getRegisteredChannels());
        try {
            hookCommander.sendCommand(CommandResponse.createInstance(200, ChannelNameMap.getInstance().get(hookCommander.getChannel()) + " Hooked Successfully for Channel.", null, getId(), hookCommander));
        } catch (IOException ex) {
            Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(hookCommander.getChannel()) + "\tChannel Hook Response Sent Failed.");
            hookCommander.getChannel().close();
            return;
        }
        Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(hookCommander.getChannel()) + "\tHook Response SENT : " + getId() + ", Hooked Successfully for Channel.");
        channelToHook.setLocked(false);
    }

    public void setChannelToHook(ChannelExtension channelToHook) {
        this.channelToHook = channelToHook;
        this.channelName = channelToHook.getChannelName();
        this.goalName = channelToHook.getParentGoal().getName();
        this.channelType = channelToHook.getClass().getName();
    }

    public void setHookCommander(CommandChannelExtension hookCommander) {
        this.hookCommander = hookCommander;
    }

    @Override
    public void addListener(Listener l) {
        this.listeners.add(l);
    }

    private void castEvent(Event e) throws Exception {
        for (Listener listener : listeners) {
            listener.performEvent(e);
        }
    }

    public class HookFinishEvent implements Event {

        private final ChannelExtension channelToHook;

        private final CommandChannelExtension hookCommander;

        public HookFinishEvent(ChannelExtension channelToHook, CommandChannelExtension hookCommander) {
            this.channelToHook = channelToHook;
            this.hookCommander = hookCommander;
        }

        @Override
        public String getName() {
            return "CHIGIX_COMMANDHOOKCHANNEL_HOOK_FINISH";
        }

        public ChannelExtension getChannelToHook() {
            return channelToHook;
        }

        public CommandChannelExtension getHookCommander() {
            return hookCommander;
        }

    }

}
