package io.livebundle;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.devsupport.interfaces.DevOptionHandler;

import java.io.BufferedInputStream;
import java.io.File;

import com.facebook.react.devsupport.interfaces.DevBundleDownloadListener;
import com.facebook.react.devsupport.BundleDownloader;

import androidx.annotation.Nullable;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.facebook.react.bridge.JSBundleLoader;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.util.Log;

public class LiveBundleModule extends ReactContextBaseJavaModule {
  private static final String JS_LIVEBUNDLE_FILE_NAME = "LiveBundle.js";
  private static final String JS_LIVEBUNDLE_ZIP_FILE_NAME = "LiveBundle.zip";
  private static final int CONNECT_TIMEOUT_MS = 5000;
  private static final String E_BUNDLE_DOWNLOAD_ERROR = "E_BUNDLE_DOWNLOAD_ERROR";
  private static final String E_METADATA_REQUEST_ERROR = "E_METADATA_REQUEST_ERROR";
  private static final String E_LIVEBUNDLE_ERROR = "E_LIVEBUNDLE_ERROR";
  private static final String PREFS_DEBUG_SERVER_HOST_KEY = "debug_http_host";
  private static final String PREFS_DEBUG_SERVER_HOST_KEY_BACKUP = "debug_http_host_backup";
  private static final String TAG = "LiveBundleModule";

  //
  // Keep the following variables static so that they are preserved
  // after recreating react context (which recreates native modules)
  private static JSBundleLoader sInitialJsBundleLoader;
  private static String sPackageId;
  private static String sBundleId;
  private static boolean sBundleInstalled = false;
  private static boolean sSessionStarted = false;
  private static ReactInstanceManager sReactInstanceManager;
  private static String sAzureSasToken;
  private static String sAzureUrl;
  private static ReactNativeHost sReactNativeHost;

  private final File mJSLiveBundleFile;
  private final File mJSLiveBundleZipFile;

  private final BundleDownloader mBundleDownloader;
  private final SharedPreferences mPreferences;
  private final String mApplicationPackageName;
  private final OkHttpClient mOkHttpClient;

  enum LB_DATA_TYPE {
    PACKAGE,
    SESSION,
  }

  /**
   * LiveBundle constructor
   * Called by React Native during Native Modules initialization phase (prior to bundle load)
   * Will be called any time the React context is recreated (all native modules are reinstantiated)
   *
   * @param reactContext The React context
   */
  public LiveBundleModule(ReactApplicationContext reactContext) {
    super(reactContext);

    Log.d(TAG, "ctor");

    //
    // Create the OkHttp client to be used to download LiveBundle bundles
    mOkHttpClient = new OkHttpClient.Builder()
      .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .readTimeout(0, TimeUnit.MILLISECONDS)
      .writeTimeout(0, TimeUnit.MILLISECONDS)
      .build();

    //
    // Create file handles
    mJSLiveBundleZipFile = new File(reactContext.getApplicationContext().getFilesDir(), JS_LIVEBUNDLE_ZIP_FILE_NAME);
    mJSLiveBundleFile = new File(reactContext.getApplicationContext().getFilesDir(), JS_LIVEBUNDLE_FILE_NAME);

    //
    // Get application package name
    mApplicationPackageName = reactContext.getApplicationContext().getPackageName();

    //
    // Create bundle downloader (to download LiveBundle bundles)
    // To make things easier, we just use the downloader shipped with React Native, but could
    // write a custom one instead at some point
    mBundleDownloader = new BundleDownloader(mOkHttpClient);

    //
    // Get access to Shared Preferences
    mPreferences = PreferenceManager.getDefaultSharedPreferences(reactContext.getApplicationContext());

    //
    // Add new "LiveBundle" entry to React Native developer menu
    getInstanceManager().getDevSupportManager().addCustomDevOption("LiveBundle", new DevOptionHandler() {
      @Override
      public void onOptionSelected() {
        // Launch LiveBundleUI component if the option is selected by the user
        launchUI(null);
      }
    });

    //
    // Backup a reference to the JSBundleLoader instance currently used by React Native
    // so that we can revert back to it when resetting app state to what it was prior
    // to switching to LiveBundle context (package or session)
    try {
      // Only backup if not currently in LiveBundle context (package or session)
      if (!LiveBundleModule.sBundleInstalled && !LiveBundleModule.sSessionStarted) {
        final ReactInstanceManager instanceManager = getInstanceManager();
        Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
        bundleLoaderField.setAccessible(true);
        LiveBundleModule.sInitialJsBundleLoader = (JSBundleLoader) bundleLoaderField.get(instanceManager);
      }
    } catch (Exception e) {
    }

    //
    // Backup the current debug http host used by React Native to connect to local packager
    // so that we can revert back to it when resetting app state to what it was prior
    // to establishing a LiveBundle live session (which alter debug http host url)
    String curDebugServerHost = mPreferences.getString(PREFS_DEBUG_SERVER_HOST_KEY, null);
    String curDebugServerHostBackup = mPreferences.getString(PREFS_DEBUG_SERVER_HOST_KEY_BACKUP, null);
    if (curDebugServerHostBackup == null) {
      mPreferences.edit().putString(PREFS_DEBUG_SERVER_HOST_KEY_BACKUP,
        curDebugServerHost == null ? "null" : curDebugServerHost).commit();
    }
  }

  /**
   * Initialize LiveBundle
   * Should be called by client application during application start
   *
   * @param reactNativeHost Instance of ReactNativeHost to use
   * @param azureUrl        Azure URL
   * @param azureSasToken   Azure SAS token (reads)
   */
  public static void initialize(
    ReactNativeHost reactNativeHost,
    String azureUrl,
    String azureSasToken) {
    LiveBundleModule.sReactNativeHost = reactNativeHost;
    LiveBundleModule.sReactInstanceManager = reactNativeHost.getReactInstanceManager();
    LiveBundleModule.sAzureUrl = azureUrl;
    LiveBundleModule.sAzureSasToken = azureSasToken;
  }

  public static ReactNativeHost getReactNativeHost() {
    return LiveBundleModule.sReactNativeHost;
  }

  /**
   * Constants exposed for use by JavaScript side of the native module
   * The JS side of the native module will make some calls to Azure using fetch
   * so it needs the azure storage url as well the sas token
   */
  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put("AZURE_SASTOKEN", sAzureSasToken);
    constants.put("AZURE_URL", sAzureUrl);
    return constants;
  }

  /**
   * Unzips the current LiveBundle zipped bundle (LiveBundle.zip) to target file (LiveBundle.js)
   *
   * @throws IOException
   */
  private void unzipLiveBundleZipFile() throws IOException {
    Log.d(TAG, "unzipLiveBundleZipFile()");
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

  /**
   * Returns the current ReactInstanceManager instance
   */
  private ReactInstanceManager getInstanceManager() {
    Log.d(TAG, "getInstanceManager()");

    /*
    //
    // Old way of retrieving instance manager via current Activity
    // This has the advantage of not requiring any code on the client side (mobile app) to
    // pass the instance manager, but the problem is in case there is no current activity
    // yet, it will not work
    // Keeping it here as we might need it again
    final Activity currentActivity = getCurrentActivity();
    ReactApplication reactApplication = (ReactApplication) currentActivity.getApplication();
    return reactApplication.getReactNativeHost().getReactInstanceManager();
    */
    return LiveBundleModule.sReactInstanceManager;
  }

  private void getMetadata(LB_DATA_TYPE type, String id, final Promise promise) {
    String metadataUrl = String.format(
      "%s%s/%s/%s%s",
      sAzureUrl,
      type == LB_DATA_TYPE.PACKAGE ? "packages" : "sessions",
      id,
      "metadata.json",
      sAzureSasToken);

    Request request = new Request.Builder()
      .url(metadataUrl)
      .build();

    Call call = mOkHttpClient.newCall(request);
    call.enqueue(new Callback() {
      public void onResponse(Call call, Response response)
        throws IOException {
        String res = response.body().string();
        promise.resolve(res);
      }

      public void onFailure(Call call, IOException e) {
        promise.reject(E_METADATA_REQUEST_ERROR, e);
      }
    });
  }

  @Override
  public String getName() {
    return "LiveBundle";
  }

  //===============================================================================================
  // METHODS EXPOSED TO JS
  //===============================================================================================

  /**
   * Returns the current LiveBundle state
   *
   * @param promise
   */
  @ReactMethod
  public void getState(Promise promise) {
    // Note : This could probably be simplified to keep only packageId/bundleId/sessionId
    // as we can derive isBundleInstalled / isSessionStarted just from these values
    WritableMap res = Arguments.createMap();
    res.putBoolean("isBundleInstalled", LiveBundleModule.sBundleInstalled);
    res.putBoolean("isSessionStarted", LiveBundleModule.sSessionStarted);
    res.putString("packageId", LiveBundleModule.sPackageId);
    res.putString("bundleId", LiveBundleModule.sBundleId);
    promise.resolve(res);
  }

  /**
   * Starts LiveBundleActivity containing the LiveBundleUI RN component (LiveBundle menu screen)
   */
  @ReactMethod
  public void launchUI(@Nullable Promise promise) {
    Log.d(TAG, "launchUI()");
    ReactInstanceManager instanceManager = getInstanceManager();
    Intent intent = new Intent(instanceManager.getCurrentReactContext(), LiveBundleActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    instanceManager.getCurrentReactContext().startActivity(intent);
    if (promise != null) {
      promise.resolve(null);
    }
  }

  /**
   * Returns the metadata associated to a given package
   * @param packageId id of the package
   */
  @ReactMethod
  public void getPackageMetadata(String packageId, final Promise promise) {
    getMetadata(LB_DATA_TYPE.PACKAGE, packageId, promise);
  }

  /**
   * Returns the metadata associated to a given session
   * @param sessionId id of the session
   */
  @ReactMethod
  public void getSessionMetadata(String sessionId, final Promise promise) {
    getMetadata(LB_DATA_TYPE.SESSION, sessionId, promise);
  }

  /**
   * Starts a LiveBundle session
   * We don't need to resolve the promise as this method recreate the React context,
   * so the caller context will be lost once this method completes
   */
  @ReactMethod
  public void launchLiveSession(String serverHost, final Promise promise) {
    Log.d(TAG, "launchLiveSession()");
    mPreferences.edit().putString(PREFS_DEBUG_SERVER_HOST_KEY, serverHost).commit();

    //
    // Set current JSBundleLoader of ReactInstanceManager instance to null so that it will use
    // the debug host server to load the bundle.
    // Because the target mBundleLoader field is not public, we have to resort to using reflection.
    try {
      final ReactInstanceManager instanceManager = getInstanceManager();
      Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
      bundleLoaderField.setAccessible(true);
      bundleLoaderField.set(instanceManager, null);
      LiveBundleModule.sSessionStarted = true;
    } catch (Exception e) {
      Log.e(TAG, "launchLiveSession", e);
      promise.reject(E_LIVEBUNDLE_ERROR, e);
    }

    //
    // Call recreateReactContextInBackground method.
    // This method should be called from the main UI thread (RN requirement).
    final ReactInstanceManager instanceManager = getInstanceManager();
    new Handler(Looper.getMainLooper()).post(new Runnable() {
      @Override
      public void run() {
        try {
          instanceManager.recreateReactContextInBackground();
        } catch (Exception e) {
          promise.reject(E_LIVEBUNDLE_ERROR, e);
        }
      }
    });
  }

  /**
   * Downloads a bundle given its id and its containing package id
   *
   * @param packageId The id (UUID v4) of the package containing the bundle to download
   * @param bundleId  The id (UUID v4) of the bundle to download
   */
  @ReactMethod
  public void downloadBundle(final String packageId, final String bundleId, final Promise promise) {
    Log.d(TAG, "downloadBundle()");
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
            LiveBundleModule.sPackageId = packageId;
            LiveBundleModule.sBundleId = bundleId;
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
      String.format("%spackages/%s/%s%s", sAzureUrl, packageId, bundleId, sAzureSasToken),
      bundleInfo);
  }

  /**
   * Installs the current bundle.
   * Note that we don't need to resolve the promise here, because installing
   * the bundle will recreate the react context, so the caller will not
   * be in context anymore after this method is executed.
   */
  @ReactMethod
  public void installBundle(final Promise promise) {
    Log.d(TAG, "installBundle()");
    try {
      //
      // Create a React Native bundle file loader to load the bundle.
      // and set it as current JSBundleLoader on ReactInstanceManager instance.
      // Because the target mBundleLoader field is not public, we have to resort to reflection.
      final ReactInstanceManager instanceManager = getInstanceManager();
      final JSBundleLoader jsBundleLoader = JSBundleLoader.createFileLoader(mJSLiveBundleFile.getAbsolutePath());
      Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
      bundleLoaderField.setAccessible(true);
      bundleLoaderField.set(instanceManager, jsBundleLoader);

      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          try {
            Context appContext = instanceManager.getCurrentReactContext().getApplicationContext();
            Intent appLaunchIntent = appContext.getPackageManager().getLaunchIntentForPackage(mApplicationPackageName);

            //
            // Start the main activity of the client application.
            // This is mostly solely needed when a LiveBundle bundle is installed from a deep link
            // and the application is not yet started (COLD state).
            // In this case, the deep link will load the LiveBundleActivity (LiveBundle menu) but
            // once the bundle is installed, there will be no Activity to go back to.
            // Starting the main activity of the client application prior to installing the bundle
            // in that case, solves this problem.
            // Because we are using FLAG_ACTIVITY_NEW_TASK, all other scenarios are also properly
            // covered. Indeed, if the main application activity already exist, it will not be
            // recreated, but the task its running in will be brought to front
            appLaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.getApplicationContext().startActivity(appLaunchIntent);

            //
            // Call recreateReactContextInBackgroundFromBundleLoader method.
            // This method should be called from the main UI thread (RN requirement).
            // Because the method is not public, we have to resort to reflection to call it
            Method recreateMethod = instanceManager.getClass().getDeclaredMethod("recreateReactContextInBackgroundFromBundleLoader");
            recreateMethod.setAccessible(true);
            recreateMethod.invoke(instanceManager);
            LiveBundleModule.sBundleInstalled = true;
          } catch (Exception e) {
            Log.e(TAG, "installBundle error [A]", e);
            LiveBundleModule.this.reset(null);
            LiveBundleModule.sBundleInstalled = false;
            promise.reject(E_LIVEBUNDLE_ERROR, e);
          }
        }
      });
    } catch (Exception e) {
      Log.e(TAG, "installBundle error [B]", e);
      LiveBundleModule.this.reset(null);
      LiveBundleModule.sBundleInstalled = false;
      promise.reject(E_LIVEBUNDLE_ERROR, e);
    }
  }

  /**
   * Restores the state of the application to the one prior to installing a LiveBundle bundle.
   * Note that we don't need to resolve the promise here, because installing
   * the bundle will recreate the react context, so the caller will not
   * be in context anymore after this method is executed.
   */
  @ReactMethod
  public void reset(@Nullable final Promise promise) {
    Log.d(TAG, "reset()");
    try {
      //
      // Restore the JSBundleLoader instance of ReactInstanceManager to the one that was used
      // before installing any JS bundle using LiveBundle.
      // Because the target mBundleLoader field is not public, we have to resort to reflection.
      final ReactInstanceManager instanceManager = getInstanceManager();
      Field bundleLoaderField = instanceManager.getClass().getDeclaredField("mBundleLoader");
      bundleLoaderField.setAccessible(true);
      bundleLoaderField.set(instanceManager, LiveBundleModule.sInitialJsBundleLoader);

      //
      // Restore the debug server host
      String debugServerHostBackup = mPreferences.getString(PREFS_DEBUG_SERVER_HOST_KEY_BACKUP, null);
      if (debugServerHostBackup == null || debugServerHostBackup.equals("null")) {
        mPreferences.edit().remove(PREFS_DEBUG_SERVER_HOST_KEY).commit();
      } else {
        mPreferences.edit().putString(PREFS_DEBUG_SERVER_HOST_KEY, debugServerHostBackup).commit();
      }

      //
      // Call recreateReactContextInBackground method.
      // This method should be called from the main UI thread (RN requirement).
      new Handler(Looper.getMainLooper()).post(new Runnable() {
        @Override
        public void run() {
          try {
            instanceManager.recreateReactContextInBackground();
          } catch (Exception e) {
            Log.e(TAG, "reset", e);
            if (promise != null) {
              promise.reject(E_LIVEBUNDLE_ERROR, e);
            }
          }
        }
      });
    } catch (Exception e) {
      Log.e(TAG, "reset", e);
      if (promise != null) {
        promise.reject(E_LIVEBUNDLE_ERROR, e);
      }
    } finally {
      LiveBundleModule.sPackageId = null;
      LiveBundleModule.sBundleId = null;
      LiveBundleModule.sBundleInstalled = false;
      LiveBundleModule.sSessionStarted = false;
    }
  }
}
