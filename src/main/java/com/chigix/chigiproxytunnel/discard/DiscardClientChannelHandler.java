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
public class DiscardClientChannelHandler extends ChannelHandler {

    private int receiveCount = 0;

    public int getReceiveCount() {
        return this.receiveCount;
    }

    @Override
    public void channelActive(final Channel channel) {
        Application.getLogger(getClass().getName()).log(Level.INFO, "TARGET CONNECTED");
        final DiscardClientChannelHandler client = this;
        new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        channel.getOutputStream().write(1 + (int) (Math.random() * 255));
                    } catch (IOException ex) {
                        Application.getLogger(getClass().getName()).debug("Connection Closed From Host.");
                        channel.close();
                        return;
                    }
                }
            }

        }.start();
        new Thread() {

            @Override
            public void run() {
                int historyReceiveCount = client.getReceiveCount();
                while (true) {
                    if (channel.isClosedFlag()) {
                        return;
                    }
                    int currentReceiveCount = client.getReceiveCount();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Application.getLogger(getClass().getName()).debug("InterruptedException", ex);
                    }
                    Application.getLogger(getClass().getName()).info("RECEIVE SPEED: " + ((float) (currentReceiveCount - historyReceiveCount) / (float) 1024) + "kb/s");
                    Application.getLogger(getClass().getName()).info("RECEIVE: " + ((float) currentReceiveCount / (float) 1024) + "kb");
                    historyReceiveCount = currentReceiveCount;
                }
            }

        }.start();
    }

    @Override
    public void channelInactive(Channel channel, CloseReason reason) {
        Application.getLogger(getClass().getName()).log(Level.INFO, "CONNECTION ABORTED.");
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
