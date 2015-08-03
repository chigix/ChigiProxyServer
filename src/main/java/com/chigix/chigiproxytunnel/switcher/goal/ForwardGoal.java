/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.goal;

import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.command.CommandHookGoal;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import java.io.IOException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ForwardGoal extends Goal {

    public ForwardGoal(String name, SwitcherTable parentSwitcher) {
        super(name, parentSwitcher);
    }

    @Override
    public boolean hookRemote() throws GoalChannelNotRegisteredException, IOException, ChannelsManager.ChannelRegisterException {
        if (this.getCommandChannel() == null) {
            throw new GoalChannelNotRegisteredException(this);
        }
        this.getCommandChannel().setLocked(true);
        CommandHookGoal c;
        c = CommandHookGoal.createInstance(this);
        this.getCommandChannel().sendCommand(c);
        return true;
    }

    @Override
    public void createChannel(ReturnAction a) throws IOException {
        throw new UnsupportedOperationException();
    }

}
