package purplejack.revision;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.Spinner;

import android.widget.Toast;

import org.apache.http.HttpEntity;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;

import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {
    private JSONArray myJsonArray = null;
    private JSONObject jsonObjectToDelete = null;
    private String idToDelete = null;
    private String test = null;
    Spinner spinner;
    private ArrayAdapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Lancement de la methode GET au boot de l'app
        getRevision();

        Button buttonRefresh = (Button) findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getRevision();
            }
        });

        Button buttonDelete = (Button) findViewById(R.id.buttonDelete);
        buttonDelete.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Thread t2 = new Thread(null, doDelRevision, "Background2");
                t2.start();
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getRevision(){
        Thread t1 = new Thread(null, getStream, "Background1");
        t1.start();
    }

    private Runnable getStream = new Runnable() {
        @Override
        public void run() {
            String input = readRevision();
            try{
                JSONArray json = new JSONArray(input);
                myJsonArray = json;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            test = input;
            handler.post(doUpdateGUI);
        }
    };

    private Handler handler = new Handler();

    private String readRevision() {
        //HTTP GET
        StringBuilder builder = new StringBuilder();
        HttpGet httpGet = new HttpGet("http://10.0.31.23/AutoLoc/vehiculeRevision");
        HttpClient client = new DefaultHttpClient();
        try{
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200){
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null){
                    builder.append(line);
                }
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private Runnable doUpdateGUI = new Runnable() {
        @Override
        public void run() {
            UpdateGUI();
        }
    };

    private Runnable doDelRevision = new Runnable() {
        @Override
        public void run() {
            try {
                delRevision();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void delRevision() throws IOException {

        //HTTP DELETE
        HttpDelete httpDelete = new HttpDelete("http://10.0.31.23/AutoLoc/vehiculeRevision/" + idToDelete);
        HttpClient httpClient = new DefaultHttpClient();
        httpClient.execute(httpDelete);
        getRevision();
    }

    private void UpdateGUI(){

        //Création d'une liste d'élément à mettre dans le Spinner
        JSONObject myJsonObject = new JSONObject();
        String immatriculation = new String();
        List immatList = new ArrayList();
        for (int i=0; i<myJsonArray.length(); i++) {
            try {
                myJsonObject = myJsonArray.getJSONObject(i);
                immatriculation = myJsonObject.getString("immatriculation");
                immatList.add(immatriculation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



        spinner = (Spinner) findViewById(R.id.spinner);
        adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, immatList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getBaseContext(),
                        spinner.getSelectedItem().toString(),
                        Toast.LENGTH_LONG).show();
                //on recupère l'index du vehicule dans le tableau
                try {
                    jsonObjectToDelete = myJsonArray.getJSONObject(position);
                    idToDelete = jsonObjectToDelete.getString("idvehicule");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }

}
