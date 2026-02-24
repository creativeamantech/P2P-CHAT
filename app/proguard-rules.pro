# Keep WebRTC
-keep class org.webrtc.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }

# Keep Bouncy Castle
-keep class org.bouncycastle.** { *; }

# Keep Room
-keep @androidx.room.Entity class *
-keep interface * extends androidx.room.RoomDatabase

# Keep Hilt
-keep class dagger.hilt.** { *; }
