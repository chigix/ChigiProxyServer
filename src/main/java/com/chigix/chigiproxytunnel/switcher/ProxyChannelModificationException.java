/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProxyChannelModificationException extends Exception {

    public ProxyChannelModificationException() {
    }

    public ProxyChannelModificationException(String message) {
        super(message);
    }

    public ProxyChannelModificationException(Throwable cause) {
        super(cause);
    }

    public ProxyChannelModificationException(String message, Throwable cause) {
        super(message, cause);
    }

}
