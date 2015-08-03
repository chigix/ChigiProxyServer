/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.processors;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.Asyncable;
import com.chigix.chigiproxytunnel.switcher.command.Command;
import com.chigix.chigiproxytunnel.switcher.command.CommandResponse;
import com.chigix.chigiproxytunnel.switcher.command.Dispatchable;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class CommandDispatcher extends CommandChannelProcessor {

    public CommandDispatcher(SwitcherTable switcher, ProcessorsPackage pkg) {
        super(switcher, pkg);
        this.asyncCommandPool = Executors.newCachedThreadPool(new AsyncCommandThreadFactory());
    }

    private final ExecutorService asyncCommandPool;

    protected void asyncProcessCommand(AsyncCommandThread t) {
        this.asyncCommandPool.execute(t);
    }

    @Override
    protected Processor processCommand(CommandChannelExtension channel, Command command) throws CommandProcessException {
        Goal g = getSwitcher().getGoal(command.getCommanderGoalName());
        if (g == null) {
            try {
                channel.sendCommand(CommandResponse.createInstance(400, "[" + channel.getParentGoal().getName() + ":" + channel.getChannelName() + "] GOAL [" + command.getCommanderGoalName() + "] NOT CONFIGURED", null, command.getId(), channel));
            } catch (IOException ex) {
                Application.getLogger(getClass().getName()).debug(ex.getMessage(), ex);
                channel.getChannel().close();
            }
            return null;
        }
        if (command instanceof CommandResponse) {
            CommandResponse resp = (CommandResponse) command;
            Application.getLogger(getClass().getName()).debug(channel.getChannel() + "\tResponse Received.");
            if (Command.COMMANDS.containsKey(resp.getId())) {
                Application.getLogger(getClass().getName()).debug(ChannelNameMap.getName(channel.getChannel()) + "\tCOMMAND RESPONSE {" + resp.getMessage() + "} FOUND.");
                Dispatchable.ProcessorsArg arg = new Dispatchable.ProcessorsArg();
                arg.setPkg(getProcessorsPkg());
                arg.setRequest(this);
                arg.setResponse(this);
                Command.COMMANDS.get(resp.getId()).processResponse(resp, arg);
                Command.COMMANDS.remove(resp.getId());
                return arg.getResponse();
            } else {
                Application.getLogger(getClass().getName()).fatal(ChannelNameMap.getName(channel.getChannel()) + "\t[" + resp.getCommanderGoalName() + ":" + resp.getCommanderChannelName()
                        + "] NON-COMMAND RESPONSE OCCURED:[" + resp.getId() + ":" + resp.getMessage() + "]");
                return this;
            }
        } else if (command instanceof Dispatchable) {
            Dispatchable cmdToDispatch = (Dispatchable) command;
            Dispatchable.ProcessorsArg arg = new Dispatchable.ProcessorsArg();
            arg.setPkg(getProcessorsPkg());
            arg.setRequest(this);
            arg.setResponse(this);
            if (command instanceof Asyncable) {
                cmdToDispatch.processRequest(channel, getSwitcher(), arg);
                return this;
            } else {
                cmdToDispatch.processRequest(channel, getSwitcher(), arg);
                return arg.getResponse();
            }
        }
        return this;
    }

    @Override
    protected CommandChannelExtension getCommandChannelExtension(Goal goal, Channel channel, String channelName) {
        if (this.getManager().checkNameHooked(channelName)) {
            return (CommandChannelExtension) this.getManager().getRegisteredChannel(channelName);
        } else {
            return null;
        }
    }

    private class AsyncCommandThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final AtomicInteger commandCount = new AtomicInteger(1);

        public AsyncCommandThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, "CommandDispatcher-AsyncCommand-" + this.commandCount.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }

    }

    public abstract class AsyncCommandThread implements Runnable {

        private final Command command;

        public AsyncCommandThread(Command command) {
            this.command = command;
        }

        public Command getCommand() {
            return command;
        }

        @Override
        public abstract void run();

    }
}
