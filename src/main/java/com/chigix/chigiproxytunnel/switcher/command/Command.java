/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public interface Command {

    String getName();

    String getId();

    /**
     * Returns the parent goal name of this current channel.
     *
     * @return
     */
    String getCommanderGoalName();

    /**
     * Returns the channel name of this current channel.
     *
     * @return
     */
    String getCommanderChannelName();

    void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) throws CommandChannelProcessor.CommandProcessException;

    public static final Map<String, Command> COMMANDS = new ConcurrentHashMap<>();
}
