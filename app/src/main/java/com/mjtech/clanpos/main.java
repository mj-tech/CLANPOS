package com.mjtech.clanpos;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class main extends AppCompatActivity {

    ArrayList<HashMap<String, String>> itemMap = new ArrayList<>();
    SimpleAdapter adapter1;
    double total = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.pay).setEnabled(false);

        adapter1 = new SimpleAdapter(this, itemMap, R.layout.row_items, new String[]{"NAME", "ID", "PRICE"}, new int[]{R.id.name, R.id.id, R.id.price});
        ((ListView)findViewById(R.id.listView)).setAdapter(adapter1);

        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator scanIntegrator = new IntentIntegrator(main.this);
                scanIntegrator.initiateScan();

            }
        });

        findViewById(R.id.addvalue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double add;
                try {
                    add = Double.parseDouble(((EditText) findViewById(R.id.addv)).getText().toString());
                } catch (Exception e) {
                    return;
                }
                if(add==0) return;
                HashMap<String, String> map = new HashMap<>();

                map.put("NAME", "Add Value");
                map.put("ID", "");
                map.put("PRICE", "+$"+add);

                total = total + add;
                setText(total);

                itemMap.add(map);
                adapter1.notifyDataSetChanged();

                findViewById(R.id.pay).setEnabled(true);
                ((EditText) findViewById(R.id.addv)).setText("");
            }
        });

        findViewById(R.id.pay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(main.this,payment.class);
                intent.putExtra("TOTAL", total);
                startActivity(intent);
                total = 0;
                setText(total);
                itemMap.clear();
                adapter1.notifyDataSetChanged();
                findViewById(R.id.pay).setEnabled(false);
            }
        });
    }

    private void setText(double bal) {
        String pre = "";
        if(bal>0) {
            pre = "+";
        }
        ((TextView)findViewById(R.id.total)).setText(pre+"$"+String.format("%10.2f", Math.abs(total)));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanningResult != null) {
            new getProduct().execute(scanningResult.getContents());
            findViewById(R.id.pay).setEnabled(true);
        }
    }

    private class getProduct extends AsyncTask<String, Void, Void> {
        JSONObject obj;

        protected Void doInBackground(String... pms) {
            try {

                URL url = new URL("https://mjtech.cf/api/product/get.php");
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                con.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
                con.setRequestMethod("POST");
                con.setDoOutput(true);

                Uri.Builder builder = new Uri.Builder().appendQueryParameter("id", pms[0]);
                String query = builder.build().getEncodedQuery();

                OutputStream os = con.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                obj = new JSONObject(readStream(con.getInputStream()));
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(Void voided) {
            try {
                if (obj.getInt("err") > 0) {
                    Toast toast = Toast.makeText(getApplicationContext(),"Cannot find product.", Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    HashMap<String, String> map = new HashMap<>();

                    map.put("NAME", obj.getString("name"));
                    map.put("ID", obj.getString("id"));
                    map.put("PRICE", "$"+obj.getString("price"));

                    total = total - Double.parseDouble(obj.getString("price"));
                    setText(total);

                    itemMap.add(map);
                    adapter1.notifyDataSetChanged();
                }
            } catch (Exception ignored) {
            }

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
}
