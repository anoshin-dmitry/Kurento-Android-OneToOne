package com.example.max.websockettest.webrtc.commands;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Max on 13-4-2015.
 */
public class RegisterNameCommand extends RtcCommandBase{
    private String claimId;
    private String userId;
    private String name;

    public RegisterNameCommand(String claimId, String userId, String name) {
        this.claimId = claimId;
        this.userId = userId;
        this.name = name;
    }

    @Override
    public String compile() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("ClaimID", claimId);
        json.put("UserID", userId);
        json.put("id", "register");
        json.put("name", name);
        return json.toString();
    }
}
