/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.goal;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class GoalChannelNotRegisteredException extends Exception {

    public GoalChannelNotRegisteredException(Goal goal) {
        super(goal.getName() + ": No Channel Has Been Registered for this goal.");
    }

}
