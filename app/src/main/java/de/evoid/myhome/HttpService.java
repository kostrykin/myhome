package de.evoid.myhome;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import fi.iki.elonen.NanoHTTPD;

public class HttpService extends Service {
    private Application app;
    private String authKey;
    private Handler handler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        authKey = getResources().getString(R.string.config_httpserver_auth_key);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Server server = new Server(8080);
        try {
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class Server extends NanoHTTPD {

        public Server(int port) {
            super(port);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, List<String>> parameters = session.getParameters();
            if (parameters.containsKey("auth") && parameters.get("auth").contains(authKey)) {
                Scanner sc = new Scanner(session.getInputStream());
                StringBuffer sb = new StringBuffer();
                while (sc.hasNext()) {
                    sb.append(sc.nextLine());
                }
                handleRequestTerm(sb.toString().trim());
                return newFixedLengthResponse(Response.Status.OK, "", "");
            } else {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "", "");
            }
        }
    }

    private void handleRequestTerm(String term) {
        if (term.isEmpty()) return;
        PhoneticResolver.Option option = PhoneticResolver.instance.resolve(term);
        if (option != null) {
            handler.post(option.action);
        }
    }
}
