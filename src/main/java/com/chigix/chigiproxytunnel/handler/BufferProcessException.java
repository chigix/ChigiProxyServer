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
public class BufferProcessException extends Exception {

    public BufferProcessException() {
    }

    public BufferProcessException(String message) {
        super(message);
    }

    public BufferProcessException(Throwable cause) {
        super(cause);
    }

    public BufferProcessException(String message, Throwable cause) {
        super(message, cause);
    }

}
