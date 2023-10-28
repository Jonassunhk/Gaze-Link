package com.demo.opencv;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Flowable;
import io.reactivex.Single;

public class UserDataManager extends Application {
    RxDataStore<androidx.datastore.preferences.core.Preferences> dataStore;
    public void initialize(Context mContext) { // initialize the shared preferences
        dataStore = new RxPreferenceDataStoreBuilder(mContext, "settings").build();
        // set default values for all preferences:
        setInt("Sensitivity", 100); // determines how sensitive the detection will mark something as a gaze input
        setInt("Detection Interval", 12); // determines how may frames the app will consider to detect one gaze input
    }

    public int getInt(String name) { // read
        Preferences.Key<Integer> preference = PreferencesKeys.intKey(name);
        Flowable<Integer> flowable = dataStore.data().map(prefs -> prefs.get(preference));
        int result = flowable.firstElement().blockingGet();
        Log.d("UserSettings", "Value of " + name + " is " + result);
        return result;
    }

    public void setInt(String name, int value) { // write

        Preferences.Key<Integer> preference = PreferencesKeys.intKey(name); // get the key first

        dataStore.updateDataAsync(prefsIn -> { // update the result
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            Integer currentInt = prefsIn.get(preference);
            Log.d("UserSettings", "Changed " + name + " from " + currentInt + " to " + value);
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
}
