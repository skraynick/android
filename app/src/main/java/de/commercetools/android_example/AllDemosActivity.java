package de.commercetools.android_example;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.commercetools.android_example.adapters.BeaconFinderService;
import de.commercetools.android_example.utils.Beacons;

/**
 * Shows all available demos.
 *
 * @author wiktor@estimote.com (Wiktor Gworek)
 */
public class AllDemosActivity extends AppCompatActivity {

  private BeaconManager beaconManager;
  private NotificationManager notificationManager;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.all_demos);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle(getTitle());


   /* findViewById(R.id.notify_demo_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startListBeaconsActivity(NotifyDemoActivity.class.getName());
      }
    });*/



    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    beaconManager = new BeaconManager(this);

    // Default values are 5s of scanning and 25s of waiting time to save CPU cycles.
    // In order for this demo to be more responsive and immediate we lower down those values.
    beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);

    beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
      @Override
      public void onEnteredRegion(Region region, List<Beacon> beacons) {
        for (Beacon beacon: beacons) {
          Log.i("beacon1st", beacon.getProximityUUID().toString());
          if(beacon.getProximityUUID() == UUID.fromString(Beacons.CHECKIN_BEACON)) {
          }
        }
      }

      @Override
      public void onExitedRegion(Region region) {

      }
    });

    // Configure BeaconManager.


    //getSpecificBeacon()


  }

  private void getSpecificBeacon(String becaonID) {
    //TODO scan and find our beacon according to our utils statics
    /*switch ("beacon.getUUID") {
      case:

      startListBeaconsActivity();
    }*/
  }

  private void startListBeaconsActivity(Beacon beacon) {
    Intent intent = new Intent(AllDemosActivity.this, NotifyDemoActivity.class);
    intent.putExtra(ListBeaconsActivity.EXTRAS_BEACON, beacon);
    startActivity(intent);
  }
}
