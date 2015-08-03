/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.handler;

import com.chigix.chigiproxytunnel.channel.Channel;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public interface Processor {

    /**
     *
     * @param channel
     * @param input
     * @return The next processor.
     * @throws com.chigix.chigiproxytunnel.handler.ProcessorUpdateException
     */
    Processor update(Channel channel, int input) throws ProcessorUpdateException;

}
