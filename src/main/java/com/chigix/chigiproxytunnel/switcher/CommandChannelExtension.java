/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

import com.alibaba.fastjson.JSON;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.switcher.command.Command;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.io.IOException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class CommandChannelExtension extends ChannelExtension {

    private final Object commandSendWriterLock;

    public CommandChannelExtension(Goal parentGoal, Channel channel, String channelName) {
        super(parentGoal, channel, channelName);
        this.commandSendWriterLock = new Object();
    }

    public void sendCommand(Command commandRequesting) throws IOException {
        synchronized (this.commandSendWriterLock) {
            this.getChannel().getOutputStream().write(JSON.toJSONString(commandRequesting).getBytes());
            this.getChannel().getOutputStream().write(10);
        }
        Command.COMMANDS.put(commandRequesting.getId(), commandRequesting);
    }

    @Override
    public void setLocked(boolean lock) {
        super.setLocked(lock); //To change body of generated methods, choose Tools | Templates.
    }

}
