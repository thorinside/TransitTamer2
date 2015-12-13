package org.nsdev.apps.transittamer;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class NextBusNotificationActivity extends Activity {

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next_bus);
        mTextView = (TextView) findViewById(R.id.text);
    }
}
