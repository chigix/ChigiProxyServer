/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

import com.chigix.chigiproxytunnel.Application;
import com.chigix.chigiproxytunnel.switcher.ChannelExtension;
import com.chigix.chigiproxytunnel.switcher.ChannelNameMap;
import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;
import com.chigix.chigiproxytunnel.switcher.SwitcherTable;
import com.chigix.chigiproxytunnel.switcher.goal.Goal;
import com.chigix.chigiproxytunnel.switcher.processors.CommandChannelProcessor;
import com.chigix.event.Event;
import com.chigix.event.Listener;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The flow for createChannel is:<br>
 *
 * <code>
 * CreateChannel-->CommandHookChannel-->Response for  CommandHookChannel-->Response for CreateChannel.
 * </code>
 *
 * Response For CommandHookChannel is sent from hook target, and the response
 * for CreateChannel is a listener embeded in Response for CommandHookChannel
 * Sent.
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class CreateChannel implements Command, Dispatchable {

    private String id;

    private String goalName;
    private String channelName;

    private String newlyCreatedChannelName;

    private final Set<Listener> createResponseListeners;

    /**
     * The goal from which new channel to create.
     *
     * @param g
     * @return
     */
    public static CreateChannel createInstance(Goal g) {
        CreateChannel cmd = new CreateChannel();
        cmd.setCommanderChannelName(g.getCommandChannel().getChannelName());
        cmd.setCommanderGoalName(g.getName());
        cmd.setId(UUID.randomUUID().toString());
        return cmd;
    }

    public CreateChannel() {
        this.createResponseListeners = new CopyOnWriteArraySet<>();
    }

    @Override
    public String getName() {
        return CreateChannel.class.getName();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getCommanderGoalName() {
        return this.goalName;
    }

    @Override
    public String getCommanderChannelName() {
        return this.channelName;
    }

    @Override
    public void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) throws CommandChannelProcessor.CommandProcessException {
        if (resp.getId().equals(this.getId())) {
            this.newlyCreatedChannelName = (String) resp.getData();
            Application.getLogger(getClass().getName()).info("Create Response Received: " + resp.getData());
            Iterator<Listener> it = this.createResponseListeners.iterator();
            Event e = new ResponseEvent(this);
            while (it.hasNext()) {
                Listener listener = it.next();
                try {
                    listener.performEvent(e);
                } catch (Exception ex) {
                    throw new CommandChannelProcessor.CommandProcessException(ex);
                }
            }
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCommanderGoalName(String goalName) {
        this.goalName = goalName;
    }

    public void setCommanderChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getNewlyCreatedChannelName() {
        return newlyCreatedChannelName;
    }

    public void setNewlyCreatedChannelName(String newlyCreatedChannelName) {
        this.newlyCreatedChannelName = newlyCreatedChannelName;
    }

    @Override
    public void processRequest(final CommandChannelExtension commander, SwitcherTable switcher, ProcessorsArg processor) {
        try {
            switcher.getGoal(getCommanderGoalName()).createChannel(new Goal.ReturnAction() {

                private ChannelExtension newChannel;

                @Override
                public void setNewChannel(ChannelExtension channel) {
                    this.newChannel = channel;
                }

                @Override
                public void run() {
                    Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(this.newChannel.getChannel()) + "\tChannel Created.(" + CreateChannel.class.getName() + ")");
                    try {
                        commander.sendCommand(CommandResponse.createInstance(200, ChannelNameMap.getName(this.newChannel.getChannel()) + " Channel Created.", this.newChannel.getChannelName(), getId(), commander));
                        Application.getLogger(getClass().getName()).info(ChannelNameMap.getName(this.newChannel.getChannel()) + "\tChannel Create Response Sent");
                    } catch (IOException ex) {
                        Application.getLogger(getClass().getName()).fatal(ex.getMessage(), ex);
                        Application.getLogger(getClass().getName()).debug(ChannelNameMap.getName(this.newChannel.getChannel()) + "\tChannel Create Response Sent Failed.");
                    }
                }
            });
        } catch (IOException ex) {
            Application.getLogger(getClass().getName()).debug(ex.getMessage(), ex);
            processor.setResponse(null);
        }
        processor.setResponse(processor.getPkg().getDispatcher());
    }

    public void addResponseListener(Listener l) {
        this.createResponseListeners.add(l);
    }

    public class ResponseEvent implements Event {

        private final CreateChannel cmd;

        public ResponseEvent(CreateChannel cmd) {
            this.cmd = cmd;
        }

        @Override
        public String getName() {
            return "CHIGIX_EVENT_CREATECHANNEL_RESPONSE_PROCESSING_EVENT";
        }

        public CreateChannel getCmd() {
            return cmd;
        }

    }

}
