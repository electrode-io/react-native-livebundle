package io.livebundle;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;
import com.facebook.react.ReactNativeHost;

public class LiveBundleActivity extends ReactActivity {
  private Bundle mInitialProps;

  private static final String TAG = "LiveBundleActivity";

  @Override
  protected String getMainComponentName() {
    return "LiveBundleUI";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    Intent intent = getIntent();
    mInitialProps = intent.getExtras();
    super.onCreate(savedInstanceState);
  }

  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    Log.d(TAG, "createReactActivityDelegate()");
    return new ReactActivityDelegate(this, getMainComponentName()) {
      @Override
      protected ReactNativeHost getReactNativeHost() {
        return LiveBundle.getReactNativeHost();
      }

      @Nullable
      @Override
      protected Bundle getLaunchOptions() {
        return LiveBundleActivity.this.mInitialProps;
      }
    };
  }
}
