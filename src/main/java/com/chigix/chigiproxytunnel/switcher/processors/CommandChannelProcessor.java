/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.processors;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.handler.ProcessorUpdateException;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.Command;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public abstract class CommandChannelProcessor implements Processor {

    /**
     * STATIC input buffer for each corresponding channel cross thread support.
     */
    public static final ConcurrentHashMap<Channel, List<Integer>> COMMAND_BUFFER = new ConcurrentHashMap<>();

    private final SwitcherTable switcher;

    private final ChannelsManager manager;

    private final ProcessorsPackage processorsPkg;

    public CommandChannelProcessor(SwitcherTable switcher, ProcessorsPackage pkg) {
        this.switcher = switcher;
        this.manager = switcher.getChannelsManager();
        this.processorsPkg = pkg;
    }

    public final ChannelsManager getManager() {
        return manager;
    }

    @Override
    public final Processor update(Channel channel, int input) throws ProcessorUpdateException {
        List<Integer> buf = COMMAND_BUFFER.get(channel);
        if (input != 10) {
            try {
                buf.add(input);
                return this;
            } catch (NullPointerException e) {
                if (buf == null) {
                    buf = new ArrayList<>(1024);
                    COMMAND_BUFFER.put(channel, buf);
                    buf.add(input);
                    return this;
                } else {
                    throw new RuntimeException("OTHER NULL EXCEPTION");
                }
            }
        }
        byte[] result_bytes = new byte[buf.size()];
        for (int i = 0; i < result_bytes.length; i++) {
            result_bytes[i] = buf.get(i).byteValue();
        }
        buf.clear();
        Application.getLogger(getClass().getName()).debug(new String(result_bytes));
        JSONObject json = JSON.parseObject(new String(result_bytes));
        try {
            CommandChannelExtension chn;
            if ((chn = this.getCommandChannelExtension(this.switcher.getGoal(json.getString("commanderGoalName")), channel, json.getString("commanderChannelName"))) != null) {
                return this.processCommand(chn, (Command) JSON.parseObject(result_bytes, Class.forName(json.getString("name"))));
            } else {
                Application.getLogger(getClass().getName()).fatal(ChannelNameMap.getInstance().get(channel) + "\tCommander Channel is closing for no next processor: [" + json.getString("commanderGoalName") + ":" + json.getString("commanderChannelName") + "] CHANNEL NOT EXISTS.(" + CommandChannelProcessor.class.getName() + ")");
                return null;
            }
        } catch (ClassNotFoundException ex) {
            Application.getLogger(getClass().getName()).fatal(ex.getMessage(), ex);
            return null;
        } catch (CommandProcessException ex) {
            throw new ProcessorUpdateException(ex.getCause());
        }
    }

    public final SwitcherTable getSwitcher() {
        return switcher;
    }

    public ProcessorsPackage getProcessorsPkg() {
        return processorsPkg;
    }

    protected abstract CommandChannelExtension getCommandChannelExtension(Goal goal, Channel channel, String channelName);

    protected abstract Processor processCommand(CommandChannelExtension channel, Command command) throws CommandProcessException;

    public static class CommandProcessException extends Exception {

        public CommandProcessException() {
        }

        public CommandProcessException(String message) {
            super(message);
        }

        public CommandProcessException(Throwable cause) {
            super(cause);
        }

        public CommandProcessException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
