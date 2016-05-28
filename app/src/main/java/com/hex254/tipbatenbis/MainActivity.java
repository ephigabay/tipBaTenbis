package com.hex254.tipbatenbis;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements OnConnectionFailedListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 123;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private GoogleApiClient mGoogleApiClient;
    Restaurant currentRestaurant;
    private PlaceLikelihood likeliestPlace;

    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    DatabaseReference DBPlaceRef = mDatabase.getReference("places");

    private void setAcceptsTip(Restaurant restaurant) {
        DBPlaceRef.child(restaurant.place_id).setValue(restaurant);
        displayPlaceStatus(restaurant);
        Toast.makeText(MainActivity.this, getString(R.string.thanks), Toast.LENGTH_SHORT).show();
        findViewById(R.id.responseView).setVisibility(View.INVISIBLE);
    }

    public Restaurant restaurantFromPlaceLikelihood(PlaceLikelihood placeLikelihood) {
        return new Restaurant(placeLikelihood.getPlace().getId(), placeLikelihood.getPlace().getName().toString());
    }

    public Restaurant restaurantFromPlace(Place place) {
        return new Restaurant(place.getId(), place.getName().toString());
    }

    protected void getPlace(String place_id) {
        // /<place_id>/
        DBPlaceRef.child(place_id).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        // Get user value
                        Restaurant restaurant = dataSnapshot.getValue(Restaurant.class);


                        displayPlaceStatus(restaurant);

                        findViewById(R.id.responseView).setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("", "getUser:onCancelled", databaseError.toException());
                    }
                });
    }

    private void displayPlaceStatus(Restaurant restaurant) {
        String tipAvailable;
        String otherwise;

        if (restaurant == null) {
            tipAvailable = getString(R.string.unknown_tip_acceptance);
            otherwise = getString(R.string.can_ask_waiter);

        }
        else {
            if (restaurant.getAccepts_tenbis_tip()) {
                tipAvailable = getString(R.string.accepts_tip);
            }
            else {
                tipAvailable = getString(R.string.doesnt_accept_tip);
            }

            otherwise = getString(R.string.know_otherwise);
        }

        TextView textTip = (TextView)findViewById(R.id.textTip);
        TextView otherwiseMessage = (TextView)findViewById(R.id.otherwiseMessage);

        textTip.setText(tipAvailable);
        textTip.setVisibility(View.VISIBLE);

        otherwiseMessage.setText(otherwise);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
//                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .build();

        mGoogleApiClient.connect();

        Button locationButton = (Button)findViewById(R.id.findLocationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });



        Button responseAcceptButton = (Button)findViewById(R.id.response_accept);
        responseAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRestaurant.setAccepts_tenbis_tip(true);
                setAcceptsTip(currentRestaurant);
            }
        });

        Button responseDontAcceptButton = (Button)findViewById(R.id.response_dont_accept);
        responseDontAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRestaurant.setAccepts_tenbis_tip(false);
                setAcceptsTip(currentRestaurant);
            }
        });

        Button notInLocationBTN = (Button)findViewById(R.id.notInLocationBTN);
        notInLocationBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                displayAutocomplete();

            }


        });


    }

    private void displayAutocomplete() {
        try {

            AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT)
                    .build();

            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .setFilter(typeFilter)
                            .build(MainActivity.this);


            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                currentRestaurant = restaurantFromPlace(place);

                TextView textRestName = (TextView)findViewById(R.id.textRestName);
                assert textRestName != null;
                textRestName.setText(currentRestaurant.name);
                textRestName.setVisibility(View.VISIBLE);

                ((Button)findViewById(R.id.notInLocationBTN)).setText(getString(R.string.notInLocation) + currentRestaurant.name + "?");

                findViewById(R.id.findLocationButton).setVisibility(View.INVISIBLE);

                getPlace(currentRestaurant.place_id);
            }
        }
    }

    private Restaurant getTopRestaurant(PlaceLikelihoodBuffer likelyPlaces) {
        for (PlaceLikelihood placeLikelihood : likelyPlaces) {
            if(placeLikelihood.getPlace().getPlaceTypes().indexOf(Place.TYPE_RESTAURANT) > -1) {
                return restaurantFromPlaceLikelihood(placeLikelihood);
            }
        }
        return null;
    }

    private void getLocation() {

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        if(!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, "not connected", Toast.LENGTH_LONG).show();
            return;
        }



        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(mGoogleApiClient, null);

        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
//                Toast.makeText(getApplicationContext(), "found " + likelyPlaces.getCount(), Toast.LENGTH_LONG).show();

                    currentRestaurant = getTopRestaurant(likelyPlaces);

                    if(currentRestaurant != null) {

                        TextView textRestName = (TextView) findViewById(R.id.textRestName);
                        assert textRestName != null;
                        textRestName.setText(currentRestaurant.name);
                        textRestName.setVisibility(View.VISIBLE);

                        ((Button) findViewById(R.id.notInLocationBTN)).setText(getString(R.string.notInLocation) + currentRestaurant.name + "?");

                        findViewById(R.id.findLocationButton).setVisibility(View.INVISIBLE);

                        getPlace(currentRestaurant.place_id);
                    }
                    else {
                        Toast.makeText(MainActivity.this, getString(R.string.haveNoIdea), Toast.LENGTH_LONG).show();
                        displayAutocomplete();
                    }
                /*
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i("asdf", String.format("Restaurant '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getLikelihood()));
                }*/
                likelyPlaces.release();
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();

                } else {
                    Toast.makeText(this, "oh noz", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if( mGoogleApiClient != null )
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if( mGoogleApiClient != null && mGoogleApiClient.isConnected() ) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.toString(), Toast.LENGTH_LONG).show();
    }
}
