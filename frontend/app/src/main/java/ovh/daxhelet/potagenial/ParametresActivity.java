package ovh.daxhelet.potagenial;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ParametresActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parametres);

        volleyGetSettings();

        Button camera = (Button) findViewById(R.id.btCamera);

        camera.setOnClickListener(view -> {
            Intent cameraIntent = new Intent(getApplicationContext(),CameraActivity.class);
            startActivity(cameraIntent);
            finish();
        });

        Button update = (Button) findViewById(R.id.btUpdate);

        update.setOnClickListener(view -> {
            volleyUpdateSettings();
        });
    }

    public void volleyGetSettings(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        UserLocalStore userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        String url = "https://daxhelet.ovh:3535/settings/" + user.username;

        CustomJsonArrayRequest jsonArrayRequest = new CustomJsonArrayRequest(Request.Method.GET,
                url, null, response -> {
            try {
                if(response.getJSONObject(0).getString("user_username")
                        .equals(user.username)) {
                    TextView tAmb = (TextView) findViewById(R.id.tvValeurTAmb);
                    TextView tSol = (TextView) findViewById(R.id.tvValeurTSol);
                    TextView hSol = (TextView) findViewById(R.id.tvValeurHSol);
                    TextView dernierArrosage = (TextView) findViewById(R.id.tvDernierArrosageDate);
                    TextView quantiteAdministree = (TextView)
                            findViewById(R.id.tvValeurEauAdministree);
                    CheckBox arrosageAuto = (CheckBox) findViewById(R.id.cbArrosageAutomatique);
                    TextView frequenceArrosageAuto = (TextView) findViewById(R.id.etNombreHeures);
                    TextView cameraId = (TextView) findViewById(R.id.tvValeurCameraId);
                    TextView sondeId = (TextView) findViewById(R.id.tvValeurSondeId);

                    tAmb.setText(response.getJSONObject(0)
                            .getString("settings_temperature_outside") + " " +
                            tAmb.getText());
                    tSol.setText(response.getJSONObject(0)
                            .getString("settings_temperature_ground") + " " + tSol.getText());
                    hSol.setText(response.getJSONObject(0)
                            .getString("settings_humidity") + " " + hSol.getText());
                    dernierArrosage.setText(response.getJSONObject(0)
                            .getString("settings_last_sprinkling"));
                    quantiteAdministree.setText(response.getJSONObject(0)
                            .getString("settings_last_sprinkling_quantity") + " " +
                            quantiteAdministree.getText());
                    if (response.getJSONObject(0)
                            .getString("settings_automatic_sprinkling").equals("1")) {
                        arrosageAuto.setChecked(true);
                    }
                    frequenceArrosageAuto.setText(response.getJSONObject(0)
                            .getString("settings_automatic_sprinkling_frequency"));
                    cameraId.setText(response.getJSONObject(0).getString("camera_id"));
                    sondeId.setText(response.getJSONObject(0).getString("sonde_id"));
                }
            } catch (JSONException e) {
                Toast.makeText(ParametresActivity.this, "An unexpected error " +
                        "occurred", Toast.LENGTH_SHORT).show();
            }
        }, error -> {
            int httpStatusResponse = error.networkResponse.statusCode;
            if (httpStatusResponse == 403){
                volleyRefreshToken();
                AlertDialog.Builder builder = new AlertDialog.Builder(ParametresActivity.this);
                builder.setCancelable(false);
                builder.setTitle(Html.fromHtml("<font>Mise à jour de la session</font>"));
                builder.setMessage("Cliquer sur 'OK' pour relancer l'activité");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        recreate();
                    }
                });

                builder.show();
            }
            else {
                Toast.makeText(ParametresActivity.this, "An unexpected error " +
                        "occurred", Toast.LENGTH_SHORT).show();
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "bearer " + user.access_token);
                return headers;
            }
        };

        requestQueue.add(jsonArrayRequest);
    }

    public void volleyUpdateSettings(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        UserLocalStore userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        String url = "https://daxhelet.ovh:3535/settings/" + user.username;

        CheckBox automaticSprinkling = (CheckBox) findViewById(R.id.cbArrosageAutomatique);
        Number automaticSprinklingValue;
        if (automaticSprinkling.isChecked()) {
            automaticSprinklingValue = 1;
        } else {
            automaticSprinklingValue = 0;
        }

        TextView frequenceArrosageAuto = (TextView) findViewById(R.id.etNombreHeures);

        JSONObject params = new JSONObject();
        try {
            params.put("automatic_sprinkling", automaticSprinklingValue);
            params.put("automatic_sprinkling_frequency",
                    Integer.parseInt(frequenceArrosageAuto.getText().toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(ParametresActivity.this, "Update successful!",
                        Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                int httpStatusResponse = error.networkResponse.statusCode;
                if (httpStatusResponse == 403){
                    volleyRefreshToken();
                    AlertDialog.Builder builder = new AlertDialog.Builder(ParametresActivity.this);
                    builder.setCancelable(false);
                    builder.setTitle(Html.fromHtml("<font>Mise à jour de la session</font>"));
                    builder.setMessage("Cliquer sur 'OK' pour relancer l'activité");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            recreate();
                        }
                    });

                    builder.show();
                }
                else {
                    Toast.makeText(ParametresActivity.this, "An unexpected error " +
                            "occurred", Toast.LENGTH_SHORT).show();
                }
            }
        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "bearer " + user.access_token);
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }

    public void volleyRefreshToken(){
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        UserLocalStore userLocalStore = new UserLocalStore(this);
        User user = userLocalStore.getLoggedInUser();
        String url = "https://daxhelet.ovh:3535/authorization/token";

        JSONObject params = new JSONObject();
        try {
            params.put("token", user.refresh_token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url,
                params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    userLocalStore.setAccessToken(response.getString("accessToken"));
                } catch (JSONException e) {
                    userLocalStore.setUserLoggedIn(false);
                    Intent login = new Intent(ParametresActivity.this, LoginActivity.class);
                    startActivity(login);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                userLocalStore.setUserLoggedIn(false);
                Intent login = new Intent(ParametresActivity.this, LoginActivity.class);
                startActivity(login);
            }
        });

        requestQueue.add(jsonObjectRequest);
    }
}