package com.demo.opencv;

import android.content.ClipData;
import android.graphics.Bitmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UIViewModel extends ViewModel {
    private final MutableLiveData<GazeInput> devImages = new MutableLiveData<>();
    public void selectItem(GazeInput item) {
        devImages.setValue(item);
    }
    public LiveData<GazeInput> getSelectedItem() {
        return devImages;
    }
}
