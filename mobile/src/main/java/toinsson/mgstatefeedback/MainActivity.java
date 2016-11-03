package toinsson.mgstatefeedback;

import android.app.Activity;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements
        CapabilityApi.CapabilityListener,
        MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    //Request code for launching the Intent to resolve Google Play services errors.
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String SEND_STATE_PATH = "/send-state";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    private View mStartActivityBtn;
    private TextView mTextView;
    private TextView mIpText;
    private Spinner mSpinner;
    private boolean serverConnected;
    private Thread serverThread;
    private Button mConnectButton;


    private void subMessageReceived(Bundle messageBundle) {
        String msg = messageBundle.getString("msg");
        Log.d(TAG, "subMessageReceived: "+msg);

        mTextView.setText(msg);
        //mTextView.setBackgroundColor(color); TODO

        // propagate the msg to the watch
        new sendStateTask().execute(msg);
    }

    private final MessageListenerHandler serverMessageHandler = new MessageListenerHandler(
        new IMessageListener() {
            @Override
            public void messageReceived(Bundle messageBundle) {subMessageReceived(messageBundle);}
        });

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {

        mStartActivityBtn = findViewById(R.id.start_wearable_activity);
        mTextView = (TextView) findViewById(R.id.statetextview);
        mIpText = (TextView) findViewById(R.id.iptext);

        mConnectButton = (Button) findViewById(R.id.connectbutton);
        mSpinner = (Spinner) findViewById(R.id.spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ipaddress_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinner.setAdapter(adapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        serverConnected = false;
        // the SUB need to be launched via connect button
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        if (!mResolvingError && (mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Google API Client was connected");
        mResolvingError = false;
        mStartActivityBtn.setEnabled(true);

        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addListener(
                mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API client was suspended");
        mStartActivityBtn.setEnabled(false);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mResolvingError) {

            if (result.hasResolution()) {
                try {
                    mResolvingError = true;
                    result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    mGoogleApiClient.connect();
                }
            } else {
                Log.e(TAG, "Connection to Google API client has failed");
                mResolvingError = false;
                mStartActivityBtn.setEnabled(false);
                Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            }
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        Log.d(TAG, "onCapabilityChanged: " + capabilityInfo);
    }


    public void onConnectClick(View view){

        Integer viewId = view.getId();
        String ip = "ip_not_initialised";

        Log.d(TAG, viewId +" "+ mConnectButton.getId());

        if (viewId.equals(mConnectButton.getId())) {
            ip = mIpText.getText().toString();
        }
        else if (viewId.equals(mSpinner.getId())) {
            ip = mSpinner.getSelectedItem().toString();
        }

        if (!serverConnected) {
            serverThread = new Thread(new ZeroMQSub(serverMessageHandler, ip));
            serverThread.start();
        }
        else {
            serverThread.interrupt();
            serverThread = new Thread(new ZeroMQSub(serverMessageHandler, ip));
            serverThread.start();
        }

        serverConnected = true;
    }

    /**
     * Sends an RPC to start a fullscreen Activity on the wearable.
     */
    public void onStartWearableActivityClick(View view) {
        Log.d(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    private class sendStateTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msg) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStateMessage(node, msg);
            }
            return null;
        }
    }
    private void sendStateMessage(String node, String... msg) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, SEND_STATE_PATH, msg[0].getBytes()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }
}
