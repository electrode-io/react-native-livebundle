package com.livebundle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.ReactActivity;
import com.facebook.react.ReactActivityDelegate;

public class LiveBundleActivity extends ReactActivity {
  private String mDeepLinkPackageId = null;
  private String mDeppLinkLiveSessionId = null;

  @Override
  protected String getMainComponentName() {
    return "LiveBundleUI";
  }

  @Override
  protected ReactActivityDelegate createReactActivityDelegate() {
    return new ReactActivityDelegate(this, getMainComponentName()) {
      @Nullable
      @Override
      protected Bundle getLaunchOptions() {
        /**
         * If this activity has been launched using a LiveBundle package Deep Link,
         * then provide the 'deepLinkPackageId' initial prop to launch LiveBundleUI
         * component, so that it immediately proceeds to LiveBundle package download phase.
         */
        if (LiveBundleActivity.this.mDeepLinkPackageId != null) {
          Bundle bundle = new Bundle();
          bundle.putString("deepLinkPackageId", LiveBundleActivity.this.mDeepLinkPackageId);
          return bundle;
        }
        /**
         * Else if this activity has been launched using a LiveBundle live session Deep Link
         * then provide the 'deepLinkSessionId' initial prop to launch LiveBundleUI
         */
         else if (LiveBundleActivity.this.mDeppLinkLiveSessionId != null) {
          Bundle bundle = new Bundle();
          bundle.putString("deepLinkSessionId", LiveBundleActivity.this.mDeppLinkLiveSessionId);
          return bundle;
         }
        /**
         * Otherwise the activity was launched by the user from the application.
         * In that case, do not pass any initial props to LiveBundleUI component.
         * It will show the landing page of LiveBundle UI.
         */
        return null;
      }
    };
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
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
        Log.d("LB", "authority is " + authority);
        if (authority.equals("packages")) {
          this.mDeepLinkPackageId = uri.getQueryParameter("id");
        } else if (authority.equals("sessions")) {
          this.mDeppLinkLiveSessionId = uri.getQueryParameter("id");
        }
      } catch (Exception e) {
      }
    }
    super.onCreate(savedInstanceState);
  }
}
