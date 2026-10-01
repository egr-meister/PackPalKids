package com.packpal.kids.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.packpal.kids.AppContainer
import com.packpal.kids.PackPalApp

/** Manual DI: builds a ViewModel from the app container and the navigation SavedStateHandle. */
inline fun <reified VM : ViewModel> packPalFactory(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
) = viewModelFactory {
    initializer {
        val app = this[APPLICATION_KEY] as PackPalApp
        create(app.container, createSavedStateHandle())
    }
}

fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as PackPalApp).container
