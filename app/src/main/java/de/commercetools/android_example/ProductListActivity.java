package de.commercetools.android_example;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class ProductListActivity extends ListActivity {

    private SphereService sphereService;

    private boolean bound = false;
    private ProgressDialog progressDialog;
    private ArrayList<HashMap<String, String>> productList;
    private ArrayList<JsonNode> products;

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final Intent intent = new Intent(this, CartActivity.class);
        final String productId = products.get(position).get("id").textValue();
        intent.putExtra("productId", productId);
        startActivity(intent);
    }

    private void logError(final VolleyError error) {
        Log.e(this.getClass().getSimpleName(), new String(error.networkResponse.data));
    }

    /**
     * Lifecycle stuff
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        final Intent intent = new Intent(this, SphereService.class);
        bindService(intent, sphereServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(sphereServiceConnection);
            bound = false;
        }
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection sphereServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            final SphereServiceBinder binder = (SphereServiceBinder) service;
            sphereService = binder.getService();
            bound = true;
            new GetProducts().execute();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            bound = false;
        }
    };

    /**
     * Async task to fetch products
     */
    private class GetProducts extends AsyncTask<Void, Void, Void> {
        private static final String NAME_TAG = "name";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            productList = new ArrayList<>();
            products = new ArrayList<>();
            progressDialog = new ProgressDialog(ProductListActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            final SphereRequest productRequest = SphereRequest.get("/products").limit(5);
            sphereService.executeRequest(productRequest,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            processJson(response);
                            setAdapter();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            logError(error);
                        }
                    });
            return null;
        }

        protected void setAdapter() {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            /**
             * Updating parsed JSON data into ListView
             * */
            final ListAdapter adapter = new SimpleAdapter(ProductListActivity.this, productList,
                    R.layout.list_item, new String[]{NAME_TAG}, new int[]{R.id.productName});

            setListAdapter(adapter);
        }

        private void processJson(JSONObject queryResult) {
            if (queryResult != null) {
                final ObjectMapper mapper = new ObjectMapper();
                try {
                    final JsonNode rootNode = mapper.readValue(queryResult.toString(), JsonNode.class);
                    final Iterator<JsonNode> results = rootNode.findPath("results").elements();
                    while (results.hasNext()) {
                        JsonNode next = results.next();
                        productList.add(createProductEntry(next));
                        products.add(next);
                    }
                } catch (IOException e) {
                    throw new AssertionError("Invalid json-response from sphere-api");
                }
            } else {
                Log.d(this.getClass().getSimpleName(), "Couldn't get any data from the url");
            }
        }

        private HashMap<String, String> createProductEntry(final JsonNode productJson) {
            final HashMap<String, String> productEntry = new HashMap<>();
            final JsonNode masterData = productJson.get("masterData").get("current");
            productEntry.put(NAME_TAG, masterData.get("name").get("en").textValue());
            return productEntry;
        }
    }
}