/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.goal;

import com.chigix.chigiproxytunnel.switcher.ChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ChannelsManager;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
abstract public class Goal {

    private final String name;
    private CommandChannelExtension commandChannel;

    private final AtomicBoolean hooked;

    private final SwitcherTable parentSwitcher;

    public Goal(String name, SwitcherTable parentSwitcher) {
        this.name = name;
        this.commandChannel = null;
        this.hooked = new AtomicBoolean(false);
        this.parentSwitcher = parentSwitcher;
    }

    public void setCommandChannel(CommandChannelExtension commandChannel) {
        this.commandChannel = commandChannel;
    }

    public String getName() {
        return name;
    }

    public CommandChannelExtension getCommandChannel() {
        return commandChannel;
    }

    /**
     * Create a new Channel, which is return through call back action.
     *
     * @param a
     * @throws IOException
     */
    abstract public void createChannel(ReturnAction a) throws IOException;

    /**
     *
     * @return the result of this hook try.
     * @throws GoalChannelNotRegisteredException
     * @throws com.chigix.chigiproxytunnel.switcher.ChannelsManager.ChannelRegisterException
     * @throws IOException
     */
    abstract public boolean hookRemote() throws GoalChannelNotRegisteredException, ChannelsManager.ChannelRegisterException, IOException;

    public boolean isHooked() {
        return hooked.get();
    }

    public void setHooked(boolean hooked) {
        while (!this.hooked.compareAndSet(this.isHooked(), hooked)) {
        }
    }

    public SwitcherTable getParentSwitcher() {
        return parentSwitcher;
    }

    public interface ReturnAction {

        void setNewChannel(ChannelExtension channel);

        void run();
    }

}
