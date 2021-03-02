package de.evoid.tradfri;

import androidx.annotation.NonNull;

import org.eclipse.californium.core.CoapResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Room {

    public final TradfriGateway gateway;
    public final int roomId;
    public final String name;
    public final boolean isSupergroup;
    public final Collection<Integer> objectIds;

    public Room(TradfriGateway gateway, int roomId, CoapResponse response) throws TradfriException {
        this.gateway = gateway;
        this.roomId = roomId;
        try {
            JSONObject json = new JSONObject(response.getResponseText());
            String name = json.getString(TradfriConstants.NAME);
            this.isSupergroup = name.equals("SuperGroup");
            this.name = (this.isSupergroup ? "" : name);
            JSONArray roomData = json.getJSONObject(TradfriConstants.HS_ACCESSORY_LINK).getJSONObject(TradfriConstants.OBJECTS).getJSONArray(TradfriConstants.INSTANCE_ID);
            Set<Integer> objectIds = new TreeSet<Integer>();
            for (int i = 0; i < roomData.length(); i++) {
                int objectId = roomData.getInt(i);
                objectIds.add(objectId);
            }
            this.objectIds = Collections.unmodifiableCollection(objectIds);
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
    }

    public List<Scene> discoverScenes(Collection<LightBulb> bulbWhitelist) throws TradfriException {
        List<Scene> scenes = new ArrayList<Scene>();
        try {
            CoapResponse response = gateway.get(TradfriConstants.SCENE + "/" + roomId);
            if (response == null) return null;
            JSONArray scenesData = new JSONArray(response.getResponseText());
            for (int i = 0; i < scenesData.length(); i++) {
                int sceneId = scenesData.getInt(i);
                response = gateway.get(TradfriConstants.SCENE + "/" + roomId + "/" + sceneId);
                scenes.add(new Scene(gateway, sceneId, response, bulbWhitelist));
            }
        } catch (JSONException ex) {
            throw new TradfriException(ex);

        }
        return scenes;
    }

    @NonNull
    @Override
    public String toString() {
        return this.name;
    }
}
