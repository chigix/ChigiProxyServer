/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

import com.chigix.chigiproxytunnel.handler.Processor;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.chigiproxytunnel.switcher.processors.ProcessorsPackage;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public interface Dispatchable {

    void processRequest(CommandChannelExtension commander, SwitcherTable switcher, ProcessorsArg processor) throws CommandChannelProcessor.CommandProcessException;

    public class ProcessorsArg {

        private Processor request;
        private Processor response;
        private ProcessorsPackage pkg;

        public Processor getRequest() {
            return request;
        }

        public void setRequest(Processor request) {
            this.request = request;
        }

        public Processor getResponse() {
            return response;
        }

        public void setResponse(Processor response) {
            this.response = response;
        }

        public ProcessorsPackage getPkg() {
            return pkg;
        }

        public void setPkg(ProcessorsPackage pkg) {
            this.pkg = pkg;
        }

    }
}
