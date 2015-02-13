package com.chigix.bio.proxy.channel;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class Tunnel {

    /**
     * The channel between Client/Browser and Proxy Server
     */
    private final Channel requestClientChannel;
    /**
     * The channel between TargetHost/Server and Proxy Server
     */
    private final Channel targetHostChannel;

    private String tunnelName;

    public Tunnel(String tunnelName, Channel requestClientChannel, Channel targetHostChannel) {
        this.requestClientChannel = requestClientChannel;
        this.targetHostChannel = targetHostChannel;
        this.tunnelName = tunnelName;
        if (targetHostChannel != null) {
            targetHostChannel.registerTunnel(this);
        }
        if (requestClientChannel != null) {
            requestClientChannel.registerTunnel(this);
        }
    }

    public Tunnel(Channel requestClientChannel, Channel targetHostChannel) {
        this(requestClientChannel.getRemoteHostAddress() + ":" + targetHostChannel.getRemoteHostAddress(), requestClientChannel, targetHostChannel);
    }

    public Channel getRequestClientChannel() {
        return requestClientChannel;
    }

    public Channel getTargetHostChannel() {
        return targetHostChannel;
    }

    public String getTunnelName() {
        return tunnelName;
    }

    public void setTunnelName(String tunnelName) {
        this.tunnelName = tunnelName;
    }

    public void close() {
        if (this.requestClientChannel != null) {
            this.requestClientChannel.close();
        }
        if (this.targetHostChannel != null) {
            this.targetHostChannel.close();
        }
    }

}
