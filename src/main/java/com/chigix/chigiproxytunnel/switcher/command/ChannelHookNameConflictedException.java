/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelHookNameConflictedException extends Exception {

    public ChannelHookNameConflictedException() {
    }

    public ChannelHookNameConflictedException(String message) {
        super(message);
    }

    public ChannelHookNameConflictedException(Throwable cause) {
        super(cause);
    }

    public ChannelHookNameConflictedException(String message, Throwable cause) {
        super(message, cause);
    }

}
