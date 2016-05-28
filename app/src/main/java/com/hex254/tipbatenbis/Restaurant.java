package com.hex254.tipbatenbis;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Restaurant implements java.io.Serializable {

    public String place_id; // As defined by Google API
    public String name;
    private Boolean accepts_tenbis_tip;
    private Boolean do_we_know = false;

    public Restaurant() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Restaurant(String place_id, String name) {
        this.name = name;
        this.place_id = place_id;
    }

    public Restaurant(String place_id, String name, boolean accepts_tenbis_tip) {
        this(place_id, name);
        setAccepts_tenbis_tip(accepts_tenbis_tip);
    }

    public boolean getDo_we_know() {
        return this.do_we_know;
    }

    public void setAccepts_tenbis_tip(Boolean accepts_tenbis_tip) {
        this.accepts_tenbis_tip = accepts_tenbis_tip;
        this.do_we_know = true;
    }

    public boolean getAccepts_tenbis_tip() {
        assert getDo_we_know() == true;
        return this.accepts_tenbis_tip;
    }

}