/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.goal;

import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import java.io.IOException;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class SelfGoal extends Goal {

    public SelfGoal(String name, SwitcherTable parentSwitcher) {
        super(name, parentSwitcher);
    }

    @Override
    public boolean hookRemote() throws GoalChannelNotRegisteredException, IOException {
        return true;
    }

    @Override
    public void createChannel(ReturnAction a) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
