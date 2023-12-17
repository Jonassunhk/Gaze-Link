package com.demo.opencv;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void addNICData(List<Double> LeftNIC, List<Double> RightNIC, List<String> GazeType) {

        Map<String, Object> docData = new HashMap<>(); // the document stores each frame of NIC, gaze type, and blink.
        docData.put("LeftNIC", LeftNIC);
        docData.put("RightNIC", RightNIC);
        docData.put("GazeType", GazeType); // other than the gazes, this also includes blink and invalid (eye input is invalid)

        db.collection("demoData").document("demo1")
                .set(docData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("Firestore", "Document successfully written!");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("Firestore", "Error writing document", e);
                    }
                });
    }

}
