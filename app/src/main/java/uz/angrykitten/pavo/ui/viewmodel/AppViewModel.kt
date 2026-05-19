package uz.angrykitten.pavo.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.angrykitten.pavo.data.model.Animal
import uz.angrykitten.pavo.data.model.City
import uz.angrykitten.pavo.data.model.District
import uz.angrykitten.pavo.data.repository.AuthRepository
import uz.angrykitten.pavo.data.repository.AuthResult
import uz.angrykitten.pavo.data.repository.AnimalRepository
import uz.angrykitten.pavo.data.repository.SupabaseRepository
import uz.angrykitten.pavo.data.repository.SupabaseUser
import uz.angrykitten.pavo.data.repository.UserRepository

data class FilterState(
    val listingType: String? = null,
    val animalType: String? = null,
    val cityId: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val query: String = ""
) {
    fun hasActiveConstraints(): Boolean =
        listingType != null ||
            animalType != null ||
            cityId != null ||
            minPrice != null ||
            maxPrice != null ||
            query.isNotBlank()
}

/** UI state for authentication operations */
data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val phoneVerificationId: String? = null,
    val phoneStep: PhoneStep = PhoneStep.ENTER_NUMBER
)

enum class PhoneStep { ENTER_NUMBER, ENTER_OTP }

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val animalRepo = AnimalRepository(application)
    val userRepo = UserRepository(application)
    private val authRepo = AuthRepository()
    private val supabaseRepo = SupabaseRepository()
    private val dataVersion = MutableStateFlow(0)

    // ─── Auth UI State ───────────────────────────────────────────────────────
    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState

    // ─── DataStore Auth State ────────────────────────────────────────────────
    val isLoggedIn: StateFlow<Boolean> = userRepo.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userName: StateFlow<String?> = userRepo.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userEmail: StateFlow<String?> = userRepo.userEmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userAvatar: StateFlow<String?> = userRepo.userAvatar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userId: StateFlow<String?> = userRepo.userId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userPhone: StateFlow<String?> = userRepo.userPhone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ─── Saved animals ────────────────────────────────────────────────────
    val savedIds: StateFlow<Set<String>> = userRepo.savedPropertyIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ─── Remote loading state ────────────────────────────────────────────────
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError

    // ─── Reactive city / district lists (update whenever remote data loads) ──
    val cityList: StateFlow<List<City>> = dataVersion.map {
        animalRepo.getCities()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── Filter state ────────────────────────────────────────────────────────
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    init {
        refreshRemoteContent()
    }

    // ─── Home feed ───────────────────────────────────────────────────────────
    val homeAnimals: StateFlow<List<Animal>> = combine(
        _filterState, savedIds, dataVersion
    ) { filter, saved, _ ->
        animalRepo.getAnimals(
            listingType = filter.listingType,
            animalType = filter.animalType,
            cityId = filter.cityId,
            minPrice = filter.minPrice,
            maxPrice = filter.maxPrice,
            query = filter.query.ifBlank { null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), animalRepo.getAnimals())

    val savedAnimals: StateFlow<List<Animal>> = combine(savedIds, dataVersion) { ids, _ ->
        animalRepo.getAnimals(savedIds = ids, onlySaved = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val searchResults: StateFlow<List<Animal>> = combine(_searchQuery, _filterState, dataVersion) { q, filter, _ ->
        animalRepo.getAnimals(
            listingType = filter.listingType,
            animalType = filter.animalType,
            cityId = filter.cityId,
            minPrice = filter.minPrice,
            maxPrice = filter.maxPrice,
            query = q.ifBlank { null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ════════════════════════════════════════════════════════════════════════
    // Real Firebase Auth Actions
    // ════════════════════════════════════════════════════════════════════════

    /** Register with email + password via Firebase */
    fun registerWithEmail(
        name: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _authUiState.value = _authUiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepo.registerWithEmail(email, password)) {
                is AuthResult.Success -> {
                    val user = result.user
                    persistUserLocally(
                        uid = user.uid,
                        name = name.ifBlank { user.displayName ?: "Foydalanuvchi" },
                        email = user.email ?: email,
                        avatar = user.photoUrl?.toString() ?: ""
                    )
                    syncUserToSupabase(uid = user.uid, name = name, email = user.email ?: email)
                    _authUiState.value = _authUiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _authUiState.value = _authUiState.value.copy(isLoading = false, error = result.message)
                    onError(result.message)
                }
            }
        }
    }

    /** Login with email + password via Firebase */
    fun loginWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _authUiState.value = _authUiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepo.loginWithEmail(email, password)) {
                is AuthResult.Success -> {
                    val user = result.user
                    persistUserLocally(
                        uid = user.uid,
                        name = user.displayName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() },
                        email = user.email ?: email,
                        avatar = user.photoUrl?.toString() ?: ""
                    )
                    _authUiState.value = _authUiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _authUiState.value = _authUiState.value.copy(isLoading = false, error = result.message)
                    onError(result.message)
                }
            }
        }
    }

    /** Sign in with a Google ID Token obtained from Credential Manager */
    fun signInWithGoogle(
        idToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        _authUiState.value = _authUiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepo.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    val user = result.user
                    persistUserLocally(
                        uid = user.uid,
                        name = user.displayName ?: "Google Foydalanuvchi",
                        email = user.email ?: "",
                        avatar = user.photoUrl?.toString() ?: ""
                    )
                    syncUserToSupabase(
                        uid = user.uid,
                        name = user.displayName,
                        email = user.email,
                        phone = user.phoneNumber
                    )
                    _authUiState.value = _authUiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _authUiState.value = _authUiState.value.copy(isLoading = false, error = result.message)
                    onError(result.message)
                }
            }
        }
    }

    /** Step 1: Send OTP to phone number */
    fun sendPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        onAutoVerified: () -> Unit
    ) {
        _authUiState.value = _authUiState.value.copy(isLoading = true, error = null)
        authRepo.sendPhoneOtp(
            phoneNumber = phoneNumber,
            activity = activity,
            onCodeSent = { verificationId ->
                _authUiState.value = _authUiState.value.copy(
                    isLoading = false,
                    phoneVerificationId = verificationId,
                    phoneStep = PhoneStep.ENTER_OTP
                )
            },
            onAutoVerified = { credential ->
                viewModelScope.launch {
                    signInWithPhoneCredential(
                        credential = credential,
                        onSuccess = {
                            _authUiState.value = AuthUiState()
                            onAutoVerified()
                        },
                        onError = { msg ->
                            _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg)
                        }
                    )
                }
            },
            onError = { msg ->
                _authUiState.value = _authUiState.value.copy(isLoading = false, error = msg)
            }
        )
    }

    /** Step 2: Verify OTP code entered by user */
    fun verifyPhoneOtp(
        code: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val verificationId = _authUiState.value.phoneVerificationId ?: run {
            onError("Tasdiqlash ID topilmadi")
            return
        }
        _authUiState.value = _authUiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val result = authRepo.verifyPhoneCode(verificationId, code)) {
                is AuthResult.Success -> {
                    val user = result.user
                    persistUserLocally(
                        uid = user.uid,
                        name = user.displayName ?: "Foydalanuvchi",
                        email = user.email ?: "",
                        avatar = user.photoUrl?.toString() ?: ""
                    )
                    syncUserToSupabase(
                        uid = user.uid,
                        name = user.displayName,
                        email = user.email,
                        phone = user.phoneNumber
                    )
                    _authUiState.value = AuthUiState()
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _authUiState.value = _authUiState.value.copy(isLoading = false, error = result.message)
                    onError(result.message)
                }
            }
        }
    }

    private suspend fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        when (val result = authRepo.signInWithPhoneCredential(credential)) {
            is AuthResult.Success -> {
                val user = result.user
                persistUserLocally(
                    uid = user.uid,
                    name = user.displayName ?: "Foydalanuvchi",
                    email = user.email ?: "",
                    avatar = user.photoUrl?.toString() ?: ""
                )
                syncUserToSupabase(
                    uid = user.uid,
                    name = user.displayName,
                    email = user.email,
                    phone = user.phoneNumber,
                    avatarUrl = user.photoUrl?.toString()
                )
                onSuccess()
            }
            is AuthResult.Error -> onError(result.message)
        }
    }

    fun resetPhoneStep() {
        _authUiState.value = AuthUiState()
    }

    fun sendPasswordReset(email: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepo.resetPassword(email)
            onDone(result.isSuccess)
        }
    }

    fun clearAuthError() {
        _authUiState.value = _authUiState.value.copy(error = null)
    }

    // ─── Sign Out ────────────────────────────────────────────────────────────

    fun signOut() {
        authRepo.signOut()
        viewModelScope.launch { userRepo.signOut() }
    }

    // ─── Local Persistence ───────────────────────────────────────────────────

    private suspend fun persistUserLocally(uid: String, name: String, email: String, avatar: String) {
        userRepo.signIn(uid, name, email, avatar)
    }

    private fun syncUserToSupabase(
        uid: String,
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        avatarUrl: String? = null
    ) {
        viewModelScope.launch {
            supabaseRepo.upsertUser(
                SupabaseUser(
                    id = uid,
                    name = name,
                    email = email,
                    phone = phone,
                    avatar_url = avatarUrl
                )
            )
        }
    }

    // ─── Remote Content ──────────────────────────────────────────────────────

    fun refreshRemoteContent() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _refreshError.value = null
            val result = animalRepo.refreshFromSupabase(force = true)
            result.onFailure { e ->
                _refreshError.value = e.message ?: "Ma'lumotlarni yuklashda xatolik"
            }
            dataVersion.value += 1
            _isRefreshing.value = false
        }
    }

    fun clearRefreshError() { _refreshError.value = null }

    fun toggleSaved(animalId: String) {
        viewModelScope.launch {
            userRepo.toggleSaved(animalId)
            dataVersion.value += 1
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch { userRepo.updateName(name) }
    }

    fun updateUserPhone(phone: String) {
        viewModelScope.launch { userRepo.updatePhone(phone) }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepo.signOut()
            userRepo.signOut()
            onComplete()
        }
    }

    fun updateSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun updateFilter(filter: FilterState) {
        _filterState.value = filter
    }

    fun clearFilter() {
        _filterState.value = FilterState()
    }

    // ─── Listing helpers ─────────────────────────────────────────────────────

    fun getCities(): List<City> = animalRepo.getCities()

    fun getDistricts(cityId: Int? = null): List<District> = animalRepo.getDistricts(cityId)

    suspend fun uploadImage(uri: Uri): Result<String> = animalRepo.uploadImage(uri)

    fun postListing(animal: Animal) {
        viewModelScope.launch {
            animalRepo.addAnimal(animal)
            dataVersion.value += 1
        }
    }

    fun getAllAnimals(): List<Animal> = animalRepo.getAnimals()

    fun getUserAnimals(): List<Animal> {
        val uid = userId.value ?: return emptyList()
        return animalRepo.getUserAnimals(uid)
    }

    fun deleteAnimal(id: String) {
        viewModelScope.launch {
            animalRepo.deleteAnimal(id)
            dataVersion.value += 1
        }
    }
}
