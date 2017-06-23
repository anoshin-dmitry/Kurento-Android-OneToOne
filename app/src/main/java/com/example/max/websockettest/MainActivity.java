package com.example.max.websockettest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.max.websockettest.webrtc.RtcListener;
import com.example.max.websockettest.webrtc.WebRtcClient;
import com.example.max.websockettest.webrtc.commands.RegisterNameCommand;
import com.example.max.websockettest.webrtc.commands.RejectCallCommand;

import org.webrtc.MediaStream;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.Random;


public class MainActivity extends Activity implements RtcListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;

    private WebRtcClient rtcClient;


    private static final int LOCAL_X = 0;
    private static final int LOCAL_Y = 0;
    private static final int LOCAL_WIDTH = 100;
    private static final int LOCAL_HEIGHT = 100;

    private static final int CAMERA_WIDTH = 320;
    private static final int CAMERA_HEIGHT = 240;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_FIT;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;

    private String ourName;
    private TextView userNameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissionForCameraAndMicrophone();

        vsv = (GLSurfaceView) findViewById(R.id.glview_call);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new Runnable() {
            @Override
            public void run() {
                init();
            }
        });

        // local and remote render
        localRender = VideoRendererGui.create(LOCAL_X, LOCAL_Y, LOCAL_WIDTH, LOCAL_HEIGHT, scalingType, true);

        Random rand = new Random();
        ourName = "Test-" + String.valueOf(rand.nextInt(10));
        userNameView = (TextView) findViewById(R.id.user_name_view);

    }

    private void init() {
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        rtcClient = new WebRtcClient(MainActivity.this, MainActivity.this, "wss://nxtbase.de:8443/call", localRender, CAMERA_WIDTH, CAMERA_HEIGHT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {

        switch(view.getId()) {
            case R.id.call_answer_button:
                break;
            case R.id.call_reject_button:
                break;
            default:
                Toast.makeText(this, "Not yet implemented", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSocketCompleted() {
        rtcClient.sendMessage(new RegisterNameCommand("194282", "2868", ourName));
    }

    @Override
    public  void onRegisterSuccessed() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "User Register" + ourName + "Successed", Toast.LENGTH_SHORT).show();
                userNameView.setText("User Name : " + ourName);
            }
        });
    }

    @Override
    public void onRegisterFailed() {
        runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                Toast.makeText(MainActivity.this, "User Register Failed", Toast.LENGTH_SHORT).show();
                userNameView.setText("User Register Failed");
                  }
          });
    }

    @Override
    public void onCallReceived(String from) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                rtcClient.webRTCApp.setUserId(ourName);
                                rtcClient.webRTCApp.setConnectionId(ourName);
                                rtcClient.webRTCApp.nbmWebRTCPeer.generateOffer(ourName, true);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                Button rejectButton = (Button) findViewById(R.id.call_reject_button);
                                rtcClient.sendMessage(new RejectCallCommand((String) rejectButton.getTag(), RejectCallCommand.USER_REJECT));
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Do you receive a call?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

//                Button answerButton = (Button) findViewById(R.id.call_answer_button);
//                answerButton.setVisibility(View.VISIBLE);
//                Button rejectButton = (Button) findViewById(R.id.call_reject_button);
//                rejectButton.setVisibility(View.VISIBLE);
            }
        });

        rtcClient.currentFrom = from;
        rtcClient.webRTCApp.setFromUserId(from);
    }

    @Override
    public void onCallReady(String callId) {

    }

    @Override
    public void onStatusChanged(String newStatus) {

    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)){
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void showAnswerBtn() {
        Button answerButton = (Button) findViewById(R.id.call_answer_button);
        answerButton.setVisibility(View.VISIBLE);
        Button rejectButton = (Button) findViewById(R.id.call_reject_button);
        rejectButton.setVisibility(View.VISIBLE);
    }

    private void hideAnswerBtn() {
        Button answerButton = (Button) findViewById(R.id.call_answer_button);
        answerButton.setVisibility(View.GONE);
        Button rejectButton = (Button) findViewById(R.id.call_reject_button);
        rejectButton.setVisibility(View.GONE);
    }
}
