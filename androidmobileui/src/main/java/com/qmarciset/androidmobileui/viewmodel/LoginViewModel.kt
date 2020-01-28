package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.auth.buildAuthRequestBody
import com.qmarciset.androidmobileapi.auth.handleLoginInfo
import com.qmarciset.androidmobileapi.model.auth.AuthResponse
import com.qmarciset.androidmobileapi.network.LoginApiService
import com.qmarciset.androidmobileapi.repository.AuthRepository
import com.qmarciset.androidmobileapi.utils.RequestErrorHandler
import com.qmarciset.androidmobileapi.utils.parseJsonToType
import timber.log.Timber

class LoginViewModel(application: Application, loginApiService: LoginApiService) :
    AndroidViewModel(application) {

    private val authRepository: AuthRepository = AuthRepository(loginApiService)

    val authInfoHelper = AuthInfoHelper.getInstance(application.applicationContext)

    val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    val emailValid = MutableLiveData<Boolean>().apply { value = false }

    val authenticationState: MutableLiveData<AuthenticationState> by lazy {
        val initialState =
            if (authInfoHelper.sessionToken.isEmpty())
                AuthenticationState.UNAUTHENTICATED
            else
                AuthenticationState.AUTHENTICATED
        MutableLiveData<AuthenticationState>(initialState)
    }

    fun login(email: String = "", password: String = "") {
        dataLoading.value = true
        val authRequest = authInfoHelper.buildAuthRequestBody(email, password)
        val shouldRetryOnError = authInfoHelper.guestLogin
        authRepository.authenticate(authRequest, shouldRetryOnError) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.let {
                    val responseBody = response.body()
                    val json = responseBody?.string()
                    val authResponse: AuthResponse? = Gson().parseJsonToType<AuthResponse>(json)
                    authResponse?.let {
                        if (authInfoHelper.handleLoginInfo(authResponse)) {
                            authenticationState.postValue(AuthenticationState.AUTHENTICATED)
                            return@authenticate
                        }
                    }
                }
                authenticationState.postValue(AuthenticationState.INVALID_AUTHENTICATION)
            } else {
                RequestErrorHandler.handleError(error)
                authenticationState.postValue(AuthenticationState.INVALID_AUTHENTICATION)
            }
        }
    }

    fun disconnectUser() {
        authRepository.logout { isSuccess, _, error ->
            dataLoading.value = false
            authenticationState.postValue(AuthenticationState.UNAUTHENTICATED)
            authInfoHelper.sessionToken = ""
            if (isSuccess) {
                Timber.d("[ Logout request successful ]")
            } else {
                Timber.e("Error: $error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.disposable.dispose()
    }

    class LoginViewModelFactory(
        private val application: Application,
        private val apiService: LoginApiService
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LoginViewModel(application, apiService) as T
        }
    }
}
