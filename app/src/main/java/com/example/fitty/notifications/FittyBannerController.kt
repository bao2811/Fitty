package com.example.fitty.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FittyBannerController @Inject constructor() {
    private val _banner = MutableStateFlow<FittyBannerMessage?>(null)
    val banner: StateFlow<FittyBannerMessage?> = _banner

    fun show(title: String, message: String) {
        _banner.value = FittyBannerMessage(title = title, message = message)
    }

    fun dismiss() {
        _banner.update { null }
    }
}
