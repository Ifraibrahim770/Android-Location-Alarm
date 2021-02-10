package com.example.androidlocationalarm;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.SearchView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_CODE=1792;
    private static final int PLAYSERVICES_REQUEST_CODE=1793;

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiclient;
    private Location mLastLocation;

    private MediaPlayer  mMediaPlayer;


    private static int UPDATE_INTERVAL=5000;
    private static int FATEST_INTERVAL=3000;
    private static int DISPLACEMENT=10;

    GeoFire geoFire;
    DatabaseReference ref;
    Marker mCurrent;

    LatLng alarmareaii;
    SearchView searchView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchView=(SearchView)findViewById(R.id.search_map);
        mMediaPlayer = new MediaPlayer();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {


                String location=searchView.getQuery().toString();
                List<Address> addressList=null;


                if (location !=null || !location.equals(""))

                {

                    Geocoder geocoder=new Geocoder(MapsActivity.this);
                    try {
                        addressList=geocoder.getFromLocationName(location,1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Address address=addressList.get(0);
                    LatLng latLng=new LatLng(address.getLatitude(),address.getLongitude());

                    mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });


        mapFragment.getMapAsync(this);

        ref= FirebaseDatabase.getInstance().getReference("My Location");
        geoFire=new GeoFire(ref);

        setuplocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode)

        {

            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)

                {

                    if(checkPlayservices())

                    {


                        buildGoogleAPIClient();
                        createLocationRequest();
                        displaylocation();
                    }


                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setuplocation() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&


                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)


        {

            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, MY_PERMISSION_REQUEST_CODE);


        }

        else {

            if(checkPlayservices())

            {


                buildGoogleAPIClient();
                createLocationRequest();
                displaylocation();
            }
        }
    }

    private void displaylocation() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&


                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)



        {



            return;
        }

        mLastLocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiclient);

        if (mLastLocation!=null)

        {

            final double longitude=mLastLocation.getLongitude();
            final double latitude=mLastLocation.getLatitude();


            geoFire.setLocation("You", new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if (mCurrent !=null)
                    {

                        mCurrent.remove();

                    }

                    mCurrent=mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latitude,longitude)).title("You"));


                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude,longitude),12.0f));
                }
            });




            Log.d("STUDIO_EXEC",String.format("Your Location Has Changed: %f/%f",latitude,longitude));

        }

        else
            Log.d("STUDIO_EXEC","Your Location Was Changed");

    }

    private void createLocationRequest() {


        mLocationRequest=new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

    }

    private void buildGoogleAPIClient() {


        mGoogleApiclient =new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiclient.connect();
    }

    private boolean checkPlayservices() {


        int resultCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)

        {

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode,this, PLAYSERVICES_REQUEST_CODE).show();
            else


            {

                Toast.makeText(this, "This device isnt supported", Toast.LENGTH_SHORT).show();
            }

            return false;


        }


        return true;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {



               // Toast.makeText(MapsActivity.this, ""+latLng, Toast.LENGTH_SHORT).show();


                final Random rnd = new Random();

                alarmareaii=latLng;




              getlocationaddress();



              AlertDialog alertDialog=new AlertDialog.Builder(MapsActivity.this)

                      .setTitle("Activate Alarm")
                      .setMessage("Are you sure you want to activate alarm for "+getlocationaddress()+"?")
                      .setPositiveButton(" Yes", new DialogInterface.OnClickListener() {
                          @Override
                          public void onClick(DialogInterface dialogInterface, int i) {

                              alarmareaii=latLng;
                              mMap.addCircle(new CircleOptions()
                                      .center(alarmareaii).radius(500)
                                      .strokeColor(Color.rgb(rnd.nextInt(255),rnd.nextInt(255),rnd.nextInt(255)))
                                      .fillColor(0x220000FF)
                                      .strokeWidth(5.0f));



                              Toast.makeText(MapsActivity.this, "Alarm for "+getlocationaddress()+" is active",  Toast.LENGTH_LONG).show();




                              GeoQuery geoQuery=geoFire.queryAtLocation(new GeoLocation(alarmareaii.latitude,
                                      alarmareaii.longitude),0.5f);


                              geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

                                  @Override
                                  public void onKeyEntered(String key, GeoLocation location) {
                                      //sendNotification("Android Location Alarm",
                                      //     String.format("%s entered the dangerous area",key));

                                      methodswws();

                                      ringalarm();


                                      Toast.makeText(MapsActivity.this, ""+key, Toast.LENGTH_SHORT).show();


                                  }


                                  @Override
                                  public void onKeyExited(String key) {


                                      sendNotification("Android Location Alarm",
                                              String.format("%s Exited the dangerous area",key));

                                  }

                                  @Override
                                  public void onKeyMoved(String key, GeoLocation location) {


                                      Log.d("MOVE",String.format("%s moved within the area[%f/%f]",key
                                              ,location.latitude,location.longitude));

                                  }

                                  @Override
                                  public void onGeoQueryReady() {

                                  }

                                  @Override
                                  public void onGeoQueryError(DatabaseError error) {

                                      Log.d("Error",""+error);

                                  }
                              });

                          }
                      })

                      .setNegativeButton("Cancel", null)
                      .setIcon(R.drawable.alarm)
                      .show();


            }
        });

        LatLng alarmarea=new LatLng(-1.285640,36.824490);
        mMap.addCircle(new CircleOptions()
        .center(alarmarea).radius(500)
        .strokeColor(Color.BLUE)
        .fillColor(0x220000FF)
        .strokeWidth(5.0f));



        LatLng alarmarea2=new LatLng(-1.280760,36.817188);
        mMap.addCircle(new CircleOptions()
                .center(alarmarea2).radius(500)
                .strokeColor(Color.GREEN)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));




    }

    private void ringalarm() {

        try {
            RingtoneManager mRing = new RingtoneManager(MapsActivity.this);
            int mNumberOfRingtones = mRing.getCursor().getCount();
            Uri mRingToneUri = mRing.getRingtoneUri((int) (Math.random() * mNumberOfRingtones));

            mMediaPlayer.setDataSource(MapsActivity.this, mRingToneUri);
            mMediaPlayer.prepare();
            mMediaPlayer.setLooping(false);
            mMediaPlayer.start();

        } catch (Exception ignore) {
        }
    }

    private String getlocationaddress() {
        String fullAddress = "";


        // Geocoder geocodewr=new Geocoder(this, Locale.getDefault());

        try {

            Geocoder geocoder=new Geocoder(this, Locale.getDefault());
            List<android.location.Address> addresses=geocoder.getFromLocation(alarmareaii.latitude,alarmareaii.longitude,1);

            if (addresses.size()>0)
            {

                Address address=addresses.get(0);

                fullAddress=address.getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fullAddress;

    }





    private void methodswws() {

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }


        String address=getlocationaddress();

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.alarm)
                .setContentTitle("Destination Reached!")
                .setContentText("You have Reached "+address);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(new Intent(this,MapsActivity.class));
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }

    private void sendNotification(String title, String content) {




        Notification.Builder builder=new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content);


        NotificationManager manager=(NotificationManager) this.getSystemService
                (Context.NOTIFICATION_SERVICE);


        Intent intent=new Intent(this,MapsActivity.class);

        PendingIntent contentIntent=PendingIntent.getActivity(this,0,intent,
                PendingIntent.FLAG_IMMUTABLE);

        builder.setContentIntent(contentIntent);

        Notification notification=builder.build();
        notification.flags |=Notification.FLAG_AUTO_CANCEL;
        notification.defaults |=Notification.DEFAULT_SOUND;

        manager.notify(new Random().nextInt(),notification);






    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        displaylocation();
        startlocationupdates();

    }

    private void startlocationupdates() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&


                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)


        {
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiclient, mLocationRequest,this);

    }

    @Override
    public void onConnectionSuspended(int i) {


        mGoogleApiclient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation=location;
        displaylocation();

    }
}
