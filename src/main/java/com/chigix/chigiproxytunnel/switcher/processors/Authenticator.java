/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.processors;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.Command;
import com.chigix.chigiproxytunnel.switcher.command.CommandHookChannel;
import com.chigix.chigiproxytunnel.switcher.command.CommandHookGoal;
import com.chigix.chigiproxytunnel.switcher.command.Dispatchable;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Authenticator extends CommandChannelProcessor {

    public Authenticator(SwitcherTable switcher, ProcessorsPackage pkg) {
        super(switcher, pkg);
    }

    @Override
    protected Processor processCommand(CommandChannelExtension channel, Command command) throws CommandProcessException {
        Dispatchable.ProcessorsArg processorArg = new Dispatchable.ProcessorsArg();
        processorArg.setRequest(this);
        processorArg.setResponse(null);
        processorArg.setPkg(getProcessorsPkg());
        if (command instanceof CommandHookGoal) {
            try {
                // HOOK NEW GOAL
                CommandHookGoal cmd = (CommandHookGoal) command;
                cmd.processRequest(channel, getSwitcher(), processorArg);
            } catch (ChannelsManager.ChannelRegisterException ex) {
                throw new CommandProcessException(ex);
            }
        } else if (command instanceof CommandHookChannel) {
            // HOOK NEW CHANNEL
            CommandHookChannel cmd = (CommandHookChannel) command;
            cmd.processRequest(channel, getSwitcher(), processorArg);
        }
        return processorArg.getResponse();
    }

    @Override
    protected CommandChannelExtension getCommandChannelExtension(Goal goal, Channel channel, String channelName) {
        return new CommandChannelExtension(goal, channel, channelName);
    }

}
