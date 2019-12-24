package de.evoid.tradfri;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TradfriGateway {

    private static final int MAX_RETRY_COUNT = 1;

    private final String gatewayIp;
    private final String securityKey;

    private String identity_usr = "";
    private String identity_psk = null;

    private CoapEndpoint coap = null;
    private Logger logger = Logger.getLogger(TradfriGateway.class.getName());

    public int timeout = 500; // milliseconds

    public TradfriGateway(String gatewayIp, String securityKey) throws TradfriException {
        this.gatewayIp = gatewayIp;
        this.securityKey = securityKey;
        initCoap();
    }

    public Logger getLogger() {
        return logger;
    }

    private void handshake() throws TradfriException {
        // see: https://www.reddit.com/r/homeautomation/comments/79yg03/new_ikea_tr%C3%A5dfri_gateway_firmware_1242_returns/

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
        builder.setPskStore(new StaticPskStore("Client_identity", securityKey.getBytes()));

        CoapClient client = new CoapClient();
        client.setEndpoint(new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard()));

        identity_usr = "jtr_" + Integer.toHexString(new Random().nextInt(0xFFFFFF));
        client.setURI("coaps://" + gatewayIp + "/15011/9063");
        CoapResponse response = client.post("{\"9090\":\"" + identity_usr + "\"}", MediaTypeRegistry.APPLICATION_JSON);

        try {
            JSONObject returnData = new JSONObject(response.getResponseText());
            identity_psk = returnData.getString("9091");
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
    }

    private void initCoap() throws TradfriException {
        handshake();

        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
        builder.setPskStore(new StaticPskStore(identity_usr, identity_psk.getBytes()));
        coap = new CoapEndpoint(new DTLSConnector(builder.build()), NetworkConfig.getStandard());
    }

    public void ping() throws TradfriException {
        //client.setURI("coaps://192.168.2.65:5684/.well-known/core");
        //client.ping(5000);
        get(".well-known/core");
    }

    public CoapResponse get(String path) throws TradfriException {
        Logger logger = Logger.getLogger(TradfriGateway.class.getName());
        logger.log(Level.INFO, "GET: " + "coaps://" + gatewayIp + "/" + path);
        int retryCount = 0;
        while (true) {
            CoapClient client = new CoapClient("coaps://" + gatewayIp + "/" + path);
            client.setTimeout(timeout);
            client.setEndpoint(coap);
            CoapResponse response = null;
            try {
                response = client.get(1);
                if (response != null && response.isSuccess()) return response;
                if (response == null && retryCount < MAX_RETRY_COUNT) {
                    ++retryCount;
                    logger.log(Level.WARNING, "Restarting gateway connection (" + retryCount + ")");
                    initCoap();
                    continue;
                }
            } catch (Exception ex) {
                throw new TradfriException(ex);
            } finally {
                client.shutdown();
            }
            throw new TradfriException("COAP query failed" + ( response != null ? " (reponse code: " + response.getCode() + ")" : " (timeout)"));
        }
    }

    public void set(String path, String payload) throws TradfriException {
        Logger logger = Logger.getLogger(TradfriGateway.class.getName());
        logger.log(Level.INFO, "SET: " + "coaps://" + gatewayIp + "/" + path + " = " + payload);
        int retryCount = 0;
        while (true) {
            CoapClient client = new CoapClient("coaps://" + gatewayIp + "/" + path);
            client.setTimeout(timeout);
            client.setEndpoint(coap);
            CoapResponse response = null;
            try {
                response = client.put(payload, MediaTypeRegistry.TEXT_PLAIN);
                if (response != null && response.isSuccess()) break;
                if (response == null && retryCount < MAX_RETRY_COUNT) {
                    ++retryCount;
                    logger.log(Level.WARNING, "Restarting gateway connection (" + retryCount + ")");
                    initCoap();
                    continue;
                }
            } catch (Exception ex) {
                throw new TradfriException(ex);
            } finally {
                client.shutdown();
            }
            throw new TradfriException("COAP query failed" + ( response != null ? " (reponse code: " + response.getCode() + ")" : " (timeout)"));
        }
    }

    public List<Room> discoverRooms() throws TradfriException {
        List<Room> rooms = new ArrayList<Room>();
        try {
            CoapResponse response = get(TradfriConstants.GROUPS);
            if (response == null) return null;
            JSONArray roomsData = new JSONArray(response.getResponseText());
            for (int i = 0; i < roomsData.length(); i++) {
                int roomId = roomsData.getInt(i);
                response = get(TradfriConstants.GROUPS + "/" + roomId);
                rooms.add(new Room(this, roomId, response));
            }
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
        return rooms;
    }

    public List<LightBulb> dicoverBulbs() throws TradfriException {
        List<LightBulb> bulbs = new ArrayList<LightBulb>();
        try {
            CoapResponse response = get(TradfriConstants.DEVICES);
            if (response == null) return null;
            JSONArray devices = new JSONArray(response.getResponseText());
            for (int i = 0; i < devices.length(); i++) {
                response = get(TradfriConstants.DEVICES + "/" + devices.getInt(i));
                if (response != null) {
                    JSONObject json = new JSONObject(response.getResponseText());
                    if (json.has(TradfriConstants.TYPE) && json.getInt(TradfriConstants.TYPE) == TradfriConstants.TYPE_BULB) {
                        bulbs.add(new LightBulb(json.getInt(TradfriConstants.INSTANCE_ID), this, response));
                    }
                }
            }
        } catch (JSONException ex) {
            throw new TradfriException(ex);
        }
        return bulbs;
    }

}
