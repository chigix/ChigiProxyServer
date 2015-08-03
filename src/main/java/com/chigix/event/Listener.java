/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.event;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public interface Listener {

    /**
     * Call the DefaultEvent listener, if it is defined.
     *
     * @param e
     */
    public void performEvent(Event e);

}
