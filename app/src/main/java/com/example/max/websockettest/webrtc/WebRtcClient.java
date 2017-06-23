package com.example.max.websockettest.webrtc;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.max.websockettest.R;
import com.example.max.websockettest.webrtc.commands.RejectCallCommand;
import com.example.max.websockettest.webrtc.commands.RtcCommandBase;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Max on 10-4-2015.
 */
public class WebRtcClient implements AsyncHttpClient.WebSocketConnectCallback, WebSocket.StringCallback {
    private final static String TAG = WebRtcClient.class.getCanonicalName();

    private static final int CALLSTATE_IN_CALL = 3;

    public RtcListener mListener;
    private WebSocket socket;

    private int callState;

    private static final int CALLSTATE_NO_CALL = 0;
    private static final int CALLSTATE_POST_CALL = 1;
    private static final int CALLSTATE_DISABLED = 2;

    public String currentFrom;

    public MyWebRTCApp webRTCApp;

    Context mContext;

    public WebRtcClient(Context context, RtcListener listener, String host, VideoRenderer.Callbacks localRender, int width, int height) {

        mContext = context;

        webRTCApp = new MyWebRTCApp(this, mContext, localRender, width, height);

        mListener = listener;

        AsyncHttpClient client = AsyncHttpClient.getDefaultInstance();

        TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        } };

        SSLContext sslContext=null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, byPassTrustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        client.getSSLSocketMiddleware().setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        client.getSSLSocketMiddleware().setSSLContext(getSslContext());
        client.getSSLSocketMiddleware().setTrustManagers(byPassTrustManagers);

        client.websocket(host, null, this);

    }

    public void sendMessage(RtcCommandBase message) {
        try {
            sendMessage(message.compile());
        } catch(JSONException jse) {
            Log.e(TAG, jse.getMessage());
        }
    }

    public void sendMessage(String message) {
        if(socket != null && socket.isOpen()) {
            socket.send(message);
        }
    }

    @Override
    public void onCompleted(Exception ex, WebSocket webSocket) {
        socket = webSocket;
        if (socket != null)
            socket.setStringCallback(this);
        mListener.onSocketCompleted();
    }

    @Override
    public void onStringAvailable(final String s) {
        System.out.println("I got a string: " + s);
        JSONObject parsedMessage = null;
        try {
            parsedMessage = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (parsedMessage != null) {
            String messageId = "";
            try {
                messageId = parsedMessage.getString("id");
                if(parsedMessage.has("from")) {
                    String from = parsedMessage.getString("from");

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            switch (messageId) {
                case "resgisterResponse":
                    registerResponse(parsedMessage);
                    break;
                case "incomingCall":
                    incomingCall(parsedMessage);
                    break;
                case "startCommunication":
                    startCommunication(currentFrom ,parsedMessage);
                    break;
                case "iceCandidate":
                    Log.i(TAG, "iceCandidate");
                    if (parsedMessage != null) {
                        try {
                            JSONObject data = parsedMessage.getJSONObject("candidate");
                            String sdpMid = data.getString("sdpMid");
                            int sdpMLineIndex = data.getInt("sdpMLineIndex");
                            String sdp = data.getString("candidate");
                            IceCandidate candidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                            webRTCApp.nbmWebRTCPeer.addRemoteIceCandidate(candidate, webRTCApp.getConnectionId());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                default:
                    Log.i(TAG, "JSON Request not handled");
            }
        } else {
            Log.i(TAG, "Invalid JSON");
        }
    }

    private void incomingCall(JSONObject parsedMessage) {
        // If bussy just reject without disturbing user
        String from = "";
        try {
            from = parsedMessage.getString("from");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (callState != CALLSTATE_NO_CALL && callState != CALLSTATE_POST_CALL) {
            sendMessage(new RejectCallCommand(from, RejectCallCommand.USER_BUSY));
        } else {
            mListener.onCallReceived(from);
        }

        setCallState(CALLSTATE_DISABLED);
    }

    private void startCommunication(String from, JSONObject parsedMessage) {
        setCallState(CALLSTATE_IN_CALL);
        Log.d(TAG,"SDP answer received, setting remote description");
        try {
            SessionDescription sdpAnswer = new SessionDescription(SessionDescription.Type.ANSWER, parsedMessage.getString("sdpAnswer"));
            webRTCApp.nbmWebRTCPeer.processAnswer(sdpAnswer, webRTCApp.getConnectionId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setCallState(int newState) {
        callState = newState;
    }

    private void registerResponse(JSONObject message) {
        try {
            String response = message.getString("response");
            if (response.equalsIgnoreCase("accepted")) {
                mListener.onRegisterSuccessed();
                Log.i(TAG, "Register success");
            } else {
                mListener.onRegisterFailed();
                String errorMessage = !message.getString("message").isEmpty() ? message.getString("message") : "Unknown register rejection reason";
                Log.e("RegisterResponse", errorMessage);
                Log.i(TAG, "Error registering user. See console for further information.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public SSLContext getSslContext() {

        TrustManager[] byPassTrustManagers = new TrustManager[] { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        } };

        SSLContext sslContext=null;

        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            sslContext.init(null, byPassTrustManagers, new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return sslContext;
    }
}
