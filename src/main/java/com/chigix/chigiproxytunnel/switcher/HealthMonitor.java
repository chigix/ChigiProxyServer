/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class HealthMonitor implements Runnable {

    private final SwitcherTable switcher;

    private final ChannelsManager manager;

    public HealthMonitor(SwitcherTable switcher) {
        this.switcher = switcher;
        this.manager = switcher.getChannelsManager();
    }

    @Override
    public void run() {
        Map<Goal, Integer> waitingChannelsCount = new HashMap<Goal, Integer>() {

            @Override
            public String toString() {
                Iterator<Entry<Goal, Integer>> i = this.entrySet().iterator();
                if (!i.hasNext()) {
                    return "{}";
                }

                StringBuilder sb = new StringBuilder();
                sb.append('{');
                for (;;) {
                    Entry<Goal, Integer> e = i.next();
                    Goal key = e.getKey();
                    Integer value = e.getValue();
                    if (key == switcher.getDefaultGoal()) {
                        sb.append("* ");
                        sb.append(key.getName());
                    } else {
                        sb.append(key.getName());
                    }
                    sb.append('=');
                    sb.append(value);
                    if (!i.hasNext()) {
                        return sb.append('}').toString();
                    }
                    sb.append(',').append(' ');
                }
            }

        };
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            waitingChannelsCount.clear();
            for (Goal goal : this.switcher.getGoals()) {
                if (goal.isHooked()) {
                    waitingChannelsCount.put(goal, 0);
                }
            }
            for (ChannelExtension registeredChannel : this.manager.getRegisteredChannels()) {
                if (registeredChannel.getParentGoal() != null && registeredChannel.isLocked() == false) {
                    try {
                        waitingChannelsCount.put(registeredChannel.getParentGoal(), waitingChannelsCount.get(registeredChannel.getParentGoal()) + 1);
                    } catch (NullPointerException e) {
                        Application.getLogger(HealthMonitor.class.getName()).debug("#1: " + registeredChannel);
                        Application.getLogger(HealthMonitor.class.getName()).debug("#2: " + registeredChannel.getParentGoal());
                        Application.getLogger(HealthMonitor.class.getName()).debug("#3: " + waitingChannelsCount.get(registeredChannel.getParentGoal()));
                        System.exit(1);
                    }
                }
            }
            Application.getLogger(getClass().getName()).debug("===============================================");
            for (ChannelExtension registeredChannel : this.manager.getRegisteredChannels()) {
                Application.getLogger(getClass().getName()).debug("[" + registeredChannel.getParentGoal().getName() + ":" + registeredChannel.getChannelName() + "]");
            }
            for (Entry<Goal, Integer> entrySet : waitingChannelsCount.entrySet()) {
                Goal g = entrySet.getKey();
                int count = entrySet.getValue();
                if (count < 0) {
                    for (int i = 0; i < 0; i++) {
                        if (!g.isHooked()) {
                            break;
                        }
                        try {
                            Application.getLogger(getClass().getName()).debug("g.createChannel()");
                            final List<ChannelExtension> tmpChannel = new ArrayList<>(1);
                            g.createChannel(new Goal.ReturnAction() {

                                @Override
                                public void setNewChannel(ChannelExtension channel) {
                                    tmpChannel.add(channel);
                                }

                                @Override
                                public void run() {
                                    synchronized (tmpChannel) {
                                        tmpChannel.notify();
                                    }
                                }
                            });
                            synchronized (tmpChannel) {
                                tmpChannel.wait();
                            }
                            ChannelExtension newChnEx = tmpChannel.get(0);
                            newChnEx.setLocked(false);
                        } catch (IOException ex) {
                            Application.getLogger(getClass().getName()).debug(ex.getMessage(), ex);
                            break;
                        } catch (InterruptedException ex) {
                            Logger.getLogger(HealthMonitor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            Application.getLogger(getClass().getName()).info(waitingChannelsCount);
        }
    }

}
