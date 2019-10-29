package com.anx.application.jdriver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.suke.widget.SwitchButton;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DriverMapActivity extends AppCompatActivity implements RoutingListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, NavigationView.OnNavigationItemSelectedListener {


    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark};
    private int status = 0;

    private double COMPANY_CUT = 0.17;
    private double RIDE_PRICE;
    private double rideDistance, startingPrice, quota;
    private double newQuota;
    private int COMPANY_CUT_GIVEN = 0;

    private String customerId = "", destination, driverQuota;
    private String serviceType, currentDriver, quotaReference;

    private Boolean isLoggingOut = false;
    private boolean cameraSet = false;

    private Button mLogout, mSetting, mRideStatus, mHistory, mAccept, mDecline, mCallCustomer;
    private ImageView mCustomerProfileImage;
    private LinearLayout mCustomerInfo;
    private TextView mCustomerName, mCustomerPhone, mCustomerDestination, mDriverQuota, mRideCost;
    private TextView mPhoneNumber, mFullName;
    private ImageView mProfileImage;
    private TextView mService;
    private TextView mCustomerPickup;
    private com.suke.widget.SwitchButton mWorkingSwitch;
    private DrawerLayout drawer;

    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private SupportMapFragment mapFragment;
    private Marker pickupMarker;
    private LatLng pickupLatLng, destinationLatLng;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private DatabaseReference assignedCustomerPickupLocationRef;
    private ValueEventListener assignedCustomerPickupLocationRefListener;
    private Geocoder geocoder;

    private List<Polyline> polylines;
    private List<Address> addresses;


    // Location call back when map is ready

    LocationCallback mLocationCallback = new LocationCallback() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {

                String key = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference mDriverDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                mDriverDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                            if (driverMap.get("service").equals("Bajjaj")) {
                                startingPrice = 15.00;
                                mService.setText("Service type: Bajjaj");
                            } else if (driverMap.get("service").equals("Taxi")) {
                                startingPrice = 40.00;
                                mService.setText("Service type: Taxi");
                            } else {
                                Toast.makeText(DriverMapActivity.this, "Driver service type not assigned yet", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });

                DatabaseReference mQuotaReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key).child("quota");
                mQuotaReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        quotaReference = dataSnapshot.getValue().toString();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });


                if (!customerId.equals("")) {

                    COMPANY_CUT_GIVEN = 0;

                    Location pickupLocation = new Location(LocationManager.GPS_PROVIDER);
                    pickupLocation.setLatitude(pickupLatLng.latitude);
                    pickupLocation.setLongitude(pickupLatLng.longitude);

                    Location destinationLocation = new Location(LocationManager.GPS_PROVIDER);
                    destinationLocation.setLatitude(destinationLatLng.latitude);
                    destinationLocation.setLongitude(destinationLatLng.longitude);

                    DecimalFormat df = new DecimalFormat("#.#");
                    df.setRoundingMode(RoundingMode.CEILING);

                    rideDistance = Double.parseDouble(df.format(pickupLocation.distanceTo(destinationLocation) / 1000));

                    RIDE_PRICE = (rideDistance * 13) + startingPrice;

                    COMPANY_CUT = RIDE_PRICE * 0.17;

                    mRideCost.setText(df.format(RIDE_PRICE) + "");

                    newQuota = Double.parseDouble(quotaReference) - (COMPANY_CUT);

                }

                mLastLocation = location;
                final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                Log.i("Hello", "Location Changed");
                if (!cameraSet) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    cameraSet = true;
                }

                mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                    @Override
                    public boolean onMyLocationButtonClick() {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        return true;
                    }
                });


                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
                DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                GeoFire geoFireAvailable = new GeoFire(refAvailable);
                GeoFire geoFireWorking = new GeoFire(refWorking);

                switch (customerId) {
                    case "":
                        geoFireWorking.removeLocation(userId, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Log.i("Remove", "Location removed for working driver");
                            }
                        });
                        geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Log.i("available", "Location Updated for available driver");
                            }
                        });

                        break;
                    default:
                        geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Log.i("Remove", "Location removed for available driver");
                            }
                        });
                        geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                            @Override
                            public void onComplete(String key, DatabaseError error) {
                                Log.i("working", "Location Updated for working driver");
                            }
                        });

                        break;
                }
            }
        }
    };

    // End of Location call back


    // provide a cut from the RIDE_PRICE to the company

    private void provideCompanyCut() {

        if (COMPANY_CUT_GIVEN == 0) {
            String key = FirebaseAuth.getInstance().getCurrentUser().getUid();
            final DatabaseReference quotaUpdateReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
            quotaUpdateReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap map = new HashMap();
                    map.put("quota", newQuota);
                    quotaUpdateReference.updateChildren(map);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            COMPANY_CUT_GIVEN = 1;
        }

    }

    // End of provideCompanyCut function



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        polylines = new ArrayList<>();// initialize poly lines

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



        startService(new Intent(this, onAppKilled.class));// used to make app run in the background

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mCustomerInfo = (LinearLayout) findViewById(R.id.customerInfo);

        mCustomerProfileImage = (ImageView) findViewById(R.id.customerProfileImage);

        mCustomerName = (TextView) findViewById(R.id.customerName);
        mCustomerPhone = (TextView) findViewById(R.id.customerPhone);
        mCustomerDestination = (TextView) findViewById(R.id.customerDestination);
        mDriverQuota = (TextView) findViewById(R.id.driverQuota);
        mRideCost = (TextView) findViewById(R.id.ridePrice);
        mCustomerPickup = findViewById(R.id.customerPickup);

        View v = navigationView.getHeaderView(0);
        mFullName = v.findViewById(R.id.nav_name);
        mPhoneNumber = v.findViewById(R.id.nav_phone);
        mProfileImage = v.findViewById(R.id.profileImage);
        mService = findViewById(R.id.serviceType);

        getUserInformation();


        mRideStatus = (Button) findViewById(R.id.rideStatus);
        mCallCustomer = (Button) findViewById(R.id.callCustomer);

        mCallCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + mCustomerPhone.getText().toString()));
                startActivity(callIntent);
            }
        }); // call customer button


        mWorkingSwitch = (com.suke.widget.SwitchButton) findViewById(R.id.workingSwitch);

        mWorkingSwitch.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    connectDriver();
                    getDriverQuota();
                } else {
                    disconnectDriver();
                }
            }
        });// driver is working or not switch


        // display ride status
        mRideStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (status) {
                    case 1:// when the diver picks up the passenger
                        status = 2;
                        erasePolyLines();
                        Toast.makeText(DriverMapActivity.this, "" + destinationLatLng, Toast.LENGTH_SHORT).show();
                        if (destinationLatLng.latitude != 0.0 && destinationLatLng.longitude != 0.0) {
                            getRouteToMarker(destinationLatLng);
                        }
                        mRideStatus.setText("Drive Completed");
                        break;
                    case 2: // when drive is completed or ended
                        recordRide();
                        endRide();
                        provideCompanyCut();
                        break;
                }
            }
        });

        getDriverQuota();
        getAssignedCustomer();
    }


    // Get's user information when request arrives

    private void getUserInformation() {

        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mFullName.setText(map.get("name").toString());
                        Toast.makeText(DriverMapActivity.this, "" + map.get("name"), Toast.LENGTH_SHORT).show();
                    }
                    if (map.get("phone") != null) {
                        mPhoneNumber.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(getApplicationContext()).load(uri.toString()).error(R.drawable.ic_default_profile).into(mProfileImage);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    // End of getUserInformation


    // get driver current quota left with the company

    private void getDriverQuota() {
        final DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("quota");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { // If there is a customer request
                    quota = Double.parseDouble(dataSnapshot.getValue().toString());
                    if (quota == 0) {
                        mDriverQuota.setText("Please refill quota at your local office");
                        disconnectDriver();
                        mWorkingSwitch.setChecked(false);
                    } else {
                        mDriverQuota.setText("Quota: " + df.format(quota) + " birr");
                    }
                } else { // If there is no customer request
                    mDriverQuota.setText("Please visit local office to get quota");
                    disconnectDriver();
                    mWorkingSwitch.setChecked(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // End of getDriverQuota function

    // Logs out current user

    private void logOut() {
        isLoggingOut = true;
        disconnectDriver();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DriverMapActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
        return;
    }

    // End of log out function

    // Get assigned customer when request is made to the driver

    private void getAssignedCustomer() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest").child("customerRideId");
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { // If there is a customer request

                    status = 1;
                    customerId = dataSnapshot.getValue().toString();
                    getAssignedCustomerPickupLocation();
                    getAssignedCustomerDestination();
                    getAssignedCustomerInfo();
                } else { // If there is no customer request
                    endRide();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // End of getAssignedCustomer

    // Get's assigned customer's pickup location

    private void getAssignedCustomerPickupLocation() {
        assignedCustomerPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("customerRequest").child(customerId).child("l");
        assignedCustomerPickupLocationRefListener = assignedCustomerPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !customerId.equals("")) { // If customer request exists
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    pickupLatLng = new LatLng(locationLat, locationLng);
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location"));

                    geocoder = new Geocoder(DriverMapActivity.this, Locale.getDefault());
                    try {
                        addresses = geocoder.getFromLocation(pickupLatLng.latitude, pickupLatLng.longitude, 1);
                        String address = addresses.get(0).getAddressLine(0);
                        mCustomerPickup.setText(address);
                    } catch (IOException e) {
                        Toast.makeText(DriverMapActivity.this, "IOException", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    getRouteToMarker(pickupLatLng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // End of getAssignedCustomerPickupLocation function

    // draws route from current location to given location

    private void getRouteToMarker(LatLng pickupLatLng) {
        if (pickupLatLng != null && mLastLocation != null) {
            Routing routing = new Routing.Builder()
                    .key("AIzaSyBSVxzRAiYvKc-3-4AaKi8G0-tht285aHA")
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), pickupLatLng)
                    .build();
            routing.execute();
        }
    }

    // End of getRouteMarker function

    // get's final destination of customer to show to current user

    private void getAssignedCustomerDestination() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId).child("customerRequest");
        assignedCustomerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) { // If destination call is requested
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        mCustomerDestination.setText("Destination: " + destination);
                    } else {
                        mCustomerDestination.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;

                    if (map.get("destinationLatitude") != null) {
                        destinationLat = Double.valueOf(map.get("destinationLatitude").toString());
                    }
                    if (map.get("destinationLongitude") != null) {
                        destinationLng = Double.valueOf(map.get("destinationLongitude").toString());
                    }
                    destinationLatLng = new LatLng(destinationLat, destinationLng);
                    Toast.makeText(DriverMapActivity.this, "" + destinationLatLng, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    // end of getAssignedCustomerDestination function

    // get's assigned customer information (name, phone and optionally a photo)

    private void getAssignedCustomerInfo() {
        Log.i("CustomerInfo", "Info called");
        mCustomerInfo.setVisibility(View.VISIBLE);
        ;
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mCustomerName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mCustomerPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profileImage").child(customerId);
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(getApplicationContext()).load(uri.toString()).error(R.drawable.ic_default_profile).into(mCustomerProfileImage);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    // end of getAssignedCustomerInfo function


    // called when drive officially ends or customer cancel's drive

    private void endRide() {
        mRideStatus.setText("Picked Customer");
        mCustomerInfo.setVisibility(View.GONE);
        erasePolyLines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(customerId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Log.i("CustomerRequestRemoved", "Customer Request Removed");
            }
        });
        customerId = "";
        rideDistance = 0;

        mMap.clear();

        if (pickupMarker != null) {
            pickupMarker.remove();
        }

        if (assignedCustomerPickupLocationRefListener != null) {
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }

        mCustomerName.setText("");
        mCustomerPhone.setText("");
        mCustomerDestination.setText("Destination: --");
        mCustomerProfileImage.setImageResource(R.drawable.ic_default_profile);

    }

    // end of endRide function

    // records essential information (destination, route, fare, distance and date) when drive officially ends

    private void recordRide() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("history");
        DatabaseReference customerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(customerId).child("history");
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");
        String rideId = historyRef.push().getKey();

        driverRef.child(rideId).setValue(true);
        customerRef.child(rideId).setValue(true);

        HashMap map = new HashMap();
        map.put("driver", userId);
        map.put("customer", customerId);
        map.put("rating", 0);
        map.put("timeStamp", getCurrentTimeStamp());
        map.put("destination", destination);
        map.put("location/from/lat", pickupLatLng.latitude);
        map.put("location/from/lng", pickupLatLng.longitude);
        map.put("location/to/lat", destinationLatLng.latitude);
        map.put("location/to/lng", destinationLatLng.longitude);
        map.put("distance", rideDistance);
        map.put("fare", RIDE_PRICE);
        historyRef.child(rideId).updateChildren(map);

    }

    // end of recordRide function

    // converts system date and time into a comprehensive timestamp

    private Long getCurrentTimeStamp() {
        Long timeStamp = System.currentTimeMillis() / 1000;
        return timeStamp;
    }

    // end of getCurrentTimeStamp

    // called when map is ready to use

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }
    }

    // end of onMapReady function

    // check explicitly if location permission is given to the app

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Give Permission")
                        .setMessage("Access Location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    // end of checkLocationPermission function

    // called when permission is given to the app

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide a permission", Toast.LENGTH_LONG).show();
                    Log.i("Hello", "Permission Denied");
                }
                break;
            }
        }
    }

    // end of onRequestPermissionResult function

    // connects driver when working switch is on

    private void connectDriver() {
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        mMap.setMyLocationEnabled(true);
    }

    // end if connectDriver function

    // disconnects the driver when working switch is off or app is killed

    private void disconnectDriver() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }

        if (pickupMarker != null) {
            pickupMarker.remove();
        }

        if (assignedCustomerPickupLocationRefListener != null) {
            assignedCustomerPickupLocationRef.removeEventListener(assignedCustomerPickupLocationRefListener);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFireAvailable = new GeoFire(refAvailable);
        geoFireAvailable.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {

            }
        });


    }

    // end of disconnectDriver function

    // called when drawing a route is not available

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null) {
            Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    // end of onRoutingFailure

    @Override
    public void onRoutingStart() {
    }

    // called when route is successfully drawn

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickupLatLng);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width * 0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup Location"));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;
            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    // end onRoutingSuccess function

    @Override
    public void onRoutingCancelled() {
    }

    private void erasePolyLines() {
        for (Polyline line : polylines) {
            if (line != null) {
                line.remove();
            }
        }
        if (polylines != null) {
            polylines.clear();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Intent intent;
        switch (menuItem.getItemId()) {
            case R.id.nav_history:
                intent = new Intent(DriverMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                break;
            case R.id.nav_profile:
                intent = new Intent(DriverMapActivity.this, DriverSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout:
                logOut();
                break;
            case R.id.nav_callus:
                Toast.makeText(this, "Call Us", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_email:
                Toast.makeText(this, "Email Us", Toast.LENGTH_SHORT).show();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {

        if (mWorkingSwitch.isChecked()) {
            new AlertDialog.Builder(this)
                    .setMessage("Do you want the app to run in the background?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            moveTaskToBack(true);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DriverMapActivity.super.onBackPressed();
                            mWorkingSwitch.setChecked(false);
                        }
                    })
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
