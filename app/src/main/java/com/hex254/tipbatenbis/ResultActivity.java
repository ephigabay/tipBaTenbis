package com.hex254.tipbatenbis;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ResultActivity extends AppCompatActivity {

    FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    DatabaseReference DBPlaceRef = mDatabase.getReference("places");
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    Restaurant currentRestaurant;

    private void setAcceptsTip(Restaurant restaurant) {
        DBPlaceRef.child(restaurant.place_id).setValue(restaurant);
        displayPlaceStatus(restaurant);
        Toast.makeText(this, getString(R.string.thanks), Toast.LENGTH_SHORT).show();
        findViewById(R.id.responseView).setVisibility(View.INVISIBLE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Intent intentIGotFromMainActivity = getIntent();
        currentRestaurant = ((Restaurant) intentIGotFromMainActivity.getSerializableExtra("currRestaurant"));


        if(currentRestaurant != null) {

            displayRestaurantName(currentRestaurant);

            getPlace(currentRestaurant.place_id);
        }
        else {
            Toast.makeText(this, getString(R.string.haveNoIdea), Toast.LENGTH_LONG).show();
            displayAutocomplete();
        }

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

    protected void getPlace(String place_id) {
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

    private void displayAutocomplete() {
        try {

            AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ESTABLISHMENT)
                    .build();

            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .setFilter(typeFilter)
                            .build(ResultActivity.this);


            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                currentRestaurant = restaurantFromPlace(place);

                displayRestaurantName(currentRestaurant);

                getPlace(currentRestaurant.place_id);
            }
        }
    }

    private void displayRestaurantName(Restaurant restaurant) {
        TextView textRestName = (TextView)findViewById(R.id.textRestName);
        assert textRestName != null;
        textRestName.setText(restaurant.name);
        textRestName.setVisibility(View.VISIBLE);

        ((Button)findViewById(R.id.notInLocationBTN)).setText(getString(R.string.notInLocation) + restaurant.name + "?");

    }

    public Restaurant restaurantFromPlace(Place place) {
        return new Restaurant(place.getId(), place.getName().toString());
    }



}
