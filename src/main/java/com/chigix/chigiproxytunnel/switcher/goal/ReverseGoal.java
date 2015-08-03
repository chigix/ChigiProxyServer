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
import com.chigix.chigiproxytunnel.switcher.ChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.command.CreateChannel;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ReverseGoal extends Goal {

    public ReverseGoal(String name, SwitcherTable parentSwitcher) {
        super(name, parentSwitcher);
    }

    @Override
    public boolean hookRemote() {
        return true;
    }

    @Override
    public void createChannel(final ReturnAction a) throws IOException {
        CreateChannel createCmd = CreateChannel.createInstance(this);
        createCmd.addResponseListener(new Listener() {

            @Override
            public void performEvent(Event e) {
                CreateChannel.ResponseEvent responseEvent = (CreateChannel.ResponseEvent) e;
                ChannelExtension newChannel = getParentSwitcher().getChannelsManager().getRegisteredChannel(responseEvent.getCmd().getNewlyCreatedChannelName());
                a.setNewChannel(newChannel);
                a.run();
                Application.getLogger(getClass().getName()).info("REVERSE CHANNEL: [" + newChannel.getChannelName() + "] ESTABLISHED.");
            }
        });
        try {
            this.getCommandChannel().sendCommand(createCmd);
        } catch (IOException ex) {
            Application.getLogger(getClass().getName()).info(this.getName() + " DISCONNECTED UNEXPECTEDLY.");
            this.getCommandChannel().getChannel().close();
            this.setHooked(false);
        }
    }

}
