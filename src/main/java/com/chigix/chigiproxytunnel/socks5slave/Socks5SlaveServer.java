/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.socks5slave;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.channel.CloseReason;
import com.chigix.chigiproxytunnel.handler.ChannelBoundProcessor;
import com.chigix.chigiproxytunnel.handler.ProcessorHandler;
import com.chigix.chigiproxytunnel.switcher.ChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Socks5SlaveServer extends ProcessorHandler {

    public Socks5SlaveServer(final ChannelExtension chnEx, final SwitcherTable switcher) {
        super(new Socks5Init() {

            @Override
            protected void initProcessorsChain() {
                switcher.addGoal(chnEx.getParentGoal());
                //Socks5Auth auth = new Socks5Auth();
                Socks5RequestTypeDetect requestType = new Socks5RequestTypeDetect();
                Socks5AddressTypeDetect addressType = new Socks5AddressTypeDetect();
                AddressDomainRead domainReader = new AddressDomainRead();
                AddressIpv4Read ipv4Reader = new AddressIpv4Read();
                AddressIpv6Read ipv6Reader = new AddressIpv6Read();
                PortRead portReader = new PortRead(switcher);
                //this.setNext(auth);
                this.setNext(requestType);
                //auth.setRequestDetector(requestType);
                requestType.setAddressTypeDetector(addressType);
                addressType.setDomainRead(domainReader);
                addressType.setIpv4Read(ipv4Reader);
                addressType.setIpv6Read(ipv6Reader);
                domainReader.setPortReader(portReader);
                ipv4Reader.setPortReader(portReader);
                // @TODO:
                //ipv6Reader.setPortReader(portReader);
                portReader.setTransferer(new ChannelBoundProcessor());
            }

        });
    }

    @Override
    public String getHandlerName() {
        return "Socks5SlaveServer";
    }

    @Override
    public void channelActive(Channel channel) throws Exception {
        super.channelActive(channel);
        ChannelNameMap.getInstance().put(channel, "[@" + Integer.toHexString(channel.hashCode()) + "/" + channel.getRemoteHostAddress() + ":Socks5Channel]");
        this.logInfo(channel, "NEW CHANNEL CONNECT IN");
    }

    @Override
    public void channelInactive(Channel channel, CloseReason reason) throws Exception {
        super.channelInactive(channel, reason);
        switch (reason) {
            case RemoteClose:
                this.logInfo(channel, "CHANNEL CLOSE.[CLIENT CLOSE]");
                break;
            case RemoteEnd:
                this.logInfo(channel, "CHANNEL CLOSE.[CLIENT END]");
                break;
            case SelfClose:
            default:
                this.logInfo(channel, "CHANNEL CLOSE.[SERVER CLOSE]");
                break;
        }
    }

    @Override
    public void exceptionCaught(Channel channel, Throwable cause) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static final void logInfo(Channel channel, String logMsg) {
        Application.getLogger(Socks5SlaveServer.class.getName()).info(ChannelNameMap.getName(channel) + "\t" + logMsg);
    }

}
