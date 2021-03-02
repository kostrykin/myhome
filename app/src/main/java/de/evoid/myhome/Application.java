package de.evoid.myhome;

import android.content.Context;
import android.content.Intent;

import org.acra.ACRA;
import org.acra.annotation.AcraCore;
import org.acra.annotation.AcraMailSender;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "crashreports@evoid.de")
public class Application extends android.app.Application {
    private static Application instance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, HttpService.class));
        Application.instance = this;
    }

    public static Context getContext() {
        return instance;
    }
}
