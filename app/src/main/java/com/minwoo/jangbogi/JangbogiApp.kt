package com.minwoo.jangbogi

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.minwoo.jangbogi.data.JangbogiDatabase
import com.minwoo.jangbogi.data.JangbogiRepository
import com.minwoo.jangbogi.data.MIGRATION_1_2
import com.minwoo.jangbogi.ui.viewmodel.HomeViewModel
import com.minwoo.jangbogi.ui.viewmodel.ListViewModel

class JangbogiApp : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {

    private val database = Room.databaseBuilder(
        context.applicationContext,
        JangbogiDatabase::class.java,
        "jangbogi.db"
    ).addMigrations(MIGRATION_1_2).build()

    private val repository = JangbogiRepository(database)

    fun homeViewModelFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
    }

    fun listViewModelFactory(listId: Long): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ListViewModel(repository, listId) as T
        }
    }
}
