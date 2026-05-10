package com.example.settled.ui.widget

import com.example.settled.domain.repository.CardRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun cardRepository(): CardRepository
}
