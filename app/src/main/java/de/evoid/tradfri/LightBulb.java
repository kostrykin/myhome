package de.evoid.tradfri;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * https://github.com/ffleurey/ThingML-Tradfri/blob/master/tradfri-java/src/main/java/org/thingml/tradfri/LightBulb.java
 */
public class LightBulb {

    private TradfriGateway gateway;

    // Immutable information
    private int id;
    private String name;

    private JSONObject jsonObject;

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isOnline() {
        return online;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) throws TradfriException {
        try {
            JSONObject json = new JSONObject();
            JSONObject settings = new JSONObject();
            JSONArray array = new JSONArray();
            array.put(settings);
            json.put(TradfriConstants.LIGHT, array);
            settings.put(TradfriConstants.ONOFF, (on)?1:0);
            String payload = json.toString();
            gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

        } catch (JSONException ex) {
            Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
            throw new TradfriException(ex);
        }
        this.on = on;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) throws TradfriException {
        try {
            JSONObject json = new JSONObject();
            JSONObject settings = new JSONObject();
            JSONArray array = new JSONArray();
            array.put(settings);
            json.put(TradfriConstants.LIGHT, array);
            settings.put(TradfriConstants.DIMMER, intensity);
            settings.put(TradfriConstants.TRANSITION_TIME, 5);
            String payload = json.toString();
            gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

        } catch (JSONException ex) {
            Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
            throw new TradfriException(ex);
        }
        this.intensity = intensity;
    }

    public String getColor() {
        return color;
    }

    public void sendJSONPayload(String json) throws TradfriException {
        gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), json);
    }

    public void setRGBColor(int r, int g, int b) throws TradfriException {
        double red = r;
        double green = g;
        double blue = b;

        // gamma correction
        red = (red > 0.04045) ? Math.pow((red + 0.055) / (1.0 + 0.055), 2.4) : (red / 12.92);
        green = (green > 0.04045) ? Math.pow((green + 0.055) / (1.0 + 0.055), 2.4) : (green / 12.92);
        blue = (blue > 0.04045) ? Math.pow((blue + 0.055) / (1.0 + 0.055), 2.4) : (blue / 12.92);

        // Wide RGB D65 conversion
        // math inspiration: http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
        double X = red * 0.664511 + green * 0.154324 + blue * 0.162028;
        double Y = red * 0.283881 + green * 0.668433 + blue * 0.047685;
        double Z = red * 0.000088 + green * 0.072310 + blue * 0.986039;

        // calculate the xy values from XYZ
        double x = (X / (X + Y + Z));
        double y = (Y / (X + Y + Z));

        int xyX = (int) (x * 65535 + 0.5);
        int xyY = (int) (y * 65535 + 0.5);

        try {
            JSONObject json = new JSONObject();
            JSONObject settings = new JSONObject();
            JSONArray array = new JSONArray();
            array.put(settings);
            json.put(TradfriConstants.LIGHT, array);
            settings.put(TradfriConstants.COLOR_X, xyX);
            settings.put(TradfriConstants.COLOR_Y, xyY);
            settings.put(TradfriConstants.TRANSITION_TIME, 5);
            String payload = json.toString();
            gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

        } catch (JSONException ex) {
            Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void set(TradfriGateway gateway, int bulbId, JSONObject settings) throws TradfriException {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        array.put(settings);
        try {
            json.put(TradfriConstants.LIGHT, array);
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
        String payload = json.toString();
        gateway.set(TradfriConstants.DEVICES + "/" + bulbId, payload);
    }

    public void setColor(String color) throws TradfriException {
        try {
            JSONObject json = new JSONObject();
            JSONObject settings = new JSONObject();
            JSONArray array = new JSONArray();
            array.put(settings);
            json.put(TradfriConstants.LIGHT, array);
            settings.put(TradfriConstants.COLOR, color);
            settings.put(TradfriConstants.TRANSITION_TIME, 5);
            String payload = json.toString();
            gateway.set(TradfriConstants.DEVICES + "/" + this.getId(), payload);

        } catch (JSONException ex) {
            Logger.getLogger(TradfriGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.color = color;
    }
    private String manufacturer;
    private String type;
    private String firmware;

    // Status
    private boolean online;

    // State of the bulb
    private boolean on;
    private int intensity;
    private String color;

    // Dates
    private Date dateInstalled;
    private Date dateLastSeen;

    public String getFirmware() {
        return firmware;
    }

    public Date getDateInstalled() {
        return dateInstalled;
    }

    public Date getDateLastSeen() {
        return dateLastSeen;
    }

    public LightBulb(int id, TradfriGateway gateway) {
        this.id = id;
        this.gateway = gateway;
    }

    public LightBulb(int id, TradfriGateway gateway, CoapResponse response) throws TradfriException {
        this.id = id;
        this.gateway = gateway;
        if (response != null) parseResponse(response);
    }

    protected void updateBulb() throws TradfriException {
        CoapResponse response = gateway.get(TradfriConstants.DEVICES + "/" + id);
        if (response != null) parseResponse(response);
    }

    protected void parseResponse(CoapResponse response) throws TradfriException {
        gateway.getLogger().log(Level.INFO, response.getResponseText());
        try {
            JSONObject json = new JSONObject(response.getResponseText());
            jsonObject = json;
            String new_name = json.getString(TradfriConstants.NAME);
            name = new_name;

            dateInstalled = new Date(json.getLong(TradfriConstants.DATE_INSTALLED)*1000);
            dateLastSeen = new Date(json.getLong(TradfriConstants.DATE_LAST_SEEN)*1000);
            //instanceId = json.getLong(TradfriConstants.INSTANCE_ID);

            boolean new_online = json.getInt(TradfriConstants.DEVICE_REACHABLE) != 0;
            online = new_online;

            manufacturer = json.getJSONObject("3").getString("0");
            type = json.getJSONObject("3").getString("1");
            firmware = json.getJSONObject("3").getString("3");

            JSONObject light = json.getJSONArray(TradfriConstants.LIGHT).getJSONObject(0);

            if (light.has(TradfriConstants.ONOFF) && light.has(TradfriConstants.DIMMER)) {
                boolean new_on = (light.getInt(TradfriConstants.ONOFF) != 0);
                int new_intensity = light.getInt(TradfriConstants.DIMMER);
                on = new_on;
                intensity = new_intensity;
            }
            else {
                online = false;
            }
            if (light.has(TradfriConstants.COLOR)) {
                String new_color = light.getString(TradfriConstants.COLOR);
                color = new_color;
            }
        } catch (JSONException ex) {
            String error = "Cannot update bulb info: error parsing the response from the gateway.";
            System.err.println(error);
            ex.printStackTrace();
            throw new TradfriException(error, ex);
        }
    }

    public String toString() {
        String result = "[BULB " + id + "]";
        if (online) result += "\ton:" + on + "\tdim:" + intensity + "\tcolor:" + color;
        else result += "  ********** OFFLINE *********** ";
        result += "\ttype: " + type + "\tname: " + name;
        return result;
    }

}
