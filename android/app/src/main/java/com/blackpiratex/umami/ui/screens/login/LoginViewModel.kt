package com.blackpiratex.umami.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpiratex.umami.data.api.UmamiApiService
import com.blackpiratex.umami.data.api.models.LoginRequest
import com.blackpiratex.umami.data.preferences.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "https://umami-chi-topaz.vercel.app",
    val username: String = "mateuscelestino",
    val password: String = "",
    val apiToken: String = "AZ3LnzBh1mE9H1wO86LIGs8VJlSaY7rEPJPng9ikgODVBTh61e8gjMWigMtfFrKm3AGvbbi7bk1U7LT47G9OFrLS5EeCk6vJ6MtJQti2S/Kc87Q1RBJBKCigTVAl3ps6nYQy+b78cvWeFF3PT8W1SmzDobmRg0inMuonkcboe9k6TkakKdpAzE7P51fHBpQc9m3/ZL8Fz52hq/BoW8Xlm5OSNSQ4d5JsOZilbf3MvPXIvbOM3Nh21NBDlYc+YbFNUn1MLVOedthXjUMNj9AmpQKxeF0kFO4cg/OvYcENg6y/Q4M5iSa+fQRntgZwo2l2LiPIjPEi0NwPIz7QgXwBVUMFPJc6GdrSMogm1t20yHzP0rM+gU+BmTBHNLL9y+9oD+MDMCm3jEaLqnst5FMp97EuWpqUorgOeDE14rtkc8WZIlMpsWb23roijiuRHdQGrfIt9l9gSpF3P/E8jl7wiNcZRFAlS2b9goZDouWb1oGJSGJldEPTBnuFaXUwZgK8mLF88KKb+xt59gTqnjwtnZ19gvqdGo7gnw7ug1an1biH94t91oVSSQ6vfN8uzwnU3GmMgUzEBxf04a5U02dzqpQIojodTDX4kfXljg5KmACtAUwO",
    val isTokenMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val apiService: UmamiApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        sessionManager.getServerUrl()?.let { url ->
            _uiState.value = _uiState.value.copy(serverUrl = url)
        }
        sessionManager.getUsername()?.let { user ->
            _uiState.value = _uiState.value.copy(username = user)
        }
    }

    fun onServerUrlChange(value: String) {
        _uiState.value = _uiState.value.copy(serverUrl = value, errorMessage = null)
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun onTokenChange(value: String) {
        _uiState.value = _uiState.value.copy(apiToken = value, errorMessage = null)
    }

    fun onTokenModeChange(isToken: Boolean) {
        _uiState.value = _uiState.value.copy(isTokenMode = isToken, errorMessage = null)
    }

    fun setServerUrlPreset(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url, errorMessage = null)
    }

    fun login() {
        val url = _uiState.value.serverUrl.trim()
        val isTokenMode = _uiState.value.isTokenMode

        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Por favor informe a URL do servidor")
            return
        }

        if (isTokenMode) {
            val token = _uiState.value.apiToken.trim()
            if (token.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Por favor informe o token / API Key")
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                try {
                    val websites = apiService.getWebsites(url, token)
                    sessionManager.saveSession(url, token, "Cloud User")
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Falha ao validar token na instância Umami"
                    )
                }
            }
        } else {
            val user = _uiState.value.username.trim()
            val pwd = _uiState.value.password

            if (user.isBlank() || pwd.isBlank()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Por favor informe usuário e senha")
                return
            }

            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                try {
                    val response = apiService.login(url, LoginRequest(user, pwd))
                    val token = response.token
                    if (!token.isNullOrBlank()) {
                        sessionManager.saveSession(url, token, user)
                        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Credenciais inválidas ou resposta inesperada do servidor"
                        )
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Falha ao conectar com o servidor Umami"
                    )
                }
            }
        }
    }
}
