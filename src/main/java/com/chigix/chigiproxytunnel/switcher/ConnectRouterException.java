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
public class ConnectRouterException extends Exception {

    public ConnectRouterException() {
    }

    public ConnectRouterException(String message) {
        super(message);
    }

    public ConnectRouterException(Throwable cause) {
        super(cause);
    }

    public ConnectRouterException(String message, Throwable cause) {
        super(message, cause);
    }

}
