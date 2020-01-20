package com.qmarciset.androidmobileui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.qmarciset.androidmobileapi.auth.AuthInfoHelper
import com.qmarciset.androidmobileapi.auth.AuthenticationState
import com.qmarciset.androidmobileapi.model.auth.AuthResponse
import com.qmarciset.androidmobileapi.network.ApiService
import com.qmarciset.androidmobileapi.repository.AuthRepository
import com.qmarciset.androidmobileapi.utils.parseJsonToType
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import timber.log.Timber

class LoginViewModel(application: Application, private val apiService: ApiService) :
    AndroidViewModel(application) {

    private val authRepository: AuthRepository = AuthRepository(apiService)

    val authInfoHelper = AuthInfoHelper.getInstance(application.applicationContext)

    open val dataLoading = MutableLiveData<Boolean>().apply { value = false }

    open val emailValid = MutableLiveData<Boolean>().apply { value = false }

    val authenticationState: MutableLiveData<AuthenticationState> by lazy {
        val initialState =
            if (authInfoHelper.sessionToken.isEmpty())
                AuthenticationState.UNAUTHENTICATED
            else
                AuthenticationState.AUTHENTICATED
        MutableLiveData<AuthenticationState>(initialState)
    }

    private fun buildAuthRequestBody(email: String, password: String): JSONObject {
        return JSONObject().apply {
            put(AuthInfoHelper.AUTH_EMAIL, email)
            put(AuthInfoHelper.AUTH_PASSWORD, password)
            put(AuthInfoHelper.AUTH_APPLICATION, authInfoHelper.appInfo)
            put(AuthInfoHelper.AUTH_DEVICE, authInfoHelper.device)
            put(AuthInfoHelper.AUTH_TEAM, authInfoHelper.team)
            put(AuthInfoHelper.AUTH_LANGUAGE, authInfoHelper.language)
            put(AuthInfoHelper.AUTH_PARAMETERS, JSONObject())
        }
    }

    fun login(email: String = "", password: String = "") {
        dataLoading.value = true
        val authRepository = AuthRepository(apiService)
        val authRequest = buildAuthRequestBody(email, password)
        authRepository.authenticate(authRequest) { isSuccess, response, error ->
            dataLoading.value = false
            if (isSuccess) {
                response?.let {
                    if (treatLoginInfo(it)) {
                        authenticationState.postValue(AuthenticationState.AUTHENTICATED)
                        return@authenticate
                    }
                }
                authenticationState.postValue(AuthenticationState.INVALID_AUTHENTICATION)
            } else {
                // TODO : check error from response or from error
                Timber.e("Error: $error")
                authenticationState.postValue(AuthenticationState.INVALID_AUTHENTICATION)
            }
        }
    }

    private fun treatLoginInfo(response: Response<ResponseBody>): Boolean {

        val responseBody = response.body()
        val json = responseBody?.string()
        val authResponse: AuthResponse? = Gson().parseJsonToType<AuthResponse>(json)
        authResponse?.let {
            authInfoHelper.sessionId = authResponse.id ?: ""
            authInfoHelper.sessionToken = authResponse.token ?: ""
            return true
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        authRepository.disposable.dispose()
    }

    class LoginViewModelFactory(
        private val application: Application,
        private val apiService: ApiService
    ) : ViewModelProvider.NewInstanceFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return LoginViewModel(application, apiService) as T
        }
    }
}
