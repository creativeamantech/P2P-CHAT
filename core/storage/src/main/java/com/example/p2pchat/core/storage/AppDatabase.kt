package com.example.p2pchat.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.p2pchat.core.storage.dao.*
import com.example.p2pchat.core.storage.entity.*
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        MessageEntity::class,
        ThreadEntity::class,
        TopicEntity::class,
        MessageTopicCrossRef::class,
        PeerEntity::class,
        RatchetStateEntity::class,
        ThreadParticipantEntity::class,
        MessageFtsEntity::class,
        OutboxEntity::class,
        AttachmentEntity::class,
        IdentityEntity::class,
        SenderKeyEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao
    abstract fun topicDao(): TopicDao
    abstract fun peerDao(): PeerDao
    abstract fun ratchetStateDao(): RatchetStateDao
    abstract fun outboxDao(): OutboxDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun messageDaoWithAttachments(): MessageDaoWithAttachments
    abstract fun identityDao(): IdentityDao
    abstract fun threadParticipantDao(): ThreadParticipantDao
    abstract fun senderKeyDao(): SenderKeyDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(context, AppDatabase::class.java, "p2pchat.db")
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
