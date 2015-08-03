/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.handler;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.channel.CloseReason;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public abstract class ProcessorHandler extends ChannelHandler {

    /**
     * Here is just dispatch each channel to their own thread.
     *
     * @TODO MAYBE here is chance to become a NON-Blocking mode via
     * {Channel:Processor} Map instead of the ThreadLocal.
     */
    private final ProcessorLocal processor;

    private final ThreadLocal<Map<String, Object>> lastProcessorLink;

    public ProcessorHandler(Processor init) {
        this.processor = new ProcessorLocal(init);
        this.lastProcessorLink = new ThreadLocal<>();
    }

    public Processor getInitProcessor() {
        return this.processor.getInitValue();
    }

    @Override
    public void channelActive(Channel channel) throws Exception {
        super.channelActive(channel);
    }

    @Override
    public void channelInactive(Channel channel, CloseReason reason) throws Exception {
        super.channelInactive(channel, reason);
        this.processor.remove();
        this.lastProcessorLink.remove();
    }

    @Override
    public final void channelRead(Channel channel, int input) {
        Processor p1 = this.processor.get();
        Processor p2;
        try {
            if (p1 == null || (p2 = p1.update(channel, input)) == null) {
                Logger.getLogger(ProcessorHandler.class.getName()).log(Level.INFO, "{0}:{1} handler Processor Ended.({2})", new Object[]{channel.getRemoteHostAddress(), channel.getRemotePort(), ProcessorHandler.class.getName()});
                channel.close();
                return;
            } else {
                this.processor.set(p2);
            }
        } catch (ProcessorUpdateException ex) {
            this.exceptionCaught(channel, ex.getCause());
            return;
        }
        if (true) {
            return;
        }
        if (this.lastProcessorLink.get() != null && this.lastProcessorLink.get().get("source") == p1 && this.lastProcessorLink.get().get("target") == p2) {
            int result = ((Integer) this.lastProcessorLink.get().get("count")) + 1;
            this.lastProcessorLink.get().put("count", result);
            if (result % 20 == 0) {
                Processor source_processor = (Processor) this.lastProcessorLink.get().get("source");
                Processor target_processor = (Processor) this.lastProcessorLink.get().get("target");
                String source_processor_class_name = source_processor.getClass().getName();
                String target_processor_class_name = target_processor.getClass().getName();
                if (source_processor.getClass().isAnonymousClass()) {
                    source_processor_class_name = source_processor.getClass().getSuperclass().getName();
                }
                if (target_processor.getClass().isAnonymousClass()) {
                    target_processor_class_name = target_processor.getClass().getSuperclass().getName();
                }
                Application.getLogger(getClass().getName()).debug(source_processor_class_name + " --> " + target_processor_class_name + ":[" + this.lastProcessorLink.get().get("count") + "]^");
            }
        } else {
            String source_processor_class_name = p1.getClass().getName();
            String target_processor_class_name = p2.getClass().getName();
            if (p1.getClass().isAnonymousClass()) {
                source_processor_class_name = p1.getClass().getSuperclass().getName();
            }
            if (p2.getClass().isAnonymousClass()) {
                target_processor_class_name = p2.getClass().getSuperclass().getName();
            }
            if (this.lastProcessorLink.get() != null) {
                Application.getLogger(getClass().getName()).debug(source_processor_class_name + ":[" + this.lastProcessorLink.get().get("count") + "]" + " --> " + target_processor_class_name);
            } else {
                Application.getLogger(getClass().getName()).debug("[INITIAL PROCESSOR] " + source_processor_class_name + " --> " + target_processor_class_name);
            }
            Map<String, Object> currentProcessorLink = new HashMap<>();
            currentProcessorLink.put("source", p1);
            currentProcessorLink.put("target", p2);
            currentProcessorLink.put("count", 1);
            this.lastProcessorLink.set(currentProcessorLink);
        }
    }

    private class ProcessorLocal extends ThreadLocal<Processor> {

        private final Processor initValue;

        public ProcessorLocal(Processor initValue) {
            this.initValue = initValue;
        }

        @Override
        protected Processor initialValue() {
            return this.initValue;
        }

        public Processor getInitValue() {
            return initValue;
        }

    }

}
