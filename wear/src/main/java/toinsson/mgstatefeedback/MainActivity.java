package toinsson.mgstatefeedback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static toinsson.mgstatefeedback.R.drawable.tick;

public class MainActivity extends WearableActivity {

    private static final String TAG = "watch-MainActivity";

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private RelativeLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private ImageView mImageView;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (RelativeLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.state);
        mClockView = (TextView) findViewById(R.id.clock);
        mImageView = (ImageView) findViewById(R.id.imageView);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra(DataLayerListenerService.SEND_STATE_DATA);
                Log.d(TAG, "received broadcast from service " + s);

                if (s.contains("grasp")) {
                    Resources res = getResources();
                    Drawable drawable = res.getDrawable(R.drawable.tick);
                    mImageView.setBackground(drawable);
                }
                else if (s.contains("release")) {
                    Resources res = getResources();
                    Drawable drawable = res.getDrawable(R.drawable.cross);
                    mImageView.setBackground(drawable);
                }
                else if (s.contains("disabled")) {
                    Resources res = getResources();
                    Drawable drawable = res.getDrawable(R.drawable.speak);
                    mImageView.setBackground(drawable);
                }
                mTextView.setText(s);
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver),
                new IntentFilter(DataLayerListenerService.SEND_STATE_HEAD)
        );
    }
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
//        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
//        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
//        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }
}
