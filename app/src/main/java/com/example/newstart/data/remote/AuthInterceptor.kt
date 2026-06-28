package com.example.newstart.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = FirebaseAuth.getInstance().currentUser
        
        val token = user?.let {
            try {
                // Get token synchronously
                val task = it.getIdToken(true)
                val result = Tasks.await(task)
                result.token
            } catch (e: Exception) {
                null
            }
        }

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        return chain.proceed(requestBuilder.build())
    }
}
