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
public class DefaultEvent implements Event {

    private String eventName;

    private Object param;

    public DefaultEvent() {
        this.eventName = "UNKNOWN_EVENT";
    }

    public String getName() {
        return eventName;
    }

    public Object getParam() {
        return param;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

}
