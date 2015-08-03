/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.chigiproxytunnel.switcher.command;

import com.chigix.chigiproxytunnel.switcher.CommandChannelExtension;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class CommandResponse implements Command {

    private int responseCode;

    private String responseMessage;

    private Object responseData;

    private String commandId;

    private String goalName;

    private String channelName;

    /**
     *
     * @param responseCode
     * @param responseMessage
     * @param responseData
     * @param commandId The command ID FROM request Command.
     * @param channelEx The commander channel for this operation.
     * @return
     */
    public static final CommandResponse createInstance(int responseCode, String responseMessage, Object responseData, String commandId, CommandChannelExtension channelEx) {
        CommandResponse resp = new CommandResponse();
        resp.setCommanderGoalName(channelEx.getParentGoal().getName());
        resp.setCommanderChannelName(channelEx.getChannelName());
        resp.setCode(responseCode);
        resp.setMessage(responseMessage);
        resp.setData(responseData);
        resp.setId(commandId);
        return resp;
    }

    public CommandResponse() {
    }

    @Override
    public String getName() {
        return CommandResponse.class.getName();
    }

    @Override
    public String getCommanderGoalName() {
        return this.goalName;
    }

    @Override
    public String getCommanderChannelName() {
        return this.channelName;
    }

    public int getCode() {
        return responseCode;
    }

    public String getMessage() {
        return responseMessage;
    }

    public Object getData() {
        return responseData;
    }

    @Override
    public String getId() {
        return this.commandId;
    }

    @Override
    public void processResponse(CommandResponse resp, Dispatchable.ProcessorsArg processorArg) {
        throw new UnsupportedOperationException("Not supported yet:" + getMessage()); //To change body of generated methods, choose Tools | Templates.
    }

    public void setCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public void setMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public void setData(Object responseData) {
        this.responseData = responseData;
    }

    public void setId(String commandId) {
        this.commandId = commandId;
    }

    public void setCommanderGoalName(String goalName) {
        this.goalName = goalName;
    }

    public void setCommanderChannelName(String channelName) {
        this.channelName = channelName;
    }

}
