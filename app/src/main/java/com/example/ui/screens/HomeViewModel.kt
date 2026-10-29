package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.GameApplication
import com.example.data.UserProfile
import com.example.data.UserRepository
import com.example.game.BoosterType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: UserRepository) : ViewModel() {
    val userProfile = repository.userProfile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserProfile()
    )

    val allLevelProgress = repository.allLevelProgress

    fun toggleSound() {
        viewModelScope.launch {
            repository.toggleSound()
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            repository.toggleMusic()
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleHaptics(enabled)
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.setLanguage(languageCode)
        }
    }

    fun markNotificationPermissionAsked() {
        viewModelScope.launch {
            repository.markNotificationPermissionAsked()
        }
    }

    fun refillLives() {
        viewModelScope.launch {
            repository.refillLives(100)
        }
    }

    fun refillLivesFree() {
        viewModelScope.launch {
            repository.refillLivesFree()
        }
    }

    fun grantBoosterReward(booster: BoosterType, count: Int = 1) {
        viewModelScope.launch {
            repository.grantBoosterReward(booster, count)
        }
    }

    fun buyBooster(booster: BoosterType, cost: Int) {
        viewModelScope.launch {
            repository.buyBooster(booster, cost)
        }
    }

    fun addCoins(amount: Int) {
        viewModelScope.launch {
            repository.addCoins(amount)
        }
    }

    fun addDiamonds(amount: Int) {
        viewModelScope.launch {
            repository.addDiamonds(amount)
        }
    }

    fun buyDiamondsPack(diamonds: Int, bonusCoins: Int = 0) {
        viewModelScope.launch {
            repository.buyDiamondsPack(diamonds, bonusCoins)
        }
    }

    fun buyCoinsWithDiamonds(coinsAmount: Int, diamondCost: Int) {
        viewModelScope.launch {
            repository.buyCoinsWithDiamonds(coinsAmount, diamondCost)
        }
    }

    fun buyHighValueBooster(booster: BoosterType, count: Int, diamondCost: Int) {
        viewModelScope.launch {
            repository.buyHighValueBoosterWithDiamonds(booster, count, diamondCost)
        }
    }

    fun activateInfiniteLives(hours: Int = 2, diamondCost: Int = 15) {
        viewModelScope.launch {
            repository.activateInfiniteLivesWithDiamonds(hours, diamondCost)
        }
    }

    fun subscribeVip(days: Int = 7) {
        viewModelScope.launch {
            repository.subscribeVip(days)
        }
    }

    fun claimVipDailyDiamonds() {
        viewModelScope.launch {
            repository.claimVipDailyDiamonds()
        }
    }

    fun skipLevelWithDiamonds(levelId: Int, cost: Int = 30) {
        viewModelScope.launch {
            repository.skipLevelWithDiamonds(levelId, cost)
        }
    }

    fun skipLevel(levelId: Int) {
        skipLevelWithDiamonds(levelId)
    }

    // --- Piggy Bank & Lucky Wheel Actions ---
    fun smashPiggyBank(onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val amount = repository.smashPiggyBank()
            onSuccess(amount)
        }
    }

    fun claimLuckyWheelReward(rewardIndex: Int, onResult: (Pair<String, Int>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.claimLuckyWheelReward(rewardIndex)
            onResult(res)
        }
    }

    fun grantFreeLuckySpin() {
        viewModelScope.launch {
            repository.grantFreeLuckySpin()
        }
    }

    // --- Authentication Actions ---
    fun loginWithGoogle(email: String, displayName: String, photoUrl: String? = null) {
        viewModelScope.launch {
            repository.loginWithGoogle(email, displayName, photoUrl)
        }
    }

    fun loginWithEmail(email: String, displayName: String? = null) {
        viewModelScope.launch {
            repository.loginWithEmail(email, displayName)
        }
    }

    fun loginWithPhone(phoneNumber: String) {
        viewModelScope.launch {
            repository.loginWithPhone(phoneNumber)
        }
    }

    fun loginWithSocial(provider: String, name: String) {
        viewModelScope.launch {
            repository.loginWithSocial(provider, name)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount()
        }
    }

    // --- Payment & Alternative Checkout ---
    fun recordPurchase(method: String, paypalEmail: String? = null) {
        viewModelScope.launch {
            repository.recordPurchase(method, paypalEmail)
        }
    }

    // --- Anti-Cheat & Security ---
    fun clearSecurityAlert() {
        viewModelScope.launch {
            repository.clearSecurityAlert()
        }
    }

    // --- Developer & QA Sandbox Controls ---
    fun unlockLevels(upToLevel: Int) {
        viewModelScope.launch {
            repository.unlockLevels(upToLevel)
        }
    }

    fun grantAllBoosters(count: Int = 5) {
        viewModelScope.launch {
            repository.grantAllBoosters(count)
        }
    }

    fun grantInfiniteLives(hours: Int = 2) {
        viewModelScope.launch {
            repository.grantInfiniteLives(hours)
        }
    }

    fun grantSandboxRemoveAds() {
        viewModelScope.launch {
            repository.setHasRemovedAds(true)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GameApplication)
                HomeViewModel(UserRepository(app.database.userDao()))
            }
        }
    }
}
