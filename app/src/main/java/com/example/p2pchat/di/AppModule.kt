package com.example.p2pchat.di

import android.content.Context
import com.example.p2pchat.core.crypto.CryptoManager
import com.example.p2pchat.core.crypto.CryptoManagerImpl
import com.example.p2pchat.core.network.P2PTransport
import com.example.p2pchat.core.network.WifiDirectTransport
import com.example.p2pchat.core.storage.AppDatabase
import com.example.p2pchat.core.storage.dao.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCryptoManager(impl: CryptoManagerImpl): CryptoManager

    @Binds
    @Singleton
    abstract fun bindP2PTransport(impl: WifiDirectTransport): P2PTransport

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context,
            cryptoManager: CryptoManager
        ): AppDatabase {
            val passphrase = cryptoManager.getDatabasePassphrase()
            return AppDatabase.create(context, passphrase)
        }

        @Provides
        @Singleton
        fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

        @Provides
        @Singleton
        fun provideThreadDao(db: AppDatabase): ThreadDao = db.threadDao()

        @Provides
        @Singleton
        fun providePeerDao(db: AppDatabase): PeerDao = db.peerDao()

        @Provides
        @Singleton
        fun provideTopicDao(db: AppDatabase): TopicDao = db.topicDao()

        @Provides
        @Singleton
        fun provideRatchetStateDao(db: AppDatabase): RatchetStateDao = db.ratchetStateDao()

        @Provides
        @Singleton
        fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()

        @Provides
        @Singleton
        fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

        @Provides
        @Singleton
        fun provideMessageDaoWithAttachments(db: AppDatabase): MessageDaoWithAttachments = db.messageDaoWithAttachments()

        @Provides
        @Singleton
        fun provideIdentityDao(db: AppDatabase): IdentityDao = db.identityDao()
    }
}
