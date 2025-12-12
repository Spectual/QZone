package com.qzone.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qzone.data.model.UserCoupon
import com.qzone.domain.repository.RewardRepository
import com.qzone.util.QLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WalletRewardItem(
    val id: String,
    val name: String,
    val deductedPoints: Int,
    val redeemedAt: String,
    val updatedAt: String?
)

data class WalletUiState(
    val isLoading: Boolean = true,
    val rewards: List<WalletRewardItem> = emptyList(),
    val errorMessage: String? = null
)

class WalletViewModel(
    private val rewardRepository: RewardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()
    private var activeJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _uiState.value = WalletUiState(isLoading = true)
            try {
                val coupons = rewardRepository.getUserCoupons()
                _uiState.value = WalletUiState(
                    isLoading = false,
                    rewards = coupons.map { it.toWalletItem() }
                )
            } catch (t: Throwable) {
                QLog.e(TAG, t) { "Failed to load coupon history" }
                _uiState.value = WalletUiState(
                    isLoading = false,
                    rewards = emptyList(),
                    errorMessage = t.message ?: "Unable to load wallet."
                )
            }
        }
    }

    private fun UserCoupon.toWalletItem(): WalletRewardItem =
        WalletRewardItem(
            id = id,
            name = couponName,
            deductedPoints = deductedPoints,
            redeemedAt = createdAt,
            updatedAt = updatedAt
        )

    companion object {
        private const val TAG = "WalletViewModel"

        fun factory(rewardRepository: RewardRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WalletViewModel(rewardRepository) as T
                }
            }
    }
}
