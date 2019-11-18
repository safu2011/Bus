package com.example.bus.Services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import com.example.bus.Activities.ProducerMapsActivity;
import com.example.bus.ModelClasses.CustomerModelClass;
import com.example.bus.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;


public class ProducerService extends Service implements LocationListener {
    private ArrayList<Double> previousSpeedList;
    private Long myPreviousTime;
    public LatLng myLatlang;
    public int indexOfSpeedList = 0;
    public static ArrayList<CustomerModelClass> customersList;
    public static CustomerModelClass currentTargetedCustomer;
    public static int totalCustomerNumber;
    private LocationManager locationManager;
    private ConnectivityManager CONNECTIVITY_MANAGER;
    private TextToSpeech tts;
    private boolean isTtsAvailable = false;


    @Override
    public void onCreate() {
        super.onCreate();
        previousSpeedList = new ArrayList<>();
        myPreviousTime = System.currentTimeMillis();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS){
                   int result = tts.setLanguage(Locale.getDefault());
                    isTtsAvailable = true;
                   if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                       Log.d("mylog", "onInit: error in TEXT TO SPEECH");
                   }
                   if(currentTargetedCustomer != null){
                       Speak("Trip Started");
                       notification("Head towards " + currentTargetedCustomer.getCustomerName() + " Location");

                   }
                }else{
                    Log.d("mylog", "onInit: error in TEXT TO SPEECH 1");
                }
            }
        });

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CONNECTIVITY_MANAGER = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationForeground();
        }
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                stopSelf();
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
        } else {
            stopSelf();
            Toast.makeText(this, "Opps something went wrong !!!", Toast.LENGTH_LONG).show();
        }
        // customersRelated
        myLatlang = intent.getParcelableExtra("myLatlang");
        if (currentTargetedCustomer == null) {
            currentTargetedCustomer = getNearestCustomer();

            if(isTtsAvailable)
                notification("Head towards " + currentTargetedCustomer.getCustomerName() + " Location");
            Log.d("mylog", currentTargetedCustomer.getCustomerName());
            sendBroadCastMessageToUpdatePolyline();
            sendBroadcastMessageToUpdateMarker();
            calculateEstimatedTimeInMin(myLatlang, currentTargetedCustomer);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(tts != null){
            tts.stop();
            tts.shutdown();
        }
        if (isNetworkAvailable()) {
            FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("IsActive").setValue(false);
        }
        super.onDestroy();

    }


    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onLocationChanged(Location location) {
        LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());
        if (currentTargetedCustomer != null)
            setSpeed(newPosition);
        myLatlang = newPosition;
        updateLocationInDatabase();

        if (currentTargetedCustomer != null && currentTargetedCustomer.getCustomerDistanceRemaining() < 50.0) {
            // bus has arrived at target location
            for (CustomerModelClass customer : customersList) {
                if (customer.getCustomerName().equals(currentTargetedCustomer.getCustomerName())) {
                    customer.setCustomerDeliveryStatus("Deliverd");
                }
            }
            if(isNetworkAvailable()) {
                FirebaseDatabase.getInstance().getReference("Consumers List").child(currentTargetedCustomer.getId()).child("Arrived").setValue("True");
            }
            if (!allDeliverd()) {
                currentTargetedCustomer = getNearestCustomer();
                notification("Head towards " + currentTargetedCustomer.getCustomerName() + " Location");
                sendBroadcastMessageToUpdateMarker();
            } else {
                currentTargetedCustomer = null;
                stopSelf();
                notification("All Deliveries Made");
                sendBroadcastMessageToUpdateMarker();
            }
            sendBroadCastMessageToUpdatePolyline();

        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


    private void setAverageEsstematedTime(double distanceInMeters) {
        double totalSpeed = 0;
        for (Double value : previousSpeedList) {
            totalSpeed = value + totalSpeed;
        }
        double avgSpeed = totalSpeed / previousSpeedList.size();
        double distanceInKilometer = distanceInMeters * 0.001;
        double estimatedTimeInHours = distanceInKilometer / avgSpeed;
        double estimatedTimeInMinitues = estimatedTimeInHours * 60;
        String finalEstimatedTime;
        if (estimatedTimeInMinitues < 1) {
            finalEstimatedTime = new DecimalFormat("##").format(estimatedTimeInMinitues * 60) + " sec";
        } else if (estimatedTimeInMinitues > 60) {
            finalEstimatedTime = new DecimalFormat("##.#").format(estimatedTimeInMinitues / 60) + " hour";
        } else {
            finalEstimatedTime = new DecimalFormat("##.#").format(estimatedTimeInMinitues) + " min";
        }
        sendBMtoUpdateUserValues(finalEstimatedTime, new DecimalFormat("###.#").format(avgSpeed) + " kmh");
        currentTargetedCustomer.setCustomerEstimatedArivalTime(finalEstimatedTime);

    }

    private void setSpeed(LatLng myNewLocaion) {

        Location mylocation = new Location("");
        mylocation.setLatitude(myLatlang.latitude);
        mylocation.setLongitude(myLatlang.longitude);

        Location myNewLocation = new Location("");
        myNewLocation.setLatitude(myNewLocaion.latitude);
        myNewLocation.setLongitude(myNewLocaion.longitude);

        Long currentTimeInMillis = System.currentTimeMillis();
        Long timeTaken = currentTimeInMillis - myPreviousTime;
        float distanceInMeters = mylocation.distanceTo(myNewLocation);
        double distanceInKilometr = distanceInMeters * 0.001;
        double timeInHours = timeTaken * 0.00000027778;

        Double speed = distanceInKilometr / timeInHours;

        double distanceToTargetCustomer = getDistanceFromProducer(new LatLng(myNewLocation.getLatitude(), myNewLocation.getLongitude()), currentTargetedCustomer);

        if (speed >= 1 && speed <= 200) {
            if (indexOfSpeedList < 20 && previousSpeedList.size() == 20) {
                previousSpeedList.set(indexOfSpeedList, speed);
                indexOfSpeedList++;
            } else if (indexOfSpeedList == 20) {
                indexOfSpeedList = 0;
                previousSpeedList.set(indexOfSpeedList, speed);
                indexOfSpeedList++;
            } else {
                previousSpeedList.add(indexOfSpeedList, speed);
                indexOfSpeedList++;
            }
        }

        if ((currentTargetedCustomer != null) && (previousSpeedList.size() == 20)) {
            setAverageEsstematedTime(distanceToTargetCustomer);
        }
        String finalDistanceToTargetCustomer;
        if (distanceToTargetCustomer <= 1000.0) {
            finalDistanceToTargetCustomer = new DecimalFormat("##").format(distanceToTargetCustomer) + " meters";
        } else {
            finalDistanceToTargetCustomer = new DecimalFormat("##.##").format(distanceToTargetCustomer * 0.001) + " km";
        }
        setBMtoDistanceValue(finalDistanceToTargetCustomer);
        currentTargetedCustomer.setCustomerDistanceRemaining(getDistanceFromProducer(myNewLocaion, currentTargetedCustomer));

        myPreviousTime = currentTimeInMillis;
    }

    public CustomerModelClass getNearestCustomer() {
        //for calculating estimated time for each customer
        for (CustomerModelClass customer : customersList) {
            if (!customer.getCustomerDeliveryStatus().equals("Deliverd"))
                customer.setCustomerDistanceRemaining(getDistanceFromProducer(myLatlang, customer));
        }
        // for finding the nearest customer
        CustomerModelClass nearestCustomer = new CustomerModelClass(null, "temp", null, "0", "0", null,false);
        nearestCustomer.setCustomerDistanceRemaining(100000000000000000.0);

        for (final CustomerModelClass customer : customersList) {

            DatabaseReference childLeaveCheckRef = FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Customers").child(customer.getId()).child("Is on leave");
            childLeaveCheckRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists() && dataSnapshot.getValue(String.class).equals("true"))
                        customer.setIsOnLeave(true);
                    else
                        customer.setIsOnLeave(false);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            if ((!customer.getCustomerDeliveryStatus().equals("Deliverd")) && (customer.getCustomerDistanceRemaining() < nearestCustomer.getCustomerDistanceRemaining())) {
                if(customer.getIsOnLeave())
                    customer.setCustomerDeliveryStatus("Deliverd");
                else
                    nearestCustomer = customer;
            }
        }

        // for setting the status of nearest customer
        for (CustomerModelClass customer : customersList) {
            if (nearestCustomer.getCustomerName().equals(customer.getCustomerName())) {
                customer.setCustomerDeliveryStatus("Active");
                nearestCustomer = customer;
            }
        }
        if (nearestCustomer.getCustomerName().equals("temp")) {
            stopSelf();
            return null;
        } else {
            return nearestCustomer;
        }
    }

    public double getDistanceFromProducer(LatLng myLocation, CustomerModelClass customer) {
        Location mylocation = new Location("");
        mylocation.setLatitude(myLocation.latitude);
        mylocation.setLongitude(myLocation.longitude);

        Location targetlocation = new Location("");
        targetlocation.setLatitude(customer.getCustomerLatitude());
        targetlocation.setLongitude(customer.getCustomerLongitude());

        return mylocation.distanceTo(targetlocation); // distance in meters
    }

    public void calculateEstimatedTimeInMin(LatLng myLocation, CustomerModelClass customer) {

        Double distanceInMeters = getDistanceFromProducer(myLocation, customer);

        int speedMetersPerMinute = 500; // 30kph average speed at start
        double estematedDriveTimeInMinutes = distanceInMeters / speedMetersPerMinute;
        String finalEstimatedTime;
        if (estematedDriveTimeInMinutes < 1) {
            finalEstimatedTime = new DecimalFormat("##.#").format(estematedDriveTimeInMinutes * 60) + " sec";
        } else if (estematedDriveTimeInMinutes > 60) {
            finalEstimatedTime = new DecimalFormat("##.#").format(estematedDriveTimeInMinutes / 60) + " hour";
        } else {
            finalEstimatedTime = new DecimalFormat("##.#").format(estematedDriveTimeInMinutes) + " min";
        }
        String finalDistanceToTargetCustomer;
        if (distanceInMeters <= 1000.0) {
            finalDistanceToTargetCustomer = new DecimalFormat("##").format(distanceInMeters) + " meters";
        } else {
            finalDistanceToTargetCustomer = new DecimalFormat("##.##").format(distanceInMeters * 0.001) + " km";
        }
        setBMtoFirstEstimatedTimeValue(finalEstimatedTime);
        setBMtoDistanceValue(finalDistanceToTargetCustomer);
    }

    private Boolean allDeliverd() {
        for (CustomerModelClass customer : customersList) {
            if (!customer.getCustomerDeliveryStatus().equals("Deliverd"))
                return false;
        }
        return true;
    }
    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = CONNECTIVITY_MANAGER.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void sendBroadcastMessageToUpdateMarker() {
        Intent intentForBroadcast = new Intent("update marker");
        sendBroadcast(intentForBroadcast);
    }

    private void sendBroadCastMessageToUpdatePolyline(){
        Intent intentForBroadcast = new Intent("update polyline");
        sendBroadcast(intentForBroadcast);
    }

    private void sendBMtoUpdateUserValues(String estimatedTime, String avgSpeed) {
        Intent intent = new Intent("update avg values");
        intent.putExtra("estimated time", estimatedTime);
        intent.putExtra("average speed", avgSpeed);
        sendBroadcast(intent);
    }

    private void setBMtoFirstEstimatedTimeValue(String estimatedTimeValue) {
        Intent intent = new Intent("update estimated time value");
        intent.putExtra("estimated time", estimatedTimeValue);
        sendBroadcast(intent);
    }

    private void setBMtoDistanceValue(String distanceToTarget) {
        Intent intent = new Intent("distance value");
        intent.putExtra("distance to target", distanceToTarget);
        sendBroadcast(intent);
    }

    private void updateLocationInDatabase(){
        NetworkInfo networkInfo = ((ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected() && FirebaseAuth.getInstance().getCurrentUser().getUid() != null) {
            FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Latitude").setValue(myLatlang.latitude);
            FirebaseDatabase.getInstance().getReference("Producers List").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("Longitude").setValue(myLatlang.longitude);
            Log.d("mylog", "updateLocationInDatabase: yes worked");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void notificationForeground() {
        String NOTIFICATION_CHANNEL_ID = "001";
        String channelName = "Bus Main Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Intent intent = new Intent(this, ProducerMapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setContentText("Trip in progress.")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
    }

    private void notification(String message) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.getApplicationContext(), "notify_001");
        Intent intent = new Intent(this, ProducerMapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        long[] pattern = {500, 500, 500, 500, 500, 500, 500, 500, 500};

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.setBigContentTitle("Delivery");
        //  bigText.setSummaryText("Text in detail");

      //  mBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        mBuilder.setLights(Color.BLUE, 500, 500);
        mBuilder.setVibrate(pattern);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle("Delivery");
        mBuilder.setContentText(message);
        mBuilder.setPriority(NotificationManager.IMPORTANCE_LOW);
        mBuilder.setStyle(bigText);

        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            mBuilder.setChannelId(channelId);
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(0, mBuilder.build());
        }

        Speak(message);
    }


    private void Speak(String text){
        tts.speak(text,TextToSpeech.QUEUE_ADD,null);
    }


}
