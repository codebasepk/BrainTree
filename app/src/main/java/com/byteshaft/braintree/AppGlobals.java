package com.byteshaft.braintree;

import android.app.Application;

public class AppGlobals extends Application {

    public static final String SERVER_IP = "http://178.62.69.210:9000";
    public static final String BASE_URL = String.format("%s/api/", SERVER_IP);

}
