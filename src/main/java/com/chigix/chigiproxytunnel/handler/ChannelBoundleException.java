/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.handler;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelBoundleException extends Exception {

    public ChannelBoundleException() {
    }

    public ChannelBoundleException(String message) {
        super(message);
    }

    public ChannelBoundleException(Throwable cause) {
        super(cause);
    }

    public ChannelBoundleException(String message, Throwable cause) {
        super(message, cause);
    }

}
