package org.mksn.inintobot.misc

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions


object FirestoreHolder {

    val INSTANCE: Firestore = FirestoreOptions.getDefaultInstance().toBuilder()
        .setCredentials(GoogleCredentials.getApplicationDefault())
        .build().service

}
