package io.livebundle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;

public class LiveBundleActivity extends ReactActivity {
  private String mDeepLinkPackageId;
  private String mDeppLinkLiveSessionId;

  private static final String TAG = "LiveBundleActivity";

  @Override
  protected String getMainComponentName() {
    return "LiveBundleUI";
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    Intent intent = getIntent();
    Uri uri = intent.getData();
    if (uri != null && "livebundle".equals(uri.getScheme())) {
      // Activity created navigating a Deep Link
      // A LiveBundle Deep Link can either be a LiveBundle package Deep Link ...
      // For example livebundle://packages?id=61e20565-3b44-49c5-b645-9d94672baccd
      // ... or a LiveBundle live session Deep Link
      // For exmample livebundle://sessions?id=51e20565-3b44-49c5-b645-9d94672baccd
      try {
        String authority = uri.getAuthority();
        if (authority.equals("packages")) {
          Log.d(TAG, "onCreate() packages deeplink");
          this.mDeepLinkPackageId = uri.getQueryParameter("id");
        } else if (authority.equals("sessions")) {
          Log.d(TAG, "onCreate() sessions deeplink");
          this.mDeppLinkLiveSessionId = uri.getQueryParameter("id");
        }
      } catch (Exception e) {
      }
    }
    super.onCreate(savedInstanceState);
  }

  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    Log.d(TAG, "createReactActivityDelegate()");
    return new ReactActivityDelegate(this, getMainComponentName()) {
      @Nullable
      @Override
      protected Bundle getLaunchOptions() {
        //
        // If this activity has been launched using a LiveBundle package Deep Link,
        // then provide the 'deepLinkPackageId' initial prop to launch LiveBundleUI
        // component, so that it immediately proceed to LiveBundle package download phase.
        if (LiveBundleActivity.this.mDeepLinkPackageId != null) {
          Log.d(TAG, "getLaunchOptions() deepLinkPackageId");
          Bundle bundle = new Bundle();
          bundle.putString("deepLinkPackageId", LiveBundleActivity.this.mDeepLinkPackageId);
          return bundle;
        }
        //
        // Else if this activity has been launched using a LiveBundle live session Deep Link
        // then provide the 'deepLinkSessionId' initial prop to launch LiveBundleUI
        else if (LiveBundleActivity.this.mDeppLinkLiveSessionId != null) {
          Log.d(TAG, "getLaunchOptions() deepLinkSessionId");
          Bundle bundle = new Bundle();
          bundle.putString("deepLinkSessionId", LiveBundleActivity.this.mDeppLinkLiveSessionId);
          return bundle;
        }
        Log.d(TAG, "getLaunchOptions() null");
        //
        // Otherwise the activity was launched by the user from the application.
        // In that case, do not pass any initial props to LiveBundleUI component.
        // It will show the landing page of LiveBundle UI.
        return null;
      }
    };
  }
}
