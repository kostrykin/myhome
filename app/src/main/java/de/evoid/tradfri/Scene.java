package de.evoid.tradfri;

import androidx.annotation.NonNull;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Scene {

    public final TradfriGateway gateway;
    public final int sceneId;
    public final String name;
    public final boolean predefined;

    private final Map<Integer, JSONObject> settings = new HashMap<Integer, JSONObject>();

    public Scene(TradfriGateway gateway, int sceneId, CoapResponse response, Collection<LightBulb> bulbWhitelist) throws TradfriException {
        this.gateway = gateway;
        this.sceneId = sceneId;
        try {
            JSONObject json = new JSONObject(response.getResponseText());
            name = json.getString(TradfriConstants.NAME);
            predefined = (name.equalsIgnoreCase("alloff"));
            JSONArray settings = json.getJSONArray(TradfriConstants.SCENE_LIGHT_SETTINGS);
            for (int i = 0; i < settings.length(); i++) {
                JSONObject bulbSettings = settings.getJSONObject(i);
                int bulbId = bulbSettings.getInt(TradfriConstants.INSTANCE_ID);
                bulbSettings.remove(TradfriConstants.INSTANCE_ID);
                if (isBulbWhitelisted(bulbId, bulbWhitelist)) {
                    this.settings.put(bulbId, bulbSettings);
                }
            }
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
    }

    private static boolean isBulbWhitelisted(int bulbId, Collection<LightBulb> bulbWhitelist) {
        if (bulbWhitelist == null) return true;
        for (LightBulb bulb : bulbWhitelist) {
            if (bulb.getId() == bulbId) return true;
        }
        return false;
    }

    public boolean belongsToRoom(Room room) {
        for (int bulbId : settings.keySet()) {
            if (!room.objectIds.contains(bulbId)) return false;
        }
        return true;
    }

    public void activate() throws TradfriException {
        for (Map.Entry<Integer, JSONObject> entry : settings.entrySet()) {
            final int bulbId = entry.getKey();
            final JSONObject bulbSettings = entry.getValue();
            try {
                if (!bulbSettings.has(TradfriConstants.ONOFF)) {
                    throw new TradfriException("Bulb " + bulbId + " is not connected");
                }
                if (bulbSettings.getInt(TradfriConstants.ONOFF) == 0) {
                    bulbSettings.remove(TradfriConstants.DIMMER);
                }
            } catch (JSONException ex) {
                throw new TradfriException(ex);
            }
            LightBulb.set(gateway, bulbId, bulbSettings);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return this.name;
    }
}
