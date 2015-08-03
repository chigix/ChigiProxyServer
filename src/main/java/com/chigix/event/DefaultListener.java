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
public abstract class DefaultListener implements Listener, Comparable<Object> {

    @Override
    public int compareTo(Object o) {
        return this.hashCode() - o.hashCode();
    }

}
