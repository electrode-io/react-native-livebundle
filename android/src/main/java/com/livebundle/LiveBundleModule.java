package com.livebundle;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.facebook.react.ReactApplication;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.devsupport.interfaces.DevOptionHandler;

import java.io.BufferedInputStream;
import java.io.File;

import com.facebook.react.devsupport.interfaces.DevBundleDownloadListener;
import com.facebook.react.devsupport.BundleDownloader;

import androidx.annotation.Nullable;

import okhttp3.OkHttpClient;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.facebook.react.bridge.JSBundleLoader;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LiveBundleModule extends ReactContextBaseJavaModule {
  private final ReactApplicationContext reactContext;
  private final File mJSLiveBundleFile;
  private final File mJSLiveBundleZipFile;
  private final BundleDownloader mBundleDownloader;
  private final OkHttpClient mOkHttpClient;
  private boolean mIsInitialized = false;
  private static final String JS_LIVEBUNDLE_FILE_NAME = "LiveBundle.js";
  private static final String JS_LIVEBUNDLE_ZIP_FILE_NAME = "LiveBundle.zip";
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final String E_BUNDLE_DOWNLOAD_ERROR = "E_BUNDLE_DOWNLOAD_ERROR";
  private static final String PREFS_DEBUG_SERVER_HOST_KEY = "debug_http_host";
  private static final String PREFS_DEBUG_SERVER_HOST_KEY_BACKUP = "debug_http_host_backup";
  private JSBundleLoader mInitialJsBundleLoader;
  private String mAzureUrl;
  private String mPackageId;
  private String mBundleId;
  private final SharedPreferences mPreferences;

  public LiveBundleModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    /**
     * Create the OkHttp client that will be used to download LiveBundle packages
     */
    mOkHttpClient =
      new OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(0, TimeUnit.MILLISECONDS)
        .build();

    mJSLiveBundleZipFile = new File(reactContext.getApplicationContext().getFilesDir(), JS_LIVEBUNDLE_ZIP_FILE_NAME);
    mJSLiveBundleFile = new File(reactContext.getApplicationContext().getFilesDir(), JS_LIVEBUNDLE_FILE_NAME);
    mBundleDownloader = new BundleDownloader(mOkHttpClient);
    mPreferences = PreferenceManager.getDefaultSharedPreferences(reactContext.getApplicationContext());
  }

  private void unzipLiveBundleZipFile() throws IOException {
    FileInputStream fileStream = null;
    BufferedInputStream bufferedStream = null;
    ZipInputStream zipStream = null;
    try {
      fileStream = new FileInputStream(mJSLiveBundleZipFile);
      bufferedStream = new BufferedInputStream(fileStream);
      zipStream = new ZipInputStream(bufferedStream);

      byte[] buffer = new byte[1024 * 8];
      ZipEntry entry = zipStream.getNextEntry();
      FileOutputStream fout = new FileOutputStream(mJSLiveBundleFile);
      try {
        int numBytesRead;
        while ((numBytesRead = zipStream.read(buffer)) != -1) {
          fout.write(buffer, 0, numBytesRead);
        }
      } finally {
        fout.close();
      }

      long time = entry.getTime();
      if (time > 0) {
        mJSLiveBundleFile.setLastModified(time);
      }
    } finally {
      try {
        if (zipStream != null) zipStream.close();
        if (bufferedStream != null) bufferedStream.close();
        if (fileStream != null) fileStream.close();
      } catch (Exception e) {
      }
    }
  }

  private ReactInstanceManager getInstanceManager() {
    final Activity currentActivity = getCurrentActivity();
    ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
    return reactApplication.getReactNativeHost().getReactInstanceManager();
  }

  @Override
  public String getName() {
    return "LiveBundle";
  }

  /**
   * Initializes LiveBundle native module
   * @param azureUrl Azure blob storage container url (ex: https://foo.blob.core.windows.net/bar/)
   */
  @ReactMethod
  public void initialize(String azureUrl, Promise promise) {
    if (!mIsInitialized) {
      mAzureUrl = azureUrl;
      // Add new "LiveBundle" entry to React Native developer menu
      getInstanceManager().getDevSupportManager().addCustomDevOption("LiveBundle", new DevOptionHandler() {
        @Override
        public void onOptionSelected() {
          // Launch LiveBundleUI component if the option is selected by the user
          launchLiveBundleUI(null);
        }
      });
      /**
       * Store a reference to the JSBundleLoader instance currently used by React Native
       * so that we can revert back to it when resetting app state to what it was prior
       * to installing a LiveBundle package.
       */
      try {
        final ReactInstanceManager instanceManager = getInstanceManager();
        Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
        mInitialJsBundleLoader = (JSBundleLoader) bundleLoaderField.get(instanceManager);
      } catch (Exception e) {
      }
      /**
       * Store the current debug http host used by React Native to connect to local package
       * so that we can revert back to  it when resetting app state  to what it was prior
       * to establishing a LiveBundle live session.
       */
      String curDebugServerHost = mPreferences.getString(PREFS_DEBUG_SERVER_HOST_KEY, null);
      String curDebugServerHostBackup = mPreferences.getString(PREFS_DEBUG_SERVER_HOST_KEY_BACKUP, null);
      if (curDebugServerHostBackup == null) {
        mPreferences.edit().putString(PREFS_DEBUG_SERVER_HOST_KEY_BACKUP, curDebugServerHost == null ? "null" : curDebugServerHost).commit();
      }

      mIsInitialized = true;
    }

    promise.resolve(null);
  }

  /**
   * Starts LiveBundleActivity that is containing the LiveBundleUI RN component
   */
  @ReactMethod
  public void launchLiveBundleUI(@Nullable Promise promise) {
    ReactInstanceManager instanceManager = getInstanceManager();
    Intent intent = new Intent(instanceManager.getCurrentReactContext(), LiveBundleActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    instanceManager.getCurrentReactContext().startActivity(intent);
    if (promise != null) {
      promise.resolve(null);
    }
  }

  @ReactMethod
  public void launchLiveSession(String serverHost, @Nullable Promise promise) {
    mPreferences.edit().putString(PREFS_DEBUG_SERVER_HOST_KEY, serverHost).commit();

    /**
     * Set current JSBundleLoader of ReactInstanceManager instance to null so that it will use
     * the debug host server to load the bundle.
     * Because the target mBundleLoader field is not public, we have to resort to using reflection.
     */
    try {
      final ReactInstanceManager instanceManager = getInstanceManager();
      Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
      bundleLoaderField.setAccessible(true);
      bundleLoaderField.set(instanceManager, null);
    } catch (Exception e) {
    }

    /**
     * Call recreateReactContextInBackground method.
     * This method should be called from the main UI thread (RN requirement).
     */
    final ReactInstanceManager instanceManager = getInstanceManager();
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        try {
          instanceManager.recreateReactContextInBackground();
        } catch (Exception e) {
        }
      }
    });
  }

  /**
   * Downloads a bundle given its id and its containing package id
   * @param packageId The id (UUID v4) of the package containing the bundle to download
   * @param bundleId The id (UUID v4) of the bundle to download
   */
  @ReactMethod
  public void downloadBundle(final String packageId, final String bundleId, final Promise promise) {
    final BundleDownloader.BundleInfo bundleInfo = new BundleDownloader.BundleInfo();
    // Initiate download
    mBundleDownloader.downloadBundleFromURL(
      new DevBundleDownloadListener() {
        @Override
        public void onSuccess() {
          try {
            // All bundles are zipped. Unzip the bundle to target final file location ...
            LiveBundleModule.this.unzipLiveBundleZipFile();
            // ... and trash  the zip file
            mJSLiveBundleZipFile.delete();
            // Update current packageId/bundleId
            mPackageId = packageId;
            mBundleId = bundleId;
            // Resolve promise, we're done with download !
            promise.resolve(null);

          } catch (Exception e) {
            promise.reject(E_BUNDLE_DOWNLOAD_ERROR, e);
          }
        }

        @Override
        public void onProgress(
          @Nullable final String status,
          @Nullable final Integer done,
          @Nullable final Integer total) {
          // Unused. Not sure it can be leveraged anyway given that the bundle is not downloaded
          // from metro packager, so unsure if it reports progress (if it does would be nice to
          // handle so that we can report download progress on LiveBundle download screen)
        }

        @Override
        public void onFailure(final Exception e) {
          promise.reject(E_BUNDLE_DOWNLOAD_ERROR, e);
        }
      },
      mJSLiveBundleZipFile,
      String.format("%spackages/%s/%s", mAzureUrl, packageId, bundleId),
      bundleInfo);
  }

  /**
   * Installs the current bundle.
   * Note that we don't need to resolve the promise here, because installing
   * the bundle will recreate the react context, so the caller code will not
   * be in context anymore after this method is executed.
   */
  @ReactMethod
  public void installBundle(Promise promise) {
    try {
      /**
       * Create a React Native bundle file loader to load the bundle.
       * and set it as current JSBundleLoader on ReactInstanceManager instance
       * Because the target mBundleLoader field is not public, we have to resort to using reflection.
       */
      final ReactInstanceManager instanceManager = getInstanceManager();
      final JSBundleLoader jsBundleLoader = JSBundleLoader.createFileLoader(mJSLiveBundleFile.getAbsolutePath());
      Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
      bundleLoaderField.setAccessible(true);
      bundleLoaderField.set(instanceManager, jsBundleLoader);

      /**
       * Call recreateReactContextInBackgroundFromBundleLoader method.
       * This method should be called from the main UI thread (RN requirement).
       * Because the method is not public, we have to resort to using reflection to call it
       */
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          try {
            Method recreateMethod = instanceManager.getClass().getDeclaredMethod("recreateReactContextInBackgroundFromBundleLoader");
            recreateMethod.setAccessible(true);
            recreateMethod.invoke(instanceManager);
          } catch (Exception e) {
            LiveBundleModule.this.reset(null);
          }
        }
      });
    } catch (Exception e) {
      LiveBundleModule.this.reset(null);
    }
  }

  /**
   * Restores the state of the application to the one prior to installing a LiveBundle bundle.
   */
  @ReactMethod
  public void reset(@Nullable Promise promise) {
    try {
      /**
       * Restores the JSBundleLoader instance of ReactInstanceManager to the one that was used
       * before installing any JS bundle using LiveBundle.
       * Because the target mBundleLoader field is not public, we have to resort to using reflection.
       */
      final ReactInstanceManager instanceManager = getInstanceManager();
      Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
      bundleLoaderField.setAccessible(true);
      bundleLoaderField.set(instanceManager, mInitialJsBundleLoader);

      /**
       * Restore the debug server host
       */
      String debugServerHostBackup = mPreferences.getString(PREFS_DEBUG_SERVER_HOST_KEY_BACKUP, null);
      if (debugServerHostBackup == null || debugServerHostBackup.equals("null")) {
        mPreferences.edit().remove(PREFS_DEBUG_SERVER_HOST_KEY).commit();
      } else {
        mPreferences.edit().putString(PREFS_DEBUG_SERVER_HOST_KEY, debugServerHostBackup).commit();
      }

      /**
       * Call recreateReactContextInBackground method.
       * This method should be called from the main UI thread (RN requirement).
       */
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          try {
            instanceManager.recreateReactContextInBackground();
          } catch (Exception e) {
          }
        }
      });
    } catch (Exception e) {
    } finally {
      mPackageId = null;
      mBundleId = null;
    }
  }
}
