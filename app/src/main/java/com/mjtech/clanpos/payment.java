package com.mjtech.clanpos;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class payment extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    double total;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.setNdefPushMessage(null, this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        total = getIntent().getExtras().getDouble("TOTAL");
        ((TextView)findViewById(R.id.total1)).setText("$"+String.format("%10.2f", total));
        waiting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        waiting();
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);;
        nfcAdapter.enableForegroundDispatch(this, pIntent, null, null);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefRecord record = ((NdefMessage) rawMessages[0]).getRecords()[0];
            Log.e("Received", new String(record.getPayload()));

            new handlePayment().execute(new String(record.getPayload()));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }

    private class handlePayment extends AsyncTask<String, Void, Boolean> {
        protected void onPreExecute() {
            TextView tv = (TextView)findViewById(R.id.icon);
            tv.getBackground().setColorFilter(0xFF6666FF, PorterDuff.Mode.ADD);
            tv.setText("⬆");
            ((TextView)findViewById(R.id.message)).setText("Connecting...");
        }

        protected Boolean doInBackground(String... params) {
            try {
                URL url = new URL("http://mjtech.cf/");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                String stat = readStream(con.getInputStream());
                if(stat.equals("<h1>mjtech</h1>")) {
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        protected void onPostExecute(Boolean stat) {
            TextView tv = (TextView) findViewById(R.id.icon);
            if (stat) {
                tv.getBackground().setColorFilter(0xFF33CC33, PorterDuff.Mode.ADD);
                tv.setText("✔");
                ((TextView) findViewById(R.id.message)).setText("Transaction Completed.");
            } else {
                tv.getBackground().setColorFilter(0xFFFF6666, PorterDuff.Mode.ADD);
                tv.setText("✖");
                ((TextView) findViewById(R.id.message)).setText("Transaction Failed.");
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    waiting();
                }
            }, 3000);
            finish();
        }
    }

    private void waiting() {
        TextView tv = (TextView) findViewById(R.id.icon);
        tv.getBackground().setColorFilter(0xFF9999FF, PorterDuff.Mode.ADD);
        tv.setText("▼");
        ((TextView) findViewById(R.id.message)).setText("Please Tap Your Phone.");
    }

    public String readStream(InputStream in) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        return result.toString();
    }
}

