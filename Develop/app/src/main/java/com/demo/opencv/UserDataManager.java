package com.demo.opencv;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class UserDataManager extends Application {
    RxDataStore<androidx.datastore.preferences.core.Preferences> dataStore;
    Map<String, String> defaultSettings = new HashMap<>();
    Context mContext;
    // Values
    private String textEntryContext = ""; // the conversation context from text-entry mode
    private int lightingThreshold = 0; // lighting threshold
    private int textEntryMode = 2;
    private int calibrationState = -1; // -1 = idle
    public final int calibrationTemplateNum = 6;
    private int sensitivity = 20;
    private String language = "English";
    private String requestToken = "";
    private String requestTokenSecret = "";
    private String accessToken = "";
    private String accessTokenSecret = "";
    private Bitmap[] leftCalibrationData;
    private Bitmap[] rightCalibrationData;
    String[] leftEyeFileNames = {"left_straight", "left_left", "left_right", "left_up", "left_left_up", "left_right_up"};
    String[] rightEyeFileNames = {"right_straight", "right_left", "right_right", "right_up", "right_left_up", "right_right_up"};
    private final String[] reference = {"Straight", "Left", "Right", "Up", "Down", "Closed", "Left Up", "Right Up"};
    @Override
    public void onCreate() { // initialize the shared preferences
        super.onCreate();
        mContext = this;
        dataStore = new RxPreferenceDataStoreBuilder(this, "settings").build();

        // set up default settings
        defaultSettings.put("Language", "English");
        defaultSettings.put("LightingThreshold", "0");
        defaultSettings.put("TextEntryMode", "2");
        defaultSettings.put("Sensitivity", "20");
        defaultSettings.put("AccessToken", "");
        defaultSettings.put("AccessTokenSecret", "");
        defaultSettings.put("RequestToken", "");
        defaultSettings.put("RequestTokenSecret", "");
    }


    public void getSettings() {

        // get string values
        language = getString("Language");
        lightingThreshold = Integer.parseInt(getString("LightingThreshold"));
        textEntryMode = Integer.parseInt(getString("TextEntryMode"));
        sensitivity = Integer.parseInt(getString("Sensitivity"));
        accessToken = getString("AccessToken");
        accessTokenSecret = getString("AccessTokenSecret");
        requestToken = getString("RequestToken");
        requestTokenSecret = getString("RequestTokenSecret");

        // get calibration files
        if (getCalibrationFiles(mContext, leftEyeFileNames) != null) {
            leftCalibrationData = getCalibrationFiles(mContext, leftEyeFileNames);
        } else {
            leftCalibrationData = new Bitmap[6];
        }
        if (getCalibrationFiles(mContext, rightEyeFileNames) != null) {
            rightCalibrationData = getCalibrationFiles(mContext, rightEyeFileNames);
        } else {
            rightCalibrationData = new Bitmap[6];
        }
    }

    public String getString(String name) { // read
        Preferences.Key<String> preference = PreferencesKeys.stringKey(name);
        Flowable<String> flowable = dataStore.data().map(prefs -> {
            if (prefs.get(preference) != null) { // data found
                return prefs.get(preference);
            } else { // data not found, set to default
                String defaultValue = defaultSettings.get(name);
                Log.d("UserSettings", "Data not found for " + name + ", setting default value to " + defaultValue);
                setString(name, defaultValue); // init data store
                return defaultValue;
            }
        });
        String result = flowable.firstElement().blockingGet();
        Log.d("UserSettings", "Value of " + name + " is " + result);
        return result;
    }

    public void setString(String name, String value) { // write
        Preferences.Key<String> preference = PreferencesKeys.stringKey(name); // get the key first
        dataStore.updateDataAsync(prefsIn -> { // update the result
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            String currentString = prefsIn.get(preference);
            Log.d("UserSettings", "Changed " + name + " from " + currentString + " to " + value);
            mutablePreferences.set(preference, value);
            return Single.just(mutablePreferences);
        });
    }

    public void storeCalibrationFiles(Context context, String[] fileNames, Bitmap[] bitmaps) {
        File directory = context.getFilesDir();
        for (int i = 0; i < fileNames.length; i++) {
            File file = new File(directory, fileNames[i]);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                bitmaps[i].compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public boolean checkCalibrationFiles() { // check validity of calibration files
        if (leftCalibrationData == null || rightCalibrationData == null) return false;
        if (leftCalibrationData.length != calibrationTemplateNum || rightCalibrationData.length != calibrationTemplateNum) return false;
        else {
            for (int i = 0; i < calibrationTemplateNum; i++) {
                if (leftCalibrationData[i] == null || rightCalibrationData[i] == null) return false;
            }
        }
        return true;
    }

    public void storeCalibrationFile(Context context, String fileName, Bitmap bitmap) {
        File directory = context.getFilesDir();
        File file = new File(directory, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap[] getCalibrationFiles(Context context, String[] fileNames) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap[] bitmaps = new Bitmap[fileNames.length];
        File internalStorageDir = context.getFilesDir();
        String internalStoragePath = internalStorageDir.getAbsolutePath();

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        for (int i = 0; i < fileNames.length; i++) {
            String imagePath = internalStoragePath + File.separator + fileNames[i];
            File file = new File(imagePath);
            if (file.exists()) { // the file exists
                bitmaps[i] = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            } else { // some or all calibration data are missing
                return null;
            }
        }
        return bitmaps;
    }

    public String gazeIndexToString(int index) {
        return reference[index];
    }

    // Getter and Setter

    public String getLanguage() {return language; }
    public void setLanguage(String language) {
        this.language = language;
        setString("Language", language);
    }

    public int getCalibrationState() {
        return calibrationState;
    }

    public void setCalibrationState(int calibrationState) {
        this.calibrationState = calibrationState;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        setString("AccessToken", accessToken);
    }
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
        setString("AccessTokenSecret", accessTokenSecret);
    }
    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    public void setRequestToken(String requestToken) {
        this.requestToken = requestToken;
        setString("RequestToken", requestToken);
    }
    public String getRequestToken() {
        return requestToken;
    }

    public void setRequestTokenSecret(String requestTokenSecret) {
        this.requestTokenSecret = requestTokenSecret;
        setString("RequestTokenSecret", requestTokenSecret);
    }
    public String getRequestTokenSecret() {
        return requestTokenSecret;
    }

    public String getTextEntryContext() {
        return textEntryContext;
    }
    public void setTextEntryContext(String textEntryContext) {
        this.textEntryContext = textEntryContext;
    }

    public int getLightingThreshold() {
        return lightingThreshold;
    }
    public void setLightingThreshold(int lightingThreshold) {
        this.lightingThreshold = lightingThreshold;
        setString("LightingThreshold", Integer.toString(lightingThreshold));
    }

    public int getTextEntryMode() {
        return textEntryMode;
    }
    public void setTextEntryMode(int textEntryMode) {
        this.textEntryMode = textEntryMode;
        setString("TextEntryMode", Integer.toString(textEntryMode));
    }

    public int getSensitivity() {
        return sensitivity;
    }
    public void setSensitivity(int sensitivity) {
        this.sensitivity = sensitivity;
        setString("Sensitivity", Integer.toString(sensitivity));
    }

    public Bitmap[] getLeftCalibrationData() {
        return leftCalibrationData;
    }
    public void setLeftCalibrationData(Bitmap leftCalibrationData, int index) {
        this.leftCalibrationData[index] = leftCalibrationData;
        storeCalibrationFile(mContext, leftEyeFileNames[index], leftCalibrationData);
    }

    public Bitmap[] getRightCalibrationData() {
        return rightCalibrationData;
    }

    public void setRightCalibrationData(Bitmap rightCalibrationData, int index) {
        this.rightCalibrationData[index] = rightCalibrationData;
        storeCalibrationFile(mContext, rightEyeFileNames[index], rightCalibrationData);
    }
}
