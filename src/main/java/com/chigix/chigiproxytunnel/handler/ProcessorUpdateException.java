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
public class ProcessorUpdateException extends Exception {

    public ProcessorUpdateException() {
    }

    public ProcessorUpdateException(String message) {
        super(message);
    }

    public ProcessorUpdateException(Throwable cause) {
        super(cause);
    }

    public ProcessorUpdateException(String message, Throwable cause) {
        super(message, cause);
    }

}
