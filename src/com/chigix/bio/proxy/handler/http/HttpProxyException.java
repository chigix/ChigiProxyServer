package com.chigix.bio.proxy.handler.http;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class HttpProxyException extends Exception {

    public HttpProxyException() {
    }

    public HttpProxyException(String message) {
        super(message);
    }

    public HttpProxyException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpProxyException(Throwable cause) {
        super(cause);
    }

}
