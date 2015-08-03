/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher;

import com.chigix.chigiproxytunnel.channel.Channel;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class ChannelNameMap {

    private static final ConcurrentMap<Channel, String> channelNameMap;

    static {
        channelNameMap = new ConcurrentHashMap<>();
    }

    public static final ConcurrentMap<Channel, String> getInstance() {
        return channelNameMap;
    }

    public static final String generateCommanderName(Goal g) {
        return generateNameHelper(g, "COMMANDER");
    }

    public static final String generateChannelName(Goal g) {
        return generateNameHelper(g, "CHANNEL");
    }

    private static String generateNameHelper(Goal g, String prefix) {
        StringBuilder sb = new StringBuilder(g.getName() + "-" + prefix + "-");
        byte[] uuid;
        try {
            uuid = MessageDigest.getInstance("MD5").digest(UUID.randomUUID().toString().getBytes());
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
        for (int i = 0; i < 3; i++) {
            sb.append(Integer.toHexString(uuid[i] & 0xFF));
        }
        return sb.toString();
    }

    public static final void recordChannel(ChannelExtension channelEx) {
        channelNameMap.put(channelEx.getChannel(), "[@" + Integer.toHexString(channelEx.getChannel().hashCode()) + "/" + channelEx.getChannel().getRemoteHostAddress() + ":" + channelEx.getChannelName() + "]");
    }

    /**
     *
     * @param channel
     * @return
     */
    public static final String getName(Channel channel) {
        String name = channelNameMap.get(channel);
        if (name == null) {
            channelNameMap.putIfAbsent(channel, "[@" + Integer.toHexString(channel.hashCode()) + "/" + channel.getRemoteHostAddress() + "]");
            name = channelNameMap.get(channel);
        }
        return name;
    }

}
