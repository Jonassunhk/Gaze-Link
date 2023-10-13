package com.demo.opencv;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UIViewModel extends ViewModel {
    private final MutableLiveData<DetectionOutput> devImages = new MutableLiveData<>();
    public void selectItem(DetectionOutput item) {
        devImages.setValue(item);
    }
    public LiveData<DetectionOutput> getSelectedItem() {
        return devImages;
    }
}
