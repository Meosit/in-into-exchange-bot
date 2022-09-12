package org.mksn.inintobot.misc

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.ServiceOptions
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient


object FirestoreHolder {

    val INSTANCE: Firestore

    init {
        if (FirebaseApp.getApps().find { it.name == FirebaseApp.DEFAULT_APP_NAME } == null) {
            val credentials: GoogleCredentials = GoogleCredentials.getApplicationDefault()
            val options: FirebaseOptions = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(ServiceOptions.getDefaultProjectId())
                .build()
            FirebaseApp.initializeApp(options)
        }
        INSTANCE = FirestoreClient.getFirestore()
    }
}
