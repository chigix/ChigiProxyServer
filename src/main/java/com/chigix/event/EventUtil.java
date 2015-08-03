/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.event;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class EventUtil {

    public static final void castEvent(Event e, Collection<Listener> listeners) {
        Iterator<Listener> it = listeners.iterator();
        while (it.hasNext()) {
            Listener listener = it.next();
            listener.performEvent(e);
        }
    }
}
