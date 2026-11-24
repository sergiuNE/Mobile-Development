package be.ap.nowaste.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import be.ap.nowaste.data.favorite.FavoriteRepository
import be.ap.nowaste.data.local.AppDatabase
import be.ap.nowaste.data.local.FavoriteStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    /** True until Room gave the first list. */
    val isLoading: Boolean = true,
    val favorites: List<FavoriteStore> = emptyList(),
)

/** K12: reads the favorite stores from Room only. */
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FavoriteRepository(AppDatabase.get(application).favoriteStoreDao())

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavorites().collect { favorites -> _uiState.update { FavoritesUiState(isLoading = false, favorites = favorites) } }
        }
    }

    fun onRemove(storeId: String) {
        viewModelScope.launch { repository.remove(storeId) }
    }
}
