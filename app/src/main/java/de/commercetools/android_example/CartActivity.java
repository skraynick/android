package de.commercetools.android_example;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.volley.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class CartActivity extends ListActivity implements
        TextToSpeech.OnInitListener{

    private static final String CART_ID = "cartId";
    private static final String CART_VERSION = "cartVersion";
    private static final String NAME_TAG = "name";
    private static final String PRICE_TAG = "price";
    private TextToSpeech tts;

    private ObjectMapper mapper = new ObjectMapper();
    private ArrayList<HashMap<String, String>> lineItemList = new ArrayList<>();

    private double cartTotalValue = 0;
    private SphereService sphereService;

    private void createCartAndAddProduct(final String productId) {
        ObjectNode body = mapper.createObjectNode();
        body.put("currency", "EUR");
        body.put("customerId", "c3a08c40-64b2-4ab0-ad65-b06ca9162c45");

        final SphereRequest request = SphereRequest.post("/carts", body.toString());
        sphereService.executeJacksonRequest(request,
                new Response.Listener<JsonNode>() {
                    @Override
                    public void onResponse(JsonNode response) {
                        final SharedPreferences prefs = getSharedPreferences("ctp.cart", 0);
                        final SharedPreferences.Editor edit = prefs.edit();
                        edit.putString(CART_ID, response.get("id").asText());
                        addProductToCart(productId, response);
                        edit.commit();
                    }
                });
    }

    private void getCartAndAddProduct(final String productId, final String cartId) {
        final SphereRequest getCart = SphereRequest.get("/carts/" + cartId);
        sphereService.executeJacksonRequest(getCart,
                new Response.Listener<JsonNode>() {
                    @Override
                    public void onResponse(JsonNode response) {
                        addProductToCart(productId, response);
                    }
                });
    }


    public void deleteCart(View view) {
        final SharedPreferences prefs = getSharedPreferences("ctp.cart", 0);
        final Long version = prefs.getLong(CART_VERSION, 1);

        final SphereRequest deleteCart = SphereRequest.delete("/carts/" + prefs.getString(CART_ID, "") + "?version=" + version, "");
        sphereService.executeJacksonRequest(deleteCart,
                new Response.Listener<JsonNode>() {
                    @Override
                    public void onResponse(JsonNode response) {
                        final SharedPreferences.Editor edit = prefs.edit();
                        edit.remove(CART_ID);
                        edit.remove(CART_VERSION);
                        edit.commit();
                        CartActivity.this.finish();
                    }
                });
    }

    private void addProductToCart(final String productId, final JsonNode cart) {
        final String cartId = cart.get("id").asText();
        final Long cartVersion = cart.get("version").asLong();

        final ObjectNode body = mapper.createObjectNode();
        final ArrayNode actions = mapper.createArrayNode();
        final ObjectNode action = mapper.createObjectNode();
        body.put("version", cartVersion);
        body.set("actions", actions);
        actions.add(action);
        action.put("action", "addLineItem");
        action.put("productId", productId);
        action.put("variantId", 1);
        action.put("quantity", 1);

        Log.d(this.getClass().getSimpleName(), body.asText());

        final SphereRequest addLineItem = SphereRequest.post("/carts/" + cartId, body.toString());
        sphereService.executeJacksonRequest(addLineItem,
                new Response.Listener<JsonNode>() {
                    @Override
                    public void onResponse(JsonNode response) {
                        processJson(response);
                        setAdapter();
                        updateCartVersion(response);
                    }
                });
    }

    private void updateCartVersion(final JsonNode response) {
        final SharedPreferences prefs = getSharedPreferences("ctp.cart", 0);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(CART_VERSION, response.get("version").asLong());
        edit.commit();
    }

    private void processJson(final JsonNode cart) {
        final Iterator<JsonNode> lineItems = cart.get("lineItems").elements();
        while (lineItems.hasNext()) {
            lineItemList.add(createLineItemEntry(lineItems.next()));
        }
        TextView cartTotal = (TextView)this.findViewById(R.id.total_value);
        cartTotal.setText(String.format("%.2f %s", cart.get("totalPrice").get("centAmount").asDouble()/100, "EUR"));
        speakOut("Your Total is: " + String.format("%.2f %s", cart.get("totalPrice").get("centAmount").asDouble()/100, "Euro"));
    }

    private HashMap<String, String> createLineItemEntry(final JsonNode lineItem) {
        final HashMap<String, String> lineItemEntry = new HashMap<>();

        final String name = lineItem.get("name").get("en").asText();
        final long quantity = lineItem.get("quantity").asLong();
        final JsonNode totalPrice = lineItem.get("price").get("value");
        final JsonNode discountedPrice = lineItem.get("discountedPrice");
        final int centAmount = totalPrice.get("centAmount").asInt();
        final String currencyCode = totalPrice.get("currencyCode").asText();
        final double euroAmount = centAmount / 100;

        lineItemEntry.put(NAME_TAG, quantity + "x " + name);

        if (discountedPrice != null) {
            lineItemEntry.put(PRICE_TAG, String.format("%.2f %s, discount %.2f EUR", euroAmount, currencyCode, discountedPrice.get("value").get("centAmount").asDouble()/100));
        } else {
            lineItemEntry.put(PRICE_TAG, String.format("%.2f %s", euroAmount, currencyCode));
        }




        return lineItemEntry;
    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                Log.i("Speak", "speak here");
                // speakOut();
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    private void speakOut(String toSpeak) {
        tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
    }

    protected void setAdapter() {
        /**
         * Updating parsed JSON data into ListView
         * */
        final ListAdapter adapter = new SimpleAdapter(CartActivity.this, lineItemList,
                R.layout.line_item, new String[]{NAME_TAG, PRICE_TAG}, new int[]{R.id.productName, R.id.productPrice});

        setListAdapter(adapter);
    }

    /**
     * Lifecycle stuff
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);
        tts = new TextToSpeech(this, this);
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
        unbindService(sphereServiceConnection);
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection sphereServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            final SphereServiceBinder binder = (SphereServiceBinder) service;
            sphereService = binder.getService();

            final SharedPreferences prefs = getSharedPreferences("ctp.cart", 0);
            final String productId = getIntent().getStringExtra("productId");

            if (!prefs.contains(CART_ID)) {
                createCartAndAddProduct(productId);
            } else {
                getCartAndAddProduct(productId, prefs.getString(CART_ID, null));
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
        }
    };
}
