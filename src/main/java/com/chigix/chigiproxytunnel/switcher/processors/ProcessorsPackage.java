/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.processors;

import com.chigix.chigiproxytunnel.handler.ChannelBoundProcessor;
import com.chigix.chigiproxytunnel.handler.Processor;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ProcessorsPackage {

    private Authenticator authenticator;
    private CommandDispatcher dispatcher;
    private ChannelBoundProcessor proxyTransferer;

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public CommandDispatcher getDispatcher() {
        return dispatcher;
    }

    public ChannelBoundProcessor getProxyTransferer() {
        return proxyTransferer;
    }

    public void offerProcessor(Processor p) {
        if (p instanceof Authenticator) {
            this.authenticator = (Authenticator) p;
        } else if (p instanceof CommandDispatcher) {
            this.dispatcher = (CommandDispatcher) p;
        } else if (p instanceof ChannelBoundProcessor) {
            this.proxyTransferer = (ChannelBoundProcessor) p;
        }
    }
}
