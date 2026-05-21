package com.example.floginsignup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

// singleton wrapper around firebase so we don't keep calling getInstance everywhere
public class FirebaseServices {
    private static FirebaseServices instance;
    private FirebaseAuth auth;
    private FirebaseFirestore fire;
    private FirebaseStorage storage;

    public FirebaseServices(){
        // grab the firebase stuff we need
        auth = FirebaseAuth.getInstance();
        fire = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public FirebaseAuth getAuth() {
        return auth;
    }


    public FirebaseFirestore getFire() {
        return fire;
    }


    public FirebaseStorage getStorage() {
        return storage;
    }


    // make sure we only ever have one instance
    public static FirebaseServices getInstance(){
        if (instance == null){
            instance = new FirebaseServices();
        }
        return instance;
    }

}
