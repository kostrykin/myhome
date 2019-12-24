package de.evoid.tradfri;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Scene {

    public final TradfriGateway gateway;
    public final int sceneId;
    public final String name;
    public final boolean predefined;

    private final Map<Integer, JSONObject> settings = new HashMap<Integer, JSONObject>();

    public Scene(TradfriGateway gateway, int sceneId, CoapResponse response) throws TradfriException {
        this.gateway = gateway;
        this.sceneId = sceneId;
        try {
            JSONObject json = new JSONObject(response.getResponseText());
            name = json.getString(TradfriConstants.NAME);
            predefined = json.getInt(TradfriConstants.SCENE_PREDEFINED) > 0;
            JSONArray settings = json.getJSONArray(TradfriConstants.SCENE_LIGHT_SETTINGS);
            for (int i = 0; i < settings.length(); i++) {
                JSONObject bulbSettings = settings.getJSONObject(i);
                int bulbId = bulbSettings.getInt(TradfriConstants.INSTANCE_ID);
                bulbSettings.remove(TradfriConstants.INSTANCE_ID);
                this.settings.put(bulbId, bulbSettings);
            }
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
    }

    public void activate() throws TradfriException {
        for (Map.Entry<Integer, JSONObject> entry : settings.entrySet()) {
            final int bulbId = entry.getKey();
            final JSONObject bulbSettings = entry.getValue();
            LightBulb.set(gateway, bulbId, bulbSettings);
        }
    }
}
