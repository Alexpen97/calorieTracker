package com.nutritrack.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    // Custom local plugins must be registered before BridgeActivity init (Capacitor 4+).
    registerPlugin(SamsungHealthPlugin.class);
    super.onCreate(savedInstanceState);
  }
}
