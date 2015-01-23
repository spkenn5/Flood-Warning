package com.sig.flood_warning;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseACL;

public class FloodWarningApplication extends Application {
  private static final String TAG = "MyApp";

  @Override
  public void onCreate() {
    // TODO Auto-generated method stub
    super.onCreate();
    Parse.initialize(this, "8wwCL4aNAEtQV0umExX99SF81okuev6Z2YIacS23",
        "UPmRmZEaG9eji22EqtSGEdbSfwII2UETCQQqIaK4");
    ParseACL defaultACL = new ParseACL();
    // Optionally enable public read access.
    defaultACL.setPublicReadAccess(true);
    ParseACL.setDefaultACL(defaultACL, true);

  }
}
