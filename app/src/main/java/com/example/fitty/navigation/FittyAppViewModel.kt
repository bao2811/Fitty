package com.example.fitty.navigation

import androidx.lifecycle.ViewModel
import com.example.fitty.notifications.FittyBannerController
import com.example.fitty.notifications.FittyBannerMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class FittyAppViewModel @Inject constructor(
    private val bannerController: FittyBannerController
) : ViewModel() {
    val banner: StateFlow<FittyBannerMessage?> = bannerController.banner

    fun dismissBanner() {
        bannerController.dismiss()
    }
}
