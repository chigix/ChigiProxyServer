package com.chigix.bio.proxy.handler.http;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class InvalidRequestUriException extends HttpProxyException {

    private final String uri;
    private final String reason;

    public InvalidRequestUriException(String uri, String reason) {
        super(reason + ":" + uri);
        this.uri = uri;
        this.reason = reason;
    }

    public InvalidRequestUriException(String uri) {
        this(uri, "Invalid Request Uri");
    }

}
