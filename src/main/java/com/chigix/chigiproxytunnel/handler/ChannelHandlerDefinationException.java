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
public class ChannelHandlerDefinationException extends Exception {

    public ChannelHandlerDefinationException(ChannelHandler handler) {
        super("[" + handler.getHandlerName() + "] has not inherit super method.");
    }

}
