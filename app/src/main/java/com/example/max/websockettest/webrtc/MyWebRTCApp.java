package com.example.max.websockettest.webrtc;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.util.Log;
import android.util.Size;

import com.example.max.websockettest.webrtc.commands.AcceptCallCommand;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection.IceConnectionState;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMMediaConfiguration;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMPeerConnection;
import fi.vtt.nubomedia.webrtcpeerandroid.NBMWebRTCPeer;

public class MyWebRTCApp implements NBMWebRTCPeer.Observer {
    private final static String TAG = WebRtcClient.class.getCanonicalName();

    VideoRenderer.Callbacks localRender;

    public NBMWebRTCPeer nbmWebRTCPeer;
    Context mContext;
    WebRtcClient client;

    private String userId;
    private String fromUserId;
    private String connectionId;

    public MyWebRTCApp(WebRtcClient client, Context context, VideoRenderer.Callbacks localRender, int width, int height)
    {
        mContext = context;
        this.localRender = localRender;
        this.client = client;

        Point displaySize = new Point();

        NBMMediaConfiguration.NBMVideoFormat receiverVideoFormat = new NBMMediaConfiguration.NBMVideoFormat(width, height, ImageFormat.NV21, 30);
        NBMMediaConfiguration mediaConfiguration = new NBMMediaConfiguration(receiverVideoFormat);

        nbmWebRTCPeer = new NBMWebRTCPeer(mediaConfiguration, mContext, localRender, this);

        nbmWebRTCPeer.addIceServer("stun:77.72.174.163:3478");
        nbmWebRTCPeer.initialize();
    }

    /* Observer methods and the rest of declarations */
    public void onLocalSdpOfferGenerated(SessionDescription localSdpOffer, NBMPeerConnection connection) {
        client.sendMessage(new AcceptCallCommand(fromUserId, localSdpOffer.description));
        Log.d(TAG, "onLocalSdpOfferGenerated");
    }
    public void onLocalSdpAnswerGenerated(SessionDescription localSdpAnswer, NBMPeerConnection connection) {
        client.sendMessage(new AcceptCallCommand(fromUserId, localSdpAnswer.description));
        Log.d(TAG, "onLocalSdpAnswerGenerated");
    }
    public void onIceCandidate(IceCandidate localIceCandidate, NBMPeerConnection connection) {
        try {
            Log.d("TAG", "onIceCandidate");
            JSONObject payload = new JSONObject();
            payload.put("sdpMLineIndex", localIceCandidate.sdpMLineIndex);
            payload.put("sdpMid", localIceCandidate.sdpMid);
            payload.put("candidate", localIceCandidate.sdp);

            JSONObject response = new JSONObject();
            response.put("id", "onIceCandidate");
            response.put("candidate",payload);

            client.sendMessage(response.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void onIceStatusChanged(IceConnectionState state, NBMPeerConnection connection) {
        Log.d(TAG, "onIceStatusChanged");
    }
    public void onRemoteStreamAdded(MediaStream stream, NBMPeerConnection connection) {
        Log.d(TAG, "onRemoteStreamAdded");
    }
    public void onRemoteStreamRemoved(MediaStream stream, NBMPeerConnection connection) {
        Log.d(TAG, "onRemoteStreamRemoved");
    }
    public void onPeerConnectionError(String error) {
        Log.d(TAG, "onPeerConnectionError");
    }
    public void onDataChannel(DataChannel dataChannel, NBMPeerConnection connection) {  }

    @Override
    public void onBufferedAmountChange(long l, NBMPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onStateChange(NBMPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection, DataChannel channel) {

    }

    public void onBufferedAmountChange(long l, NBMPeerConnection connection) {  }
    public void onStateChange(NBMPeerConnection connection) {  }
    public void onMessage(DataChannel.Buffer buffer, NBMPeerConnection connection) {  }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(String fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

}