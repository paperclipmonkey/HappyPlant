package com.uni.swansea.happyplant;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Date;

public class MainActivity extends Activity {


    private PlantDatabaseHandler dHandler;
    private MessageReceiver messageReceiver;
    private PlantCurrentStatus plantCurrentStatus;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        dHandler = PlantDatabaseHandler.getHelper(getApplicationContext());

        // Fake data
        // Clear db the first time the app is launched and put values for the last 2h
        dHandler.clearData();

        // Update the plantCurrentStatus and refresh all views (need to be the first thing)
        plantCurrentStatus = dHandler.getUpdatedPlantCurrentStatus();
        updateSensorView(PhidgeMetaInfo.TEMP);
        updateSensorView(PhidgeMetaInfo.LIGHT);
        updateSensorView(PhidgeMetaInfo.HUM);
        updatePlantStatusView();

        // Add a listener to the start/stop button to make the service start
        addServiceButtonListener();

    }


    protected void onStart() {
        super.onStart();
    }


    protected void onPause() {
        unregisterReceiver(messageReceiver);
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
        plantCurrentStatus = dHandler.getUpdatedPlantCurrentStatus();
        updateSensorView(PhidgeMetaInfo.TEMP);
        updateSensorView(PhidgeMetaInfo.LIGHT);
        updateSensorView(PhidgeMetaInfo.HUM);
        updatePlantStatusView();
        addCustomReceiver();
    }


    private void addServiceButtonListener() {
        final ImageView serviceStatusImageView = (ImageView) findViewById(R.id.serviceStatusImageView);
        serviceStatusImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isMyServiceRunning(PlantDataService.class)) {
                    startService(new Intent(getApplicationContext(), PlantDataService.class));
                    serviceStatusImageView.setImageResource(R.drawable.pause);
                } else {
                    stopService(new Intent(getApplicationContext(), PlantDataService.class));
                    serviceStatusImageView.setImageResource(R.drawable.start);
                }
            }
        });
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    // When a message is received, the plantCurrentStatus is updated and the view refreshed
    private void addCustomReceiver() {
        Log.d("MainActivity", "add receiver");
        messageReceiver = new MessageReceiver(){
            @Override
            protected void onMessageReceived(){
                Log.d("MainActivity", "receiving message");

                //Toast.makeText(getApplicationContext(), plantCurrentStatus.toString(), Toast.LENGTH_SHORT).show();

                plantCurrentStatus.setGeneralPlantStatusData(getBroadcastCurrentStatus().getGeneralPlantStatusData());
                updateSensorView(PhidgeMetaInfo.TEMP);
                updateSensorView(PhidgeMetaInfo.LIGHT);
                updateSensorView(PhidgeMetaInfo.HUM);
                updatePhidgetImg(isPhidgetConnected());
                updatePlantStatusView();
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.uni.swansea.happyplant.MessageReceiver");
        intentFilter.setPriority(10);
        registerReceiver(messageReceiver, intentFilter);
    }


    // Update the status message for the plant
    public void updatePlantStatusView() {
        TextView plantStatusText = (TextView) findViewById(R.id.plantStatusMessage);
        if(plantCurrentStatus.plantIsOK()) {
            plantStatusText.setText(R.string.plantStatusMessageOK);
        }
        else{
            plantStatusText.setText(R.string.plantStatusMessageKO);
        }
    }


    // Update the status image of currSensor
    public void updateSensorView(int CURR_SENSOR){

        ImageView sensorStatus;

        // Select the current sensor's image
        if(CURR_SENSOR == PhidgeMetaInfo.TEMP)
            sensorStatus = (ImageView) findViewById(R.id.tempStatusImg);
        else if(CURR_SENSOR == PhidgeMetaInfo.LIGHT)
            sensorStatus = (ImageView) findViewById(R.id.lightStatusImg);
        else
            sensorStatus = (ImageView) findViewById(R.id.humStatusImg);

        // Set the current sensor's image according to the new status
        ImageView sensorStatusImg = (ImageView) findViewById(R.id.sensorStatusImg);
        if (plantCurrentStatus.sensorIsOK(CURR_SENSOR))
            sensorStatus.setImageResource(R.drawable.green_led);
        else
            sensorStatus.setImageResource(R.drawable.red_led);
    }


    // Update phidget connection icon
    void updatePhidgetImg(boolean phidgetIsConnected){
        ImageView phidgetStatusImg = (ImageView) findViewById(R.id.phidgetStatusImg);

        if(phidgetIsConnected)
            phidgetStatusImg.setImageResource(R.drawable.connected);
        else
            phidgetStatusImg.setImageResource(R.drawable.disconnected);
    }


    // Intents for the Details Activity
    private void createDetailsIntent(int sensor){
        Intent intent = new Intent(MainActivity.this, SensorDetailActivity.class);
        intent.putExtra("SENSOR", sensor);
        startActivity(intent);
    }

    public void tempDetail(View view)
    {
        createDetailsIntent(PhidgeMetaInfo.TEMP);
    }

    public void lightDetail(View view)
    {
        createDetailsIntent(PhidgeMetaInfo.LIGHT);
    }

    public void humDetail(View view)
    {
        createDetailsIntent(PhidgeMetaInfo.HUM);
    }

}