/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.discard;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.channel.CloseReason;
import com.chigix.chigiproxytunnel.handler.ChannelHandler;
import java.io.IOException;
import org.apache.logging.log4j.Level;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class DiscardServerChannelHandler extends ChannelHandler {

    private int receiveCount;

    public int getReceiveCount() {
        return receiveCount;
    }

    public DiscardServerChannelHandler() {
        this.receiveCount = 0;
    }

    @Override
    public void channelActive(final Channel channel) {
        final DiscardServerChannelHandler handler = this;
        Application.getLogger(getClass().getName()).log(Level.INFO, "Connection Accepted");
        new Thread() {

            @Override
            public void run() {
                int historyReceiveCount = 0;
                while (true) {
                    if (channel.isClosedFlag()) {
                        return;
                    }
                    int currentReceiveCount = handler.getReceiveCount();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                    Application.getLogger(getClass().getName()).info("======================================================");
                    Application.getLogger(getClass().getName()).info("RECEIVE SPEED: " + ((float) (currentReceiveCount - historyReceiveCount) / (float) 1024) + "kb/s");
                    Application.getLogger(getClass().getName()).info("RECEIVE COUNT: " + ((float) currentReceiveCount / (float) 1024) + "kb");
                    historyReceiveCount = currentReceiveCount;
                }
            }

        }.start();
        Application.getThreadPool().execute(new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        channel.getOutputStream().write((int) (Math.random() * 255));
                    } catch (IOException ex) {
                        Application.getLogger(getClass().getName()).info("Connection Closed From Client");
                        channel.close();
                        return;
                    }
                }
            }

        });
    }

    @Override
    public void channelInactive(Channel channel, CloseReason reason) {
        Application.getLogger(getClass().getName()).log(Level.INFO, "Connection Aborted");
    }

    @Override
    public void channelRead(Channel channel, int input) {
        this.receiveCount++;
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable cause) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
