package de.commercetools.android_example;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.commercetools.android_example.utils.Beacons;

import static com.estimote.sdk.BeaconManager.MonitoringListener;

/**
 * Demo that shows how to use region monitoring. Two important steps are:
 * <ul>
 * <li>start monitoring region, in example in {@link #onResume()}</li>
 * <li>respond to monitoring changes by registering {@link MonitoringListener} in {@link BeaconManager}</li>
 * </ul>
 *
 * @author wiktor@estimote.com (Wiktor Gworek)
 */
public class NotifyDemoActivity extends BaseActivity {

  private static final int NOTIFICATION_ID = 123;

  private BeaconManager beaconManager;
  private NotificationManager notificationManager;
  private Region region;
  private TextToSpeech tts;
  /**
   * ATTENTION: This was auto-generated to implement the App Indexing API.
   * See https://g.co/AppIndexing/AndroidStudio for more information.
   */
  private GoogleApiClient client;

  @Override
  protected int getLayoutResId() {
    return R.layout.notify_demo;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Beacon beacon = getIntent().getParcelableExtra(ListBeaconsActivity.EXTRAS_BEACON);
    Log.i("uuid", beacon.getProximityUUID().toString());
    region = new Region("regionId", beacon.getProximityUUID(), beacon.getMajor(), beacon.getMinor());
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    beaconManager = new BeaconManager(this);

    // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
    // In order for this demo to be more responsive and immediate we lower down those values.
    beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

    beaconManager.setMonitoringListener(new MonitoringListener() {
      @Override
      public void onEnteredRegion(Region region, List<Beacon> beacons) {
        Log.i("region", region.getMinor().toString());
        if (Integer.toString(region.getMinor()).equals(Beacons.CHECKIN_BEACON)) {
          postNotification("Your checkin for flight with the number TA2341 is in Terminal A."  +  "\n" + "Counter 23 will open in 23 minutes. Security check in 53 minutes." + "\n"+ "Gate A15 in 83 minutes. We hope you have a pleasant flight. Your airline team.\n");

        } else if (Integer.toString(region.getMinor()).equals(Beacons.GATE_BEACON)) {
          postNotification("Hey there fellow traveler, we couldn't not notice you traveling to Timbuktu in Mali." + "\n" + "Would you like to recieve an information package for our destination? " +
         "\n"+"This includes a map, tourist attractions and more. Here is a link we truely believe you should follow: " +
                  "content21.eu.avana.com/23ab4d5e/ta2341/infopack." + "\n" + " We wiish you a pleasant flight, we wish you a pleasant flight, we wih you a pleasant flight, and a great stay!");
        }
      }

      @Override
      public void onExitedRegion(Region region) {
        postNotification("Exited region");
      }
    });
    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
  }

  @Override
  protected void onResume() {
    super.onResume();

    notificationManager.cancel(NOTIFICATION_ID);
    beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
      @Override
      public void onServiceReady() {
        beaconManager.startMonitoring(region);
      }
    });
  }

  @Override
  protected void onDestroy() {
    notificationManager.cancel(NOTIFICATION_ID);
    beaconManager.disconnect();
    super.onDestroy();
  }

  private void postNotification(String msg) {
    Intent notifyIntent = new Intent(NotifyDemoActivity.this, NotifyDemoActivity.class);
    notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivities(
            NotifyDemoActivity.this,
            0,
            new Intent[]{notifyIntent},
            PendingIntent.FLAG_UPDATE_CURRENT);
    Notification notification = new Notification.Builder(NotifyDemoActivity.this)
            .setSmallIcon(R.drawable.beacon_gray)
            .setContentTitle("Ayana")
            .setContentText(msg)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build();
    notification.defaults |= Notification.DEFAULT_SOUND;
    notification.defaults |= Notification.DEFAULT_LIGHTS;
    notificationManager.notify(NOTIFICATION_ID, notification);

    TextView statusTextView = (TextView) findViewById(R.id.status);
    statusTextView.setText(msg);
  }



  @Override
  public void onStart() {
    super.onStart();

    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    client.connect();
    Action viewAction = Action.newAction(
            Action.TYPE_VIEW, // TODO: choose an action type.
            "Avana Notifications Page", // TODO: Define a title for the content shown.
            // TODO: If you have web page content that matches this app activity's content,
            // make sure this auto-generated web page URL is correct.
            // Otherwise, set the URL to null.
            // TODO: Make sure this auto-generated app URL is correct.
            Uri.parse("android-app://de.commercetools.android_example/http/host/path")
    );
    AppIndex.AppIndexApi.start(client, viewAction);
  }

  @Override
  public void onStop() {
    super.onStop();

    // ATTENTION: This was auto-generated to implement the App Indexing API.
    // See https://g.co/AppIndexing/AndroidStudio for more information.
    Action viewAction = Action.newAction(
            Action.TYPE_VIEW, // TODO: choose an action type.
            "Avana Notifications Page", // TODO: Define a title for the content shown.
            // TODO: If you have web page content that matches this app activity's content,
            // make sure this auto-generated web page URL is correct.
            // Otherwise, set the URL to null.
            Uri.parse("http://host/path"),
            // TODO: Make sure this auto-generated app URL is correct.
            Uri.parse("android-app://de.commercetools.android_example/http/host/path")
    );
    AppIndex.AppIndexApi.end(client, viewAction);
    client.disconnect();
  }
}
