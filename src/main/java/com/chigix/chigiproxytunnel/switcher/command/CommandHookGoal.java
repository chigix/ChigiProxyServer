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
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import com.chigix.chigiproxytunnel.switcher.goal.ReverseGoal;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.UUID;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class CommandHookGoal implements Command {

    private String channelName;

    private String goalName;

    private String id;

    public static CommandHookGoal createInstance(final Goal goal) throws ChannelsManager.ChannelRegisterException {
        Application.getLogger(CommandHookGoal.class.getName()).info("CommandHookGoal creater: COMMAND CHANNEL:" + goal.getCommandChannel().getChannelName());
        goal.getParentSwitcher().getChannelsManager().putExtendedChannel(goal.getCommandChannel());
        ChannelNameMap.getInstance().put(goal.getCommandChannel().getChannel(), "[@" + Integer.toHexString(goal.getCommandChannel().getChannel().hashCode()) + "/" + goal.getCommandChannel().getChannel().getRemoteHostAddress() + ":" + goal.getCommandChannel().getChannelName() + "]");
        CommandHookGoal cmd = new CommandHookGoal() {

            @Override
            public void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) {
                if (resp.getId().equals(this.getId())) {
                    goal.setHooked(true);
                    goal.getCommandChannel().setLocked(true);
                    Application.getLogger(getClass().getName()).info(ChannelNameMap.getInstance().get(goal.getCommandChannel().getChannel()) + "\tGOAL HOOK RESPONSE RECEIVED.");
                }
            }

        };
        cmd.setCommanderChannelName(goal.getCommandChannel().getChannelName());
        cmd.setCommanderGoalName(goal.getName());
        cmd.setId(UUID.randomUUID().toString());
        return cmd;
    }

    @Override
    public String getName() {
        return CommandHookGoal.class.getName();
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
    public String getId() {
        return this.id;
    }

    public void setCommanderChannelName(String channelName) {
        this.channelName = channelName;
    }

    public void setCommanderGoalName(String goalName) {
        this.goalName = goalName;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) {
        // RESPONSE CODE defined in the constructor.
    }

    public void processRequest(CommandChannelExtension hookCommander, final SwitcherTable switcher, Dispatchable.ProcessorsArg arg) throws ChannelsManager.ChannelRegisterException {
        final Goal g = new ReverseGoal(getCommanderGoalName(), switcher);
        CommandChannelExtension commander = new CommandChannelExtension(g, hookCommander.getChannel(), getCommanderChannelName());
        commander.setLocked(true);
        g.setCommandChannel(commander);
        switcher.getChannelsManager().putExtendedChannel(commander);
        ChannelNameMap.recordChannel(commander);
        CommandResponse resp = CommandResponse.createInstance(200, ChannelNameMap.getInstance().get(commander.getChannel()) + " GOAL HOOKED.", null, getId(), commander);
        try {
            commander.sendCommand(resp);
        } catch (IOException ex) {
            Application.getLogger(getClass().getName()).fatal(ex.getMessage(), ex);
            g.setHooked(false);
            commander.getChannel().close();
            return;
        }
        commander.getChannel().addListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                if (e instanceof Channel.CloseEvent) {
                    Channel.CloseEvent event = (Channel.CloseEvent) e;
                    event.getChannelToClose().removeListener(this);
                    switcher.removeGoal(g);
                }
            }
        });
        switcher.addGoal(g);
        g.setHooked(true);
        arg.setResponse(arg.getPkg().getDispatcher());
        Application.getLogger(getClass().getName()).debug(ChannelNameMap.getInstance().get(commander.getChannel()) + "\tHOOKED RESP SENT.");
    }

}
