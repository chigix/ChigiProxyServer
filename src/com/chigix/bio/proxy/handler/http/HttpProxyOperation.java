package com.chigix.bio.proxy.handler.http;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public enum HttpProxyOperation {

    SEND_TO_PROXY_DIRECTLY, WAIT_FOR_HEADER_PARSE, DISCARD
}
