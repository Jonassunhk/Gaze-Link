package com.demo.opencv;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UIViewModel extends ViewModel {
    private final MutableLiveData<AppLiveData> devImages = new MutableLiveData<>();
    public void selectItem(AppLiveData item) {
        devImages.setValue(item);
    }
    public LiveData<AppLiveData> getSelectedItem() {
        return devImages;
    }
}
