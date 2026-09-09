package com.xaniihub.app.di

import com.xaniihub.app.data.repository.XaniiRepositoryImpl
import com.xaniihub.app.domain.repository.XaniiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: XaniiRepositoryImpl): XaniiRepository
}
