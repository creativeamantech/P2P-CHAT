# P2P Encrypted Chat Application — Full-Stack Android Architecture
### Authored by a Senior Full-Stack Developer | 50 Years Experience
---

> **Document Purpose:** End-to-end technical blueprint for designing, building, and deploying a fully decentralized, encrypted, peer-to-peer Android chat application with threaded topic management — no central server required.

---

## Table of Contents

1. [Executive Overview](#1-executive-overview)
2. [Core Architecture Philosophy](#2-core-architecture-philosophy)
3. [System Architecture Diagram](#3-system-architecture-diagram)
4. [Technology Stack](#4-technology-stack)
5. [Module Breakdown](#5-module-breakdown)
6. [End-to-End Encryption Design](#6-end-to-end-encryption-design)
7. [Networking Layer — P2P Transport](#7-networking-layer--p2p-transport)
8. [Local Storage Architecture](#8-local-storage-architecture)
9. [Threading & Topic Tagging System](#9-threading--topic-tagging-system)
10. [Connection Management Engine](#10-connection-management-engine)
11. [UI/UX Architecture](#11-uiux-architecture)
12. [Data Models](#12-data-models)
13. [Database Schema](#13-database-schema)
14. [API & Interface Contracts](#14-api--interface-contracts)
15. [Security Threat Model](#15-security-threat-model)
16. [Error Handling & Resilience](#16-error-handling--resilience)
17. [Performance Strategy](#17-performance-strategy)
18. [Testing Strategy](#18-testing-strategy)
19. [Build & Deployment](#19-build--deployment)
20. [Roadmap & Future Enhancements](#20-roadmap--future-enhancements)
21. [Peer Discovery & The Chat Address System](#21-peer-discovery--the-chat-address-system)
22. [Multi-Identity & Burner Address System](#22-multi-identity--burner-address-system)
23. [Metadata Privacy & IP Anonymization](#23-metadata-privacy--ip-anonymization)

---

## 1. Executive Overview

This document defines the complete technical specification for a **serverless, peer-to-peer encrypted Android chat application**. Every design decision prioritizes:

- **Privacy by default** — No message ever touches a third-party server
- **Resilience** — Works on local network (LAN/BT) and over the internet simultaneously
- **Organization** — Topic-tagging and thread grouping are first-class citizens
- **Simplicity for the end-user** — All complexity is hidden behind intuitive UX

The application is fully self-contained on each device. Peers discover each other, negotiate encrypted channels, and exchange messages directly. All data is stored locally in an encrypted SQLite database.

---

## 2. Core Architecture Philosophy

After decades building distributed systems, the guiding principles here are:

### 2.1 The Golden Rules

1. **Zero Trust Networking** — Every peer is untrusted until cryptographically verified
2. **Local-First Data** — The device is the source of truth; the network is just a delivery mechanism
3. **Eventual Consistency** — Messages may arrive out of order; the UI reconstructs correct thread order from timestamps and parent IDs
4. **Graceful Degradation** — If internet P2P fails, fall back to Wi-Fi Direct; if that fails, fall back to Bluetooth
5. **Minimize Attack Surface** — No open ports listening by default; connections are always outbound or via rendezvous signaling only

### 2.2 What "No Central Server" Actually Means

| Concern | How It's Handled |
|---|---|
| Peer Discovery (LAN) | mDNS / NSD (Network Service Discovery) |
| Peer Discovery (Internet) | WebRTC STUN/TURN via public servers (metadata only, not messages) |
| Message Relay | Direct socket or WebRTC DataChannel — never a relay |
| Identity | Self-signed X.509 certs + Ed25519 keypair per user |
| Group Coordination | Gossip protocol for group membership sync |

---

## 3. System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID DEVICE A                        │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                  Presentation Layer                    │  │
│  │    Jetpack Compose UI  ←→  ViewModel (StateFlow)      │  │
│  └─────────────────────┬─────────────────────────────────┘  │
│                        │ UseCase calls                       │
│  ┌─────────────────────▼─────────────────────────────────┐  │
│  │                  Domain Layer                          │  │
│  │  SendMessageUseCase | TagTopicUseCase | SearchUseCase  │  │
│  └──────────┬──────────────────────────┬─────────────────┘  │
│             │                          │                     │
│  ┌──────────▼───────┐      ┌───────────▼──────────────────┐ │
│  │  Crypto Engine   │      │     Repository Layer          │ │
│  │  (E2E Encrypt)   │      │  MessageRepo | PeerRepo       │ │
│  └──────────┬───────┘      └───────────┬──────────────────┘ │
│             │                          │                     │
│  ┌──────────▼───────┐      ┌───────────▼──────────────────┐ │
│  │  Network Layer   │      │     Local Storage Layer       │ │
│  │  WebRTC |BT|WiFi │      │  Room DB (SQLCipher)          │ │
│  └──────────────────┘      └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
              │  Encrypted P2P Channel (DataChannel / Socket)
              ▼
┌─────────────────────────────────────────────────────────────┐
│                     ANDROID DEVICE B                        │
│                   (Mirror Architecture)                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Technology Stack

### 4.1 Core Android Stack

| Layer | Technology | Rationale |
|---|---|---|
| Language | **Kotlin 1.9+** | Coroutines, sealed classes, null safety |
| UI | **Jetpack Compose** | Declarative, reactive, modern |
| Architecture | **MVVM + Clean Architecture** | Testable, scalable separation of concerns |
| DI | **Hilt (Dagger2)** | Compile-time DI, Android lifecycle-aware |
| Async | **Kotlin Coroutines + Flow** | Structured concurrency, backpressure |
| Navigation | **Navigation Component** | Deep links, safe args |

### 4.2 Networking

| Component | Technology |
|---|---|
| P2P Internet | **WebRTC (Google's libwebrtc via io.getstream:stream-webrtc-android)** |
| LAN Discovery | **Android NSD (Network Service Discovery / mDNS)** |
| LAN Transport | **Raw TCP Socket over Wi-Fi Direct** |
| Bluetooth | **Android Classic BT SPP + BLE for discovery** |
| Signaling (WebRTC) | **Self-hosted or public STUN (Google's stun.l.google.com)** |

### 4.3 Cryptography

| Purpose | Algorithm |
|---|---|
| Key Exchange | **X25519 (ECDH over Curve25519)** |
| Message Encryption | **AES-256-GCM** |
| Identity Signing | **Ed25519** |
| Key Derivation | **HKDF-SHA256** |
| Password/PIN Protection | **Argon2id** |
| Certificate | **Self-signed X.509 v3** |

### 4.4 Storage

| Component | Technology |
|---|---|
| Database | **Room + SQLCipher (encrypted SQLite)** |
| Key Storage | **Android Keystore System** |
| File/Media | **Encrypted files in app-private storage** |
| Preferences | **EncryptedSharedPreferences** |

### 4.5 Build & Quality

| Tool | Purpose |
|---|---|
| Gradle KTS | Build scripts in Kotlin |
| ProGuard/R8 | Code shrinking and obfuscation |
| JUnit5 + MockK | Unit testing |
| Espresso + Compose Test | UI testing |
| Turbine | Flow testing |
| Detekt | Static analysis |

---

## 5. Module Breakdown

The project follows a **multi-module Gradle structure** for compilation speed, encapsulation, and team scalability:

```
root/
├── app/                          # Application entry point, DI graph wiring
├── core/
│   ├── core-crypto/              # All cryptographic operations
│   ├── core-network/             # Transport abstractions (WebRTC, BT, WiFi)
│   ├── core-storage/             # Room DB, DAOs, migrations
│   ├── core-model/               # Pure Kotlin data classes (no Android deps)
│   └── core-ui/                  # Shared Compose components, theme, typography
├── feature/
│   ├── feature-conversations/    # Thread list, conversation history
│   ├── feature-messaging/        # Message composer, media attachment
│   ├── feature-topics/           # Topic/tag management and filtering
│   ├── feature-peers/            # Peer discovery, connection management
│   └── feature-settings/         # Identity, keys, preferences
└── test-utils/                   # Shared testing fakes and fixtures
```

### 5.1 Module Dependency Rules

- **`core-model`** has no dependencies (pure Kotlin)
- **`core-crypto`** depends only on `core-model`
- **Feature modules** may depend on `core/*` modules but **never on each other**
- **`app`** is the only module that wires features together

---

## 6. End-to-End Encryption Design

### 6.1 Identity & Key Generation

On first launch, the app generates a permanent identity:

```kotlin
data class UserIdentity(
    val userId: String,           // UUID v4
    val displayName: String,
    val ed25519PublicKey: ByteArray,   // For signing
    val x25519PublicKey: ByteArray,    // For key exchange
    val selfSignedCert: X509Certificate
)
```

Private keys **never leave the Android Keystore**. All signing and decryption operations are performed inside the Keystore hardware boundary (on supported devices).

### 6.2 Double Ratchet Protocol (Signal-like)

Each peer conversation uses the **Double Ratchet Algorithm**:

```
Initial Handshake:
  Alice generates ephemeral X25519 keypair
  Alice sends: [PublicKey_ephemeral + PublicKey_identity + Signature]
  Bob verifies signature against Alice's known identity cert
  Bob computes SharedSecret = ECDH(Bob_private, Alice_ephemeral_public)
  Root key derived via HKDF:
    RootKey, ChainKey = HKDF(SharedSecret, "p2pchat-v1")

Per-Message:
  MessageKey = KDF(ChainKey)
  ChainKey   = KDF(ChainKey + "next")
  CipherText = AES-256-GCM(MessageKey, Plaintext, AAD=MessageID)
```

This provides:
- **Forward Secrecy** — Compromising current keys doesn't expose past messages
- **Break-in Recovery** — New ephemeral keys restore security after compromise
- **Message Authentication** — Every message is signed and verified

### 6.3 Group Messaging Encryption (Sender Keys)

For multi-party group chats:

1. Each member generates a **Sender Key** (unique per group per member)
2. Sender Keys are distributed to all group members via individual encrypted 1:1 channels
3. Messages are encrypted once with the sender's Sender Key
4. Recipients decrypt using the stored Sender Key for that group member

### 6.4 At-Rest Encryption

```kotlin
// Database opened with SQLCipher using a key derived from:
// AndroidKeystore-protected key XOR user's PIN (Argon2id hash)
val passphrase = deriveDbPassphrase(keystoreKey, userPinHash)
val db = Room.databaseBuilder(context, AppDatabase::class.java, "p2pchat.db")
    .openHelperFactory(SupportFactory(passphrase))
    .build()
```

---

## 7. Networking Layer — P2P Transport

### 7.1 Transport Abstraction

All transports implement a single interface, making them interchangeable:

```kotlin
interface P2PTransport {
    val peerId: String
    val connectionState: StateFlow<ConnectionState>
    suspend fun connect(peerDescriptor: PeerDescriptor): Result<Unit>
    suspend fun send(payload: EncryptedPayload): Result<Unit>
    fun receive(): Flow<EncryptedPayload>
    suspend fun disconnect()
}
```

### 7.2 WebRTC (Internet P2P)

WebRTC is the backbone for internet-based communication:

**Signaling Flow:**

```
Device A                  Signaling Channel (STUN only)         Device B
   │                              │                                │
   │──── SDP Offer ──────────────►│────────────────────────────►  │
   │                              │                                │
   │◄─── SDP Answer ─────────────│◄────────────────────────────── │
   │                              │                                │
   │──────────────── ICE Candidates exchanged ──────────────────── │
   │                                                               │
   │◄══════════════ WebRTC DataChannel (DTLS encrypted) ══════════►│
   │                   (Signaling server no longer involved)        │
```

**Implementation notes:**
- DataChannel is configured with `ordered = true`, `maxRetransmits = 3`
- DTLS provides transport-layer encryption; our E2E encryption adds an additional application-layer
- The signaling channel only exchanges SDP and ICE candidates — **zero message content**
- For firewalled peers, a **TURN server** (self-hosted coturn) can relay ICE but never sees decrypted content

### 7.3 Wi-Fi Direct (LAN)

```kotlin
class WifiDirectTransport : P2PTransport {
    // 1. Discover peers via WifiP2pManager
    // 2. Form group — one device becomes Group Owner (GO)
    // 3. GO opens ServerSocket on port 47890
    // 4. Client connects via GO's IP
    // 5. Both sides upgrade to TLS 1.3 mutual auth using identity certs
    // 6. Full duplex byte stream over the TLS socket
}
```

### 7.4 Bluetooth Classic (SPP)

Used as the last-resort fallback or for very short-range, high-privacy scenarios:

```kotlin
class BluetoothTransport : P2PTransport {
    val P2P_UUID = UUID.fromString("a3b4c5d6-1234-5678-abcd-ef0123456789")
    // 1. Discover via BluetoothAdapter scan or BLE advertisement
    // 2. createInsecureRfcommSocketToServiceRecord(P2P_UUID)
    // 3. Frame messages with 4-byte length prefix
    // 4. Same E2E crypto layer applied on top
}
```

### 7.5 Transport Priority & Failover

```
Priority 1: Wi-Fi Direct (highest bandwidth, lowest latency)
Priority 2: WebRTC over internet (NAT traversal)
Priority 3: Bluetooth (always available, lowest bandwidth)

Failover logic:
  → If current transport disconnects, ConnectionManager tries next
  → Pending messages are queued and flushed on reconnect
  → Messages sent on different transports carry same messageId
    (deduplication prevents double-delivery)
```

---

## 8. Local Storage Architecture

### 8.1 Room Database — AppDatabase

```kotlin
@Database(
    entities = [
        MessageEntity::class,
        ThreadEntity::class,
        TopicEntity::class,
        MessageTopicCrossRef::class,
        PeerEntity::class,
        RatchetStateEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun threadDao(): ThreadDao
    abstract fun topicDao(): TopicDao
    abstract fun peerDao(): PeerDao
    abstract fun ratchetStateDao(): RatchetStateDao
}
```

### 8.2 SQLCipher Configuration

```kotlin
object DatabaseFactory {
    fun create(context: Context, passphrase: ByteArray): AppDatabase {
        val factory = SupportFactory(passphrase, null, false)
        return Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
            .openHelperFactory(factory)
            .addMigrations(*ALL_MIGRATIONS)
            .enableMultiInstanceInvalidation()
            .build()
    }
}
```

### 8.3 Message Delivery State Machine

```
PENDING ──► SENT ──► DELIVERED ──► READ
   │
   └──► FAILED (retry queue with exponential backoff)
```

---

## 9. Threading & Topic Tagging System

This is the application's most distinctive feature. Every message belongs to a **Thread**, and threads can be tagged with one or many **Topics**.

### 9.1 Core Concepts

| Concept | Definition |
|---|---|
| **Message** | A single unit of communication with content, sender, timestamp |
| **Thread** | A named conversation — a sequence of messages with a shared context |
| **Topic/Tag** | A user-defined label (e.g., `#project-alpha`, `#urgent`, `#funny`) |
| **Threaded View** | A nested tree view grouping replies under their parent message |

### 9.2 Thread Model

Every message carries:

```kotlin
data class Message(
    val id: String,              // UUID
    val threadId: String,        // Which thread this belongs to
    val parentMessageId: String?,// null = root message; set = it's a reply
    val senderId: String,
    val content: EncryptedContent,
    val topics: List<String>,    // "#design", "#urgent"
    val timestamp: Long,
    val editedAt: Long?,
    val deliveryState: DeliveryState
)
```

### 9.3 Topic Tagging

Topics can be added to a message at any time (before or after sending):

```kotlin
// Tagging a message
suspend fun tagMessage(messageId: String, topic: String) {
    topicRepository.addTag(messageId, topic.lowercase().trim())
    // Propagate tag update to conversation peers via a TagSyncMessage
    networkLayer.broadcast(TagSyncMessage(messageId, topic, peerId))
}

// Filtering by topic
fun getMessagesByTopic(topic: String): Flow<List<Message>> =
    messageRepository.observeByTopic(topic)
```

### 9.4 Threaded View Construction

The UI reconstructs the thread tree from flat DB records:

```kotlin
fun buildThreadTree(messages: List<Message>): List<ThreadNode> {
    val nodeMap = messages.associateBy { it.id }
    val roots = mutableListOf<ThreadNode>()
    val childMap = mutableMapOf<String, MutableList<Message>>()

    messages.forEach { msg ->
        if (msg.parentMessageId == null) {
            roots.add(ThreadNode(msg, mutableListOf()))
        } else {
            childMap.getOrPut(msg.parentMessageId) { mutableListOf() }.add(msg)
        }
    }

    fun buildNode(msg: Message): ThreadNode =
        ThreadNode(msg, (childMap[msg.id] ?: emptyList()).map { buildNode(it) }.toMutableList())

    return roots.map { buildNode(it.message) }.sortedBy { it.message.timestamp }
}
```

### 9.5 Topic Search & Recall

```kotlin
// Full-text search across topics and message content
suspend fun search(query: String): SearchResults {
    return SearchResults(
        byTopic = topicDao.searchTopics(query),
        byContent = messageDao.fullTextSearch(query), // Uses FTS5 virtual table
        byPeer = peerDao.searchByName(query)
    )
}
```

The SQLite FTS5 extension indexes all message content (after decryption at read time) for fast recall:

```sql
CREATE VIRTUAL TABLE messages_fts USING fts5(
    content,
    thread_id UNINDEXED,
    tokenize = 'unicode61 remove_diacritics 1'
);
```

---

## 10. Connection Management Engine

### 10.1 ConnectionManager Responsibilities

```kotlin
class ConnectionManager @Inject constructor(
    private val webRtcTransport: WebRtcTransport,
    private val wifiDirectTransport: WifiDirectTransport,
    private val bluetoothTransport: BluetoothTransport,
    private val messageQueue: PersistentMessageQueue
) {
    // Maintains a live map of peerId → ActiveTransport
    private val activePeers = ConcurrentHashMap<String, P2PTransport>()

    // Outbound: tries transports in priority order
    suspend fun sendToPeer(peerId: String, payload: EncryptedPayload)

    // Inbound: merges receive() flows from all active transports
    fun incomingMessages(): Flow<Pair<String, EncryptedPayload>>

    // Reconnection: exponential backoff with jitter
    private suspend fun reconnect(peerId: String)
}
```

### 10.2 Peer Discovery Pipeline

```
LAN Discovery (NSD):
  ┌──────────────────────────────────────────┐
  │  App registers mDNS service:             │
  │  _p2pchat._tcp.local port 47890          │
  │                                          │
  │  Discovers other _p2pchat._tcp services  │
  │  Reads TXT record: {peerId, pubKey}      │
  └──────────────────────────────────────────┘

Internet Discovery:
  ┌──────────────────────────────────────────┐
  │  User shares "invite link" or QR code    │
  │  containing: {peerId, pubKey, signalUrl} │
  │                                          │
  │  Recipient uses link to initiate         │
  │  WebRTC signaling exchange               │
  └──────────────────────────────────────────┘
```

### 10.3 Message Queue & Retry

```kotlin
class PersistentMessageQueue @Inject constructor(
    private val db: AppDatabase
) {
    // Enqueue a message for delivery (survives app restart)
    suspend fun enqueue(message: QueuedMessage)

    // WorkManager-scheduled retry with exponential backoff
    // Retry delays: 5s → 15s → 45s → 2min → 10min → give up
    suspend fun flushForPeer(peerId: String, transport: P2PTransport)
}
```

---

## 11. UI/UX Architecture

### 11.1 Navigation Graph

```
NavHost
├── ConversationsListScreen      ← Home: all threads sorted by recency
│   ├── NewConversationDialog
│   └── TopicFilterBottomSheet
├── ChatScreen(threadId)         ← Threaded message view
│   ├── MessageComposer
│   ├── TagPickerBottomSheet
│   └── MessageContextMenu
│       ├── ReplyInThread
│       ├── AddTag
│       ├── CopyMessage
│       └── DeleteMessage
├── TopicsScreen                 ← Browse/search all topics
│   └── TopicDetailScreen(tag)  ← All messages with this tag
├── PeersScreen                  ← Manage peer connections
│   ├── DiscoverPeersDialog
│   └── PeerProfileScreen
└── SettingsScreen
    ├── IdentityScreen           ← View/export/rotate keys
    ├── SecurityScreen           ← PIN, biometric, backup
    └── AboutScreen
```

### 11.2 ViewModel Pattern

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getThreadMessages: GetThreadMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val tagMessage: TagMessageUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val threadId = savedStateHandle.get<String>("threadId")!!

    val uiState: StateFlow<ChatUiState> = getThreadMessages(threadId)
        .map { messages -> ChatUiState.Success(buildThreadTree(messages)) }
        .catch { emit(ChatUiState.Error(it.message ?: "Unknown error")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState.Loading)

    fun onSendMessage(text: String, parentId: String? = null) {
        viewModelScope.launch {
            sendMessage(SendMessageParams(threadId, text, parentId))
        }
    }

    fun onTagMessage(messageId: String, topic: String) {
        viewModelScope.launch { tagMessage(messageId, topic) }
    }
}
```

### 11.3 Key UI Components

**ConversationListItem** — Shows thread name, last message preview (decrypted), unread count, topic chips, peer connection status indicator (green dot = online, gray = offline).

**ChatBubble** — Color-coded by sender, indented by reply depth (max 4 levels), long-press reveals context menu. Encrypted messages that fail to decrypt show a padlock icon.

**TopicChip** — Tappable colored label. Tap navigates to TopicDetailScreen for full filtered view.

**ConnectionStatusBar** — Persistent top banner showing active transport type (🔵 WebRTC / 🟢 WiFi Direct / 🟠 Bluetooth) or ⚫ Offline.

---

## 12. Data Models

```kotlin
// core-model module — no Android dependencies

data class Peer(
    val id: String,
    val displayName: String,
    val publicKey: PublicKeyBundle,
    val lastSeen: Instant,
    val isTrusted: Boolean,
    val connectionHistory: List<ConnectionRecord>
)

data class PublicKeyBundle(
    val identityKey: ByteArray,   // Ed25519
    val exchangeKey: ByteArray,   // X25519
    val certPem: String           // Self-signed X.509
)

data class Thread(
    val id: String,
    val name: String,
    val participants: List<String>, // peer IDs
    val createdAt: Instant,
    val lastActivity: Instant,
    val isPinned: Boolean
)

data class Message(
    val id: String,
    val threadId: String,
    val parentMessageId: String?,
    val senderId: String,
    val encryptedContent: ByteArray,
    val iv: ByteArray,
    val clearTextCache: String?,   // transient, never persisted
    val topics: Set<String>,
    val attachments: List<Attachment>,
    val sentAt: Instant,
    val deliveryState: DeliveryState,
    val reactions: Map<String, String> // peerId → emoji
)

data class Topic(
    val name: String,              // normalized: lowercase, no spaces
    val displayName: String,       // user-facing label
    val color: Int,                // ARGB
    val messageCount: Int,
    val lastUsed: Instant
)

sealed class DeliveryState {
    object Pending : DeliveryState()
    data class Sent(val at: Instant) : DeliveryState()
    data class Delivered(val at: Instant) : DeliveryState()
    data class Read(val at: Instant, val by: String) : DeliveryState()
    data class Failed(val reason: String, val retryCount: Int) : DeliveryState()
}
```

---

## 13. Database Schema

```sql
-- Peers table
CREATE TABLE peers (
    id           TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    identity_key BLOB NOT NULL,
    exchange_key BLOB NOT NULL,
    cert_pem     TEXT NOT NULL,
    last_seen    INTEGER NOT NULL,
    is_trusted   INTEGER NOT NULL DEFAULT 0,
    created_at   INTEGER NOT NULL
);

-- Threads table
CREATE TABLE threads (
    id            TEXT PRIMARY KEY,
    name          TEXT NOT NULL,
    created_at    INTEGER NOT NULL,
    last_activity INTEGER NOT NULL,
    is_pinned     INTEGER NOT NULL DEFAULT 0
);

-- Thread participants junction
CREATE TABLE thread_participants (
    thread_id TEXT NOT NULL,
    peer_id   TEXT NOT NULL,
    joined_at INTEGER NOT NULL,
    PRIMARY KEY (thread_id, peer_id),
    FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    FOREIGN KEY (peer_id)   REFERENCES peers(id)   ON DELETE CASCADE
);

-- Messages table
CREATE TABLE messages (
    id                TEXT PRIMARY KEY,
    thread_id         TEXT NOT NULL,
    parent_message_id TEXT,
    sender_id         TEXT NOT NULL,
    encrypted_content BLOB NOT NULL,
    iv                BLOB NOT NULL,
    sent_at           INTEGER NOT NULL,
    delivery_state    TEXT NOT NULL DEFAULT 'PENDING',
    delivered_at      INTEGER,
    read_at           INTEGER,
    is_deleted        INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_thread    ON messages(thread_id, sent_at);
CREATE INDEX idx_messages_parent    ON messages(parent_message_id);
CREATE INDEX idx_messages_sender    ON messages(sender_id);

-- Topics table
CREATE TABLE topics (
    name         TEXT PRIMARY KEY,
    display_name TEXT NOT NULL,
    color        INTEGER NOT NULL,
    created_at   INTEGER NOT NULL,
    last_used    INTEGER NOT NULL
);

-- Message ↔ Topic many-to-many
CREATE TABLE message_topics (
    message_id TEXT NOT NULL,
    topic_name TEXT NOT NULL,
    tagged_at  INTEGER NOT NULL,
    PRIMARY KEY (message_id, topic_name),
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (topic_name) REFERENCES topics(name) ON DELETE CASCADE
);

CREATE INDEX idx_message_topics_topic ON message_topics(topic_name, tagged_at);

-- Double Ratchet state (one row per peer conversation)
CREATE TABLE ratchet_states (
    peer_id           TEXT PRIMARY KEY,
    root_key          BLOB NOT NULL,
    send_chain_key    BLOB NOT NULL,
    receive_chain_key BLOB NOT NULL,
    send_message_num  INTEGER NOT NULL DEFAULT 0,
    recv_message_num  INTEGER NOT NULL DEFAULT 0,
    updated_at        INTEGER NOT NULL
);

-- Full-Text Search virtual table (populated via trigger)
CREATE VIRTUAL TABLE messages_fts USING fts5(
    decrypted_content,
    message_id UNINDEXED,
    thread_id  UNINDEXED,
    tokenize   = 'unicode61 remove_diacritics 1'
);
```

---

## 14. API & Interface Contracts

### 14.1 Use Case Layer (Domain)

```kotlin
// All use cases are single-responsibility and inject-able

class SendMessageUseCase @Inject constructor(
    private val messageRepo: MessageRepository,
    private val cryptoEngine: CryptoEngine,
    private val connectionManager: ConnectionManager
) {
    suspend operator fun invoke(params: SendMessageParams): Result<Message> {
        val encrypted = cryptoEngine.encrypt(params.content, params.recipientKey)
        val message   = messageRepo.saveOutgoing(encrypted, params)
        connectionManager.sendToPeer(params.threadId, encrypted.toPayload())
        return Result.success(message)
    }
}

class GetThreadMessagesUseCase @Inject constructor(
    private val messageRepo: MessageRepository,
    private val cryptoEngine: CryptoEngine
) {
    operator fun invoke(threadId: String): Flow<List<Message>> =
        messageRepo.observeThread(threadId)
            .map { entities -> entities.map { cryptoEngine.decrypt(it) } }
}

class TagMessageUseCase @Inject constructor(
    private val topicRepo: TopicRepository,
    private val syncEngine: TagSyncEngine
) {
    suspend operator fun invoke(messageId: String, topic: String): Result<Unit>
}

class SearchConversationsUseCase @Inject constructor(
    private val searchRepo: SearchRepository
) {
    operator fun invoke(query: String): Flow<SearchResults>
}
```

### 14.2 Repository Contracts

```kotlin
interface MessageRepository {
    fun observeThread(threadId: String): Flow<List<MessageEntity>>
    fun observeByTopic(topic: String): Flow<List<MessageEntity>>
    suspend fun saveOutgoing(payload: EncryptedPayload, params: SendMessageParams): Message
    suspend fun saveIncoming(payload: EncryptedPayload, senderId: String): Message
    suspend fun updateDeliveryState(messageId: String, state: DeliveryState)
    suspend fun deleteMessage(messageId: String)
    suspend fun fullTextSearch(query: String): List<MessageEntity>
}
```

---

## 15. Security Threat Model

### 15.1 Threat Matrix

| Threat | Mitigation |
|---|---|
| Network eavesdropping | AES-256-GCM E2E encryption; DTLS at transport layer |
| Man-in-the-middle | Ed25519 identity signing; QR/invite-link key verification ceremony |
| Physical device compromise | SQLCipher DB encryption; Android Keystore hardware binding |
| Replay attacks | Unique message IDs + nonces; Double Ratchet monotonic message counters |
| Impersonation | Self-signed cert pinning; key fingerprint display for manual verification |
| Metadata analysis | No central server; peer IPs visible only to direct peers |
| Malicious peer | Per-peer trust flag; block list; conversation isolation |
| Key extraction | Private keys in Android Keystore (StrongBox where available) |
| Forward secrecy violation | Double Ratchet deletes old message keys after use |
| Traffic analysis (Bluetooth) | Message padding to fixed 256-byte chunks |

### 15.2 Key Verification Flow

```
Alice shows Bob her key fingerprint (QR code or 12-word safety number).
Bob scans / reads and confirms on his device.
Both devices mark each other as "verified" in the peer record.
Verified peers get a ✓ badge in UI.
Any key change triggers a prominent "Safety Number Changed" warning.
```

### 15.3 What This App Cannot Protect Against

- **Compromised OS or rooted device** — If the OS is compromised, all bets are off
- **Screenshots by the recipient** — Sender cannot prevent this
- **Metadata at the network layer** — Peer IPs are visible to each other
- **Legal compulsion of the device holder** — User holds the keys; biometric/PIN is their responsibility

---

## 16. Error Handling & Resilience

### 16.1 Structured Error Types

```kotlin
sealed class P2PChatError : Throwable() {
    data class EncryptionFailed(val reason: String) : P2PChatError()
    data class DecryptionFailed(val messageId: String) : P2PChatError()
    data class PeerUnreachable(val peerId: String) : P2PChatError()
    data class TransportFailed(val transport: String, val cause: Throwable) : P2PChatError()
    data class StorageError(val operation: String, val cause: Throwable) : P2PChatError()
    object RatchetStateCorrupted : P2PChatError()
}
```

### 16.2 Resilience Patterns

**Circuit Breaker** — After 3 consecutive transport failures to a peer, the circuit opens and reconnection is attempted on a backoff schedule, preventing battery drain from constant retry storms.

**Outbox Pattern** — Messages are persisted to the outbox table before any network attempt. A WorkManager job runs on network availability to flush the outbox. Messages are never lost due to connectivity interruptions.

**Ratchet State Recovery** — If the Double Ratchet state is corrupted (detectable via MAC failure), the app initiates a new handshake automatically while notifying the user that some messages may need to be re-sent.

**Graceful UI Degradation** — Decryption failures show a placeholder bubble with a "🔒 Message could not be decrypted" label rather than crashing or silently dropping messages.

---

## 17. Performance Strategy

### 17.1 Key Performance Targets

| Metric | Target |
|---|---|
| Message send latency (LAN) | < 50ms |
| Message send latency (WebRTC) | < 200ms |
| App cold start | < 1.5 seconds |
| Thread list scroll | 60fps constant |
| DB query for 10k messages | < 100ms |
| Crypto operations | Off-main-thread always |

### 17.2 Implementation Techniques

**Paging** — Conversation history uses Paging 3 with a `RemoteMediator`-like pattern to load messages in pages of 50, preventing OOM on long conversations.

**Background Crypto** — All encrypt/decrypt runs on a dedicated `Dispatchers.Default` coroutine scope. The UI thread never blocks on crypto.

**LazyColumn with Keys** — Compose `LazyColumn` uses `key = { message.id }` to prevent unnecessary recomposition on list updates.

**Database Indexing** — Composite indexes on `(thread_id, sent_at)` and `(topic_name, tagged_at)` ensure O(log n) queries even with millions of messages.

**Media Handling** — Images/files are stored as encrypted blobs in app-private storage. Thumbnails are generated on first view and cached in a separate SQLite table.

---

## 18. Testing Strategy

### 18.1 Test Pyramid

```
                    ┌──────────────────┐
                    │   UI / E2E Tests │  ← 10%  (Espresso, Compose Test)
                  ┌─┴──────────────────┴─┐
                  │  Integration Tests    │  ← 20%  (In-memory Room DB, fake transports)
                ┌─┴───────────────────────┴─┐
                │       Unit Tests           │  ← 70%  (JUnit5, MockK, Turbine)
                └───────────────────────────┘
```

### 18.2 Critical Test Cases

**Crypto tests:**
- Known-answer tests for AES-256-GCM encrypt/decrypt
- Double Ratchet: verify message key derivation over 100 steps
- Ratchet re-synchronization after skipped messages

**Network tests:**
- Transport failover: simulate WebRTC disconnect, verify BT takeover
- Message deduplication when same message arrives on two transports
- Pending queue flushed correctly on reconnect

**Storage tests:**
- Thread tree reconstruction is correct for 5-level deep reply trees
- Topic filter returns exactly the tagged messages
- FTS search returns relevant results ranked by recency

**Security tests:**
- Tampered ciphertext rejected (MAC failure)
- Replayed message (duplicate ID) silently dropped
- Wrong-key decryption produces error, not garbled text

---

## 19. Build & Deployment

### 19.1 Build Variants

```kotlin
// app/build.gradle.kts

buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        isDebuggable = true
        // No ProGuard in debug; STUN server points to local test server
    }
    release {
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}

flavorDimensions += "connectivity"
productFlavors {
    create("fullP2P") {
        // WebRTC + WiFi Direct + Bluetooth
    }
    create("lanOnly") {
        // WiFi Direct + Bluetooth only (stricter privacy)
    }
}
```

### 19.2 ProGuard Rules (Critical)

```pro
# Keep WebRTC JNI
-keep class org.webrtc.** { *; }

# Keep SQLCipher
-keep class net.sqlcipher.** { *; }

# Keep all crypto classes (reflective access)
-keep class **.crypto.** { *; }

# Keep Room entities and DAOs
-keep @androidx.room.Entity class *
-keep interface * extends androidx.room.RoomDatabase
```

### 19.3 Minimum Requirements

| Requirement | Value |
|---|---|
| Minimum SDK | API 26 (Android 8.0) |
| Target SDK | API 35 (Android 15) |
| Compile SDK | API 35 |
| Kotlin | 1.9.x |
| AGP | 8.x |
| JVM Target | 17 |

---

## 20. Roadmap & Future Enhancements

### Phase 1 — MVP (Months 1–3)
- [x] Identity generation and key management
- [x] Bluetooth transport
- [x] Wi-Fi Direct transport
- [x] Basic Double Ratchet E2E encryption
- [x] SQLCipher local storage
- [x] Basic thread view and message sending
- [x] Topic tagging on messages

### Phase 2 — Internet P2P (Months 4–6)
- [ ] WebRTC DataChannel transport
- [ ] QR code peer invitation flow
- [ ] Key verification ceremony (safety numbers)
- [ ] Image and file attachment support
- [ ] Offline message queue with WorkManager

### Phase 3 — Advanced Features (Months 7–12)
- [ ] Group messaging with Sender Keys
- [ ] Voice notes (encrypted audio blobs)
- [ ] Disappearing messages (time-based local deletion)
- [ ] Message reactions
- [ ] Multi-device support (same identity on tablet + phone via key export)
- [ ] Gossip-protocol group membership sync
- [ ] iOS version via Kotlin Multiplatform (shared domain + crypto layers)

### Phase 4 — Hardening (Ongoing)
- [ ] Third-party security audit of crypto implementation
- [ ] Formal verification of ratchet protocol implementation
- [ ] StrongBox Keymaster integration for hardware key attestation
- [ ] Sealed sender (metadata privacy — recipient can't tell who sent a message until decryption)

---

## Appendix A — Gradle Dependencies

```kotlin
// core-crypto/build.gradle.kts
dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}

// core-network/build.gradle.kts
dependencies {
    implementation("io.getstream:stream-webrtc-android:1.1.1")
}

// core-storage/build.gradle.kts
dependencies {
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.paging:paging-runtime:3.2.1")
}

// app/build.gradle.kts
dependencies {
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

---

## Appendix B — Permissions Manifest

```xml
<!-- AndroidManifest.xml -->

<!-- Networking -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />

<!-- Wi-Fi Direct -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation" />

<!-- Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Background work -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Media -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />

<!-- Optional hardware features -->
<uses-feature android:name="android.hardware.bluetooth" android:required="false" />
<uses-feature android:name="android.hardware.wifi.direct" android:required="false" />
```

---

---

## 21. Peer Discovery & The Chat Address System

### 21.1 The Core Problem — And Why Bitcoin Solved It First

In a centralized app like WhatsApp, their server acts as a global phone book:
```
Server lookup:  +1-555-0100  →  account_id_789  →  encryption_key
```
Remove the server, and that directory vanishes. The P2P world needed a new primitive — and **Bitcoin gave us one in 2009**: the cryptographic self-sovereign address.

> **The Insight:** Bitcoin proved you don't need a bank to send value using only a cryptographic address pair. This app proves you don't need a server to send messages — for exactly the same reason.

---

### 21.2 The Chat Address — Anatomy

Every user generates a **Chat Address** on first launch. No server involved. No registration. The keys are generated on-device and the address is mathematically derived from them — identical in principle to how a Bitcoin wallet address is derived from a private key.

```
p2pchat://peer/aX9kR2mN
         ?name=John
         &ik=7f3a9bc4ed25519...   ← Identity Key  (Ed25519)  — proves you are YOU
         &ek=2d8f1a03x25519...    ← Exchange Key  (X25519)   — used to encrypt TO you
         &sig=9e4c7bSignature...  ← Signature — proves the keys were not tampered with
         &relay=stun:google.com   ← How to reach you over internet (optional)
```

| Field | Bitcoin Equivalent | Purpose |
|---|---|---|
| `aX9kR2mN` (Peer ID) | Wallet Address | Unique identifier to find you |
| Identity Key (`ik`) | Public Key | Proves messages come from you |
| Exchange Key (`ek`) | — | Lets others encrypt messages only you can open |
| Signature (`sig`) | ECDSA signature | Proves the address wasn't tampered with in transit |

**Encoding:** The full address is Base58-encoded (same as Bitcoin) and rendered as a QR code, deep link, or 12-word mnemonic for human-readable sharing.

---

### 21.3 Bitcoin vs. Chat Address — Full Comparison

| Property | Bitcoin Wallet Address | P2P Chat Address |
|---|---|---|
| Who assigns it | **You generate it yourself** | **You generate it yourself** |
| Tied to real identity | No — completely pseudonymous | No — completely anonymous |
| Can be revoked by a third party | Never | Never |
| How many can you have | Unlimited | Unlimited |
| Reveals your location | No | No |
| What knowing it lets others do | Send you Bitcoin | Send you encrypted messages |
| What knowing it does NOT let others do | Spend your funds | Decrypt your messages or impersonate you |
| Works without internet | N/A | Yes — Bluetooth / Wi-Fi Direct |
| Permanence | Forever (until you lose private key) | Forever (until you rotate or abandon) |
| Verification mechanism | ECDSA signature on blockchain | Ed25519 signature + key fingerprint |

---

### 21.4 Sharing Your Chat Address — Every Possible Channel

Just as you post a Bitcoin address anywhere to receive funds, you share your Chat Address anywhere to receive messages:

```
Via QR Code:     Perfect for in-person meetings — scan to connect instantly
Via SMS:         "p2pchat://aX9kR..." — tap opens the app, connection initiated
Via WhatsApp:    Share in any chat — ironic but useful during transition
Via Email:       Paste in signature: "Message me securely: p2pchat://..."
Via NFC Tap:     Two phones touch — addresses exchanged, connected in <1 second
Via Twitter/X:   Post in your bio — anyone can initiate a private chat with you
Via Website:     <a href="p2pchat://aX9kR2mN">Message me securely</a>
Via Business Card: Print QR code on card — scan to connect
Via Reddit DM:   "Here's my secure address: p2pchat://..."
```

**Deep Link Handler in AndroidManifest.xml:**
```xml
<activity android:name=".ui.MainActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="p2pchat" android:host="peer" />
    </intent-filter>
</activity>
```

**Deep Link Handler in Kotlin:**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { uri ->
            if (uri.scheme == "p2pchat" && uri.host == "peer") {
                val peerId   = uri.lastPathSegment
                val name     = uri.getQueryParameter("name")
                val identKey = uri.getQueryParameter("ik")
                val exchKey  = uri.getQueryParameter("ek")
                val sig      = uri.getQueryParameter("sig")
                // Verify signature, save peer, initiate WebRTC handshake
                viewModel.onIncomingPeerInvite(PeerDescriptor(peerId, name, identKey, exchKey, sig))
            }
        }
    }
}
```

---

### 21.5 Four Discovery Modes

```
┌─────────────────────────────────────────────────────────────────────┐
│  MODE 1: Same Room (Zero Config)                                    │
│  Both on same Wi-Fi → mDNS auto-discovers → "People Nearby" list   │
│  No address exchange needed. Just tap their name and connect.       │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  MODE 2: QR Code / Deep Link (Primary Method)                       │
│  Share your Chat Address as QR or link via any channel.            │
│  Recipient scans/taps → app opens → direct P2P connection formed.  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  MODE 3: Phone Number Lookup (Optional Hybrid)                      │
│  A lightweight broker stores: SHA256(phone) → PeerID + PubKey      │
│  Server never sees messages. Only used for initial discovery.       │
│  After first connection, server is never contacted again.           │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│  MODE 4: DHT Username (@handle) — Advanced                          │
│  Kademlia-style DHT maps @john_doe → PeerID + PubKey               │
│  Fully decentralized. No broker server. libp2p implementation.      │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 21.6 Full Connection Flow (Stranger to Stranger)

```
Alice wants to message Bob (complete strangers):

STEP 1 — Bob generates and shares his Chat Address
  Bob: App → "My Address" → tap Share
  Bob sends the link to Alice via Reddit DM, email, SMS — anything

STEP 2 — Alice opens the link
  Alice taps link → P2PChatApp opens → address parsed
  App shows: "Connect to Bob? Key fingerprint: A3F9-2B1C-..."
  Alice taps Confirm

STEP 3 — Cryptographic Handshake
  Alice's app generates ephemeral X25519 keypair
  Alice → Bob: { SDP Offer + her ephemeral public key + her identity key + signature }
  Bob  ← verifies Alice's signature (or marks as unverified stranger)
  Bob  → Alice: { SDP Answer + his ephemeral public key }
  Shared secret = ECDH(Alice_ephemeral_priv, Bob_ephemeral_pub)
  Root Key + Chain Key derived via HKDF

STEP 4 — Secure Channel Open
  WebRTC DataChannel established (DTLS encrypted at transport layer)
  Application-layer AES-256-GCM encryption applied on top
  Bob and Alice are now chatting — no server was ever involved in message delivery

STEP 5 — Optional: Key Verification
  Alice reads Bob the 12-digit safety number (or scans his QR in person)
  Bob confirms it matches on his screen
  Both devices mark each other as "Verified" — ✓ badge appears in UI
```

---

### 21.7 The "Public Key IS the Address" Principle

This is the deep cryptographic truth Bitcoin introduced:

```
Bitcoin:    RIPEMD160( SHA256( PublicKey ) )  =  Wallet Address
P2P Chat:   PublicKey itself                  =  Your Identity
```

In both systems, the address is **mathematically bound to the private key**. You cannot fake ownership of an address without the corresponding private key. This means:

- Knowing Bob's Chat Address → you can send Bob a message
- Knowing Bob's Chat Address → you **cannot** read Bob's messages
- Knowing Bob's Chat Address → you **cannot** impersonate Bob
- Bob's private key never leaves his Android Keystore → **no one can steal his identity**

---

## 22. Multi-Identity & Burner Address System

### 22.1 The Core Concept

Just as a privacy-conscious Bitcoin user generates a **fresh wallet address for every transaction** to prevent transaction graph analysis, this app supports **multiple simultaneous identities** — each completely unlinkable from the others.

```kotlin
sealed class IdentityType {
    object Permanent : IdentityType()   // Your long-term, trusted identity
    object Burner    : IdentityType()   // Ephemeral — burn after use
    data class Contextual(
        val label: String               // "Work", "Family", "Anonymous"
    ) : IdentityType()
}

data class UserIdentity(
    val id: String,
    val type: IdentityType,
    val displayName: String,
    val ed25519KeyPair: KeyPair,
    val x25519KeyPair: KeyPair,
    val createdAt: Instant,
    val expiresAt: Instant?,            // null = permanent
    val useCount: Int,
    val isBurned: Boolean = false
)
```

---

### 22.2 Identity Manager

```kotlin
@Singleton
class IdentityManager @Inject constructor(
    private val keystore: AndroidKeystoreWrapper,
    private val identityDao: IdentityDao
) {

    // Generate a brand new permanent identity (first launch or rotation)
    suspend fun generatePermanentIdentity(name: String): UserIdentity {
        val ed25519 = keystore.generateEd25519KeyPair(alias = "permanent_identity")
        val x25519  = keystore.generateX25519KeyPair(alias = "permanent_exchange")
        return UserIdentity(
            id          = UUID.randomUUID().toString(),
            type        = IdentityType.Permanent,
            displayName = name,
            ed25519KeyPair = ed25519,
            x25519KeyPair  = x25519,
            createdAt   = Instant.now(),
            expiresAt   = null
        ).also { identityDao.insert(it) }
    }

    // Generate a single-use burner identity — auto-burns after first conversation ends
    suspend fun generateBurnerIdentity(): UserIdentity {
        val alias = "burner_${System.currentTimeMillis()}"
        return UserIdentity(
            id          = UUID.randomUUID().toString(),
            type        = IdentityType.Burner,
            displayName = "Anonymous",
            ed25519KeyPair = keystore.generateEd25519KeyPair(alias),
            x25519KeyPair  = keystore.generateX25519KeyPair(alias),
            createdAt   = Instant.now(),
            expiresAt   = Instant.now().plus(Duration.ofHours(24))
        ).also { identityDao.insert(it) }
    }

    // Burn an identity — wipe keys from Keystore, mark as burned in DB
    suspend fun burnIdentity(identityId: String) {
        val identity = identityDao.getById(identityId)
        keystore.deleteKey(identity.ed25519Alias)
        keystore.deleteKey(identity.x25519Alias)
        identityDao.markBurned(identityId)
        // All conversations tied to this identity are now permanently unreadable
        // even if the device is seized — the keys no longer exist
    }

    // Rotate permanent identity — generates new keys, notifies trusted contacts
    suspend fun rotatePermanentIdentity(): UserIdentity {
        val oldIdentity = getPermanentIdentity()
        val newIdentity = generatePermanentIdentity(oldIdentity.displayName)
        // Broadcast key rotation notice to all trusted peers (signed by old key)
        // so their apps update their stored key for you
        broadcastKeyRotation(oldIdentity, newIdentity)
        burnIdentity(oldIdentity.id)
        return newIdentity
    }
}
```

---

### 22.3 Use Cases for Multiple Identities

| Scenario | Identity to Use | Why |
|---|---|---|
| Chatting with close friends & family | Permanent Identity | They know your real name; long-term trust |
| Work communications | Contextual "Work" Identity | Separate from personal life |
| Public forum / Reddit | Burner Identity | No link to your real identity |
| Whistleblowing | Burner Identity | Burn after sending — keys destroyed |
| Dating app contact (new person) | Burner Identity | Evaluate trust before revealing permanent address |
| Protest / Activist organizing | Burner Identity | Protects against device seizure |
| Testing the app | Burner Identity | Disposable, auto-expires |

---

### 22.4 Identity Isolation — The Firewall

Identities are **completely isolated** — it is cryptographically impossible to link them without the user's consent:

```
Permanent Identity (aX9kR2mN):
  └── Trusted Contact: Alice
  └── Trusted Contact: Bob
  └── Family Group Chat

Work Identity (bY8pS3nO):
  └── Colleague: Carol
  └── Work Group Chat
  (Carol cannot tell this is the same person as aX9kR2mN)

Burner Identity (cZ7qT4mP):
  └── Stranger from Reddit
  └── BURNED after conversation — keys deleted, identity gone
```

The only way to link two identities is if **you choose to tell someone** or if both are used from the same IP address (mitigatable with a VPN).

---

### 22.5 Key Rotation — Like Changing Your Bitcoin Wallet

When you suspect your identity is compromised (or just want a fresh start):

```
Key Rotation Flow:

1. App generates brand new Ed25519 + X25519 keypair
2. Constructs a signed "Key Rotation Notice":
   {
     "oldKeyFingerprint": "A3F9-2B1C-...",
     "newPublicKey": "...",
     "rotatedAt": 1700000000,
     "signature": "..."   ← Signed with OLD key (proves you authorized this)
   }
3. Sends the notice to all trusted contacts via existing encrypted channels
4. Contacts' apps automatically update their stored key for you
5. Old private key is deleted from Android Keystore
6. Future messages encrypted with new key — old key cannot decrypt them
```

---

### 22.6 Safety Number / Key Fingerprint Verification

To prevent man-in-the-middle attacks during the initial connection, users can verify each other's key fingerprints — exactly like Signal's "Safety Numbers":

```kotlin
fun generateSafetyNumber(myIdentityKey: ByteArray, theirIdentityKey: ByteArray): String {
    // Concatenate both keys in deterministic order (lexicographic by key bytes)
    val combined = if (myIdentityKey < theirIdentityKey)
        myIdentityKey + theirIdentityKey
    else
        theirIdentityKey + myIdentityKey

    val hash = SHA512.digest(combined)

    // Format as 12 groups of 5 digits (like Signal)
    return hash.take(30).chunked(5) { bytes ->
        bytes.fold(0L) { acc, b -> acc * 256 + b.toLong() }.mod(100000L)
            .toString().padStart(5, '0')
    }.joinToString(" ")
    // Example: "05421 37109 82043 11208 94732 00183 59471 22830 74920 13847 58201 39047"
}
```

**In the UI:**
```
┌──────────────────────────────────────────┐
│  🔐 Verify Bob's Identity                │
│                                          │
│  Read this number to Bob out loud,       │
│  or compare screens in person:           │
│                                          │
│  05421 37109 82043 11208                 │
│  94732 00183 59471 22830                 │
│  74920 13847 58201 39047                 │
│                                          │
│  If Bob's screen shows the same number,  │
│  this conversation is fully secure.      │
│                                          │
│  [ ✓ Numbers Match — Mark as Verified ] │
│  [ ✗ Numbers Don't Match — Disconnect ] │
└──────────────────────────────────────────┘
```

---

### 22.7 QR Code Generation for Chat Address

```kotlin
fun generateAddressQrCode(identity: UserIdentity, sizePx: Int = 512): Bitmap {
    val addressUri = buildString {
        append("p2pchat://peer/${identity.id}")
        append("?name=${Uri.encode(identity.displayName)}")
        append("&ik=${identity.ed25519PublicKey.toHex()}")
        append("&ek=${identity.x25519PublicKey.toHex()}")
        val sig = signAddress(identity)   // Sign with Ed25519 private key
        append("&sig=${sig.toHex()}")
    }

    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,  // High error correction
        EncodeHintType.MARGIN to 2
    )

    val bitMatrix = QRCodeWriter().encode(addressUri, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    return BarcodeEncoder().createBitmap(bitMatrix)
}
```

---

### 22.8 The Complete Discovery Decision Tree

```
User wants to connect with someone new:
│
├── Are you in the same room?
│   └── YES → "People Nearby" (mDNS) → tap their name → connected
│
├── Do you have their Chat Address link/QR?
│   └── YES → Scan QR or tap link → WebRTC handshake → connected
│
├── Do you know their phone number?
│   └── YES → Phone hash lookup → get their PeerID → WebRTC handshake → connected
│
├── Do you know their @username?
│   └── YES → DHT lookup → get their PeerID → WebRTC handshake → connected
│
└── Complete strangers, no info?
    └── They must share their Chat Address with you first
        (Just like: they must give you their Bitcoin address before you can send BTC)
```

---

---

## 23. Metadata Privacy & IP Anonymization

### 23.1 The Problem — Why Metadata Is As Dangerous As Content

End-to-end encryption hides *what* is said. But even with perfect AES-256-GCM encryption, a network observer sees the connection itself:

```
Device A (IP: 192.168.1.10)  ◄──────────────►  Device B (IP: 203.0.113.45)
                                  encrypted

The observer learns:
  ✗  Message content          → HIDDEN  (encryption works)
  ✓  Alice talked to Bob      → VISIBLE (IP metadata)
  ✓  When they talked         → VISIBLE (connection timestamps)
  ✓  How much they said       → VISIBLE (packet sizes, frequency)
  ✓  Alice's physical location→ VISIBLE (IP → ISP → city-level geolocation)
```

In authoritarian contexts, surveillance states, or targeted attacks, **knowing Alice talked to Bob at 2am for 3 hours** can be more actionable than the message content itself. This section covers every layer of defense against metadata exposure.

---

### 23.2 Solution 1 — Tor Network (Maximum Anonymity)

#### How Tor Works

```
Without Tor:
  Alice ──────────────────────────────────────────► Bob
  Bob sees Alice's real IP. ISP sees both endpoints.

With Tor (3-hop onion routing):
  Alice ──► Guard Node ──► Middle Node ──► Exit Node ──► Bob
            (knows Alice)  (knows nobody)  (knows Bob)
  
  Each node decrypts one layer of encryption — like peeling an onion.
  No single node knows both Alice AND Bob.
  Bob sees only the Exit Node IP — never Alice's real IP.
```

#### Android Implementation — Embedded Tor via tor-android

```kotlin
// build.gradle.kts
implementation("info.guardianproject:tor-android:0.4.5.13")
implementation("info.guardianproject:jtorctl:0.4.5.13")

class TorTransportWrapper @Inject constructor(
    private val context: Context,
    private val innerTransport: P2PTransport
) : P2PTransport {

    private val bootstrapState = MutableStateFlow(BootstrapState.STOPPED)

    suspend fun startTor() {
        OrbotHelper.get(context).init()
        // Wait for Tor bootstrap to reach 100%
        bootstrapState
            .filter { it == BootstrapState.CONNECTED }
            .first()
    }

    override suspend fun connect(peer: PeerDescriptor): Result<Unit> {
        // Route all WebRTC signaling and data through Tor SOCKS5 proxy
        val torProxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress("127.0.0.1", 9050)
        )
        // Connect to peer's .onion address — no real IP involved at any step
        return innerTransport.connectViaProxy(peer.onionAddress!!, torProxy)
    }
}
```

#### Tor v3 Onion Address as Chat Address

The most powerful feature: a Tor v3 `.onion` address is **mathematically derived from an Ed25519 public key** — the same key type already used in our identity system. This means the Chat Address and Tor address can be **cryptographically unified**:

```kotlin
fun deriveOnionAddress(ed25519PublicKey: ByteArray): String {
    // Tor v3 onion = Base32( pubkey[32] || checksum[2] || version[1] )
    val version  = byteArrayOf(0x03)
    val checksum = SHA3_256.digest(
        ".onion checksum".toByteArray() + ed25519PublicKey + version
    ).take(2).toByteArray()

    val payload = ed25519PublicKey + checksum + version
    return Base32.encode(payload).lowercase() + ".onion"
    // Output: "p53lf57qovyuvwsc6xnrppyply3vtqm7l6pcobkmyqsiofyeznfu5uqd.onion"
}

// Unified Chat Address now looks like:
// p2pchat://peer/p53lf57q...uqd.onion?ik=ed25519key...&ek=x25519key...
//
// This address:
//   ✓ Works globally — no IP needed, no DNS needed
//   ✓ Hides BOTH parties' real IP addresses
//   ✓ Is self-authenticating — the address IS the public key
//   ✓ Punches through NAT, CGNAT, firewalls automatically
//   ✓ Cannot be blocked by IP — it has no IP
```

**Pros:** Maximum anonymity. Both parties' IPs completely hidden. Battle-tested (used by SecureDrop, OnionShare, Briar). Self-authenticating address.
**Cons:** Latency overhead of 200–800ms. Tor bootstrap takes 10–30 seconds on first start.

---

### 23.3 Solution 2 — I2P (Invisible Internet Project)

#### How I2P Differs from Tor

```
Tor:  Client → Guard → Middle → Exit → Internet
      (relies on central directory servers to find nodes)

I2P:  Fully distributed mesh — every node routes traffic FOR others
      Uses "garlic routing" — bundles multiple messages together
      like cloves of garlic, making individual message tracing harder

  Alice                                              Bob
    │                                                 │
  Outbound Tunnel (3 hops)              Inbound Tunnel (3 hops)
    │                                                 │
    └─────────── via other I2P peers ────────────────┘

  No node knows both source AND destination simultaneously.
```

```kotlin
// build.gradle.kts
implementation("net.i2p.android:client:2.3.0")

class I2PTransport @Inject constructor(
    private val i2pContext: I2PAppContext
) : P2PTransport {

    // Each user gets an I2P "destination" — their persistent anonymous address
    // Equivalent to .onion but fully peer-routed (no central directory)
    private lateinit var sessionManager: I2PSocketManager

    suspend fun initialize() {
        sessionManager = I2PSocketManagerFactory.createManager(i2pContext)
    }

    override suspend fun connect(peer: PeerDescriptor): Result<Unit> {
        val destination = peer.i2pDestination
            ?: return Result.failure(P2PChatError.PeerUnreachable(peer.id))
        val socket = withContext(Dispatchers.IO) {
            sessionManager.connect(Destination(destination))
        }
        // Same E2E Double Ratchet stack applied on top of I2P socket
        return setupEncryptedChannel(socket)
    }
}
```

**Pros:** More decentralized than Tor (no central directory nodes). Designed specifically for internal P2P communication. Better resistance to global passive adversary.
**Cons:** Smaller network = less anonymity set. Less mature Android support. Higher memory usage as your node routes traffic for others.

---

### 23.4 Solution 3 — Self-Hosted TURN Relay (Practical Middle Ground)

For WebRTC specifically, force all traffic through a relay so the peer never sees your real IP:

```kotlin
// RTCConfiguration — force RELAY only (no direct P2P connection)
val rtcConfig = PeerConnection.RTCConfiguration(
    listOf(
        // Your own self-hosted TURN server — you trust yourself, not a third party
        PeerConnection.IceServer.builder("turns:yourvps.example.com:5349")
            .setUsername("user")
            .setPassword(generateTurnCredential(secret = YOUR_HMAC_SECRET))
            .createIceServer()
    )
).apply {
    // RELAY = all traffic goes through TURN server
    // Peer sees only TURN server IP — never your real IP
    iceTransportsType = PeerConnection.IceTransportsType.RELAY

    // NOHOST = hide local network IPs (192.168.x.x, 10.x.x.x)
    // but still allows server-reflexive (your public IP visible)
    // Use RELAY for full IP hiding
}
```

#### Self-Hosted coturn Server (Docker)

```yaml
# docker-compose.yml — deploy on your own VPS ($5/month)
services:
  coturn:
    image: coturn/coturn:latest
    network_mode: host
    volumes:
      - ./turnserver.conf:/etc/coturn/turnserver.conf
      - ./certs:/etc/certs
    restart: unless-stopped

# turnserver.conf
listening-port=3478
tls-listening-port=5349
min-port=49152
max-port=65535
use-auth-secret
static-auth-secret=YOUR_CRYPTOGRAPHICALLY_STRONG_SECRET
realm=yourvps.example.com
cert=/etc/certs/fullchain.pem
pkey=/etc/certs/privkey.pem
no-stdout-log
log-file=/var/log/coturn.log
# CRITICAL: disable logging of peer IPs for maximum privacy
no-loopback-peers
no-multicast-peers
```

```kotlin
// Time-limited TURN credential generation (HMAC-SHA1)
// Credentials expire after 24 hours — prevents credential theft
fun generateTurnCredential(
    username: String = UUID.randomUUID().toString(),
    secret: String,
    ttlSeconds: Long = 86400
): Pair<String, String> {
    val expiry        = (System.currentTimeMillis() / 1000) + ttlSeconds
    val turnUsername  = "$expiry:$username"
    val hmac          = Mac.getInstance("HmacSHA1")
    hmac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA1"))
    val password      = Base64.encode(hmac.doFinal(turnUsername.toByteArray()))
    return Pair(turnUsername, password)
}
```

**What the adversary sees:**

```
Without self-hosted TURN:
  Alice(1.2.3.4) ◄──────────────────────────────► Bob(5.6.7.8)
  Each peer's real IP fully visible to the other

With self-hosted TURN (RELAY mode):
  Alice(1.2.3.4) ──► YourVPS(9.10.11.12) ◄── Bob(5.6.7.8)
  Alice sees only VPS IP — not Bob's real IP
  Bob sees only VPS IP — not Alice's real IP
  You (VPS owner) see both IPs — but you trust yourself
```

**Pros:** Low latency (50–100ms added). Transparent to users. Easy to deploy.
**Cons:** VPS becomes a metadata point. You must maintain the server. Not suitable if your VPS is seized.

---

### 23.5 Solution 4 — Message Padding (Hide Message Size)

Even with IP hidden, **message sizes reveal information**. A 3-byte message is "yes" or "no". A 50KB message is a photo. Padding defeats this:

```kotlin
object MessagePadding {

    private const val BLOCK_SIZE = 512  // All messages padded to multiples of 512 bytes

    // Pad plaintext to next block boundary before encryption
    // Size fingerprinting becomes impossible
    fun pad(plaintext: ByteArray): ByteArray {
        val targetSize = ((plaintext.size / BLOCK_SIZE) + 1) * BLOCK_SIZE
        val padded     = ByteArray(targetSize)
        plaintext.copyInto(padded)
        // Store real length in last 2 bytes (encrypted along with content)
        padded[targetSize - 2] = (plaintext.size shr 8).toByte()
        padded[targetSize - 1] = (plaintext.size and 0xFF).toByte()
        return padded
    }

    // Strip padding after decryption
    fun unpad(padded: ByteArray): ByteArray {
        val realSize = ((padded[padded.size - 2].toInt() and 0xFF) shl 8) or
                        (padded[padded.size - 1].toInt() and 0xFF)
        return padded.copyOf(realSize)
    }
}

// Usage in CryptoEngine
fun encryptMessage(plaintext: ByteArray, key: SecretKey): EncryptedPayload {
    val padded     = MessagePadding.pad(plaintext)   // pad BEFORE encrypting
    val iv         = generateSecureRandom(12)
    val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
    return EncryptedPayload(cipher.doFinal(padded), iv)
}
```

**Block sizes by content type:**

| Content | Unpadded Size | Padded To | Leaked Info |
|---|---|---|---|
| Short text | 1–100 bytes | 512 bytes | Nothing |
| Long text | 500–900 bytes | 1024 bytes | "It's a longer message" |
| Voice note | variable | next 4096 boundary | Rough duration only |
| Image thumbnail | variable | next 8192 boundary | Approximate size |

---

### 23.6 Solution 5 — Cover Traffic (Hide Communication Patterns)

Even with Tor + padding, **silence is information**. When Alice is not sending, the absence of traffic tells an observer she is not communicating. Cover traffic eliminates this signal:

```kotlin
class CoverTrafficManager @Inject constructor(
    private val transport: P2PTransport,
    private val crypto: CryptoEngine,
    private val config: PrivacyConfig
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Dummy message marker — recipient silently discards these
    // Indistinguishable from real messages to any outside observer
    private val DUMMY_MARKER = ByteArray(4) { 0xDE.toByte() }

    fun startCoverTraffic(peerId: String) = scope.launch {
        while (isActive && config.coverTrafficEnabled) {
            val dummyPayload = crypto.encryptMessage(
                plaintext = DUMMY_MARKER + ByteArray(508),  // Padded to 512 bytes
                peerId    = peerId
            )
            transport.send(dummyPayload)

            // Random interval so even the timing pattern carries no info
            delay(Random.nextLong(
                config.coverTrafficMinIntervalMs,   // e.g. 500ms
                config.coverTrafficMaxIntervalMs    // e.g. 5000ms
            ))
        }
    }

    fun stopCoverTraffic() = scope.cancel()
}
```

**What an observer sees:**

```
Without cover traffic:
  ──── silence ────[MSG]── silence ──── silence ────[MSG]──[MSG]────
                    ^                                 ^     ^
                    Alice messaged Bob at 10:04       burst at 14:22

With cover traffic:
  ─[d]─[d]─[MSG]─[d]─[d]─[d]─[d]─[MSG]─[MSG]─[d]─[d]─[d]─[MSG]─
  Constant stream. Observer cannot distinguish real from dummy.
  No communication pattern is visible whatsoever.

  d = encrypted dummy (512 bytes, indistinguishable from real message)
```

**Battery impact mitigation:**

```kotlin
data class PrivacyConfig(
    val coverTrafficEnabled: Boolean       = false,  // Off by default
    val coverTrafficMinIntervalMs: Long    = 2000,   // 2 second minimum
    val coverTrafficMaxIntervalMs: Long    = 10000,  // 10 second maximum
    val coverTrafficOnlyWhenCharging: Boolean = true,// Respect battery
    val coverTrafficOnlyOnWifi: Boolean   = true     // Respect data plan
)
```

---

### 23.7 Solution 6 — Mix Networks (Defeat Timing Correlation)

Even Tor is vulnerable to **global passive adversaries** who watch both ends of the connection and correlate packet timing. Mix networks add deliberate, randomized delays to defeat this:

```kotlin
class MixNetworkLayer @Inject constructor(
    private val transport: P2PTransport
) {
    private val mixPool = ArrayDeque<QueuedMessage>()
    private val poolMutex = Mutex()

    // Batch incoming messages and flush them in random order after delay
    // Breaks the 1:1 timing correlation between send and receive events
    suspend fun sendViaMix(payload: EncryptedPayload, peerId: String) {
        poolMutex.withLock {
            mixPool.add(QueuedMessage(payload, peerId, System.currentTimeMillis()))
        }
    }

    // Flush pool every N seconds in randomized order
    private fun startMixFlush() = scope.launch {
        while (isActive) {
            delay(Random.nextLong(1000, 5000))  // Random flush interval
            poolMutex.withLock {
                mixPool.shuffle()               // Randomize send order
                val batch = mixPool.toList()
                mixPool.clear()
                batch.forEach { queued ->
                    transport.send(queued.payload)
                    delay(Random.nextLong(0, 500)) // Small jitter between sends
                }
            }
        }
    }
}
```

**Without mixing vs. with mixing:**

```
Without mixing:
  Alice sends at  10:00:01.003 ────────────► Bob receives at 10:00:01.847
  Carol sends at  10:00:02.441 ────────────► Dave receives at 10:00:03.012
  Timing analysis trivially maps sender → receiver

With mixing (pool of 3 messages flushed together):
  Alice at 10:00:01 ─┐
  Carol at 10:00:02 ─┼── Mix Pool ──[shuffle+delay]──► Dave  at 10:00:07
  Eve   at 10:00:03 ─┘                                 Bob   at 10:00:08
                                                        Carol at 10:00:06
  Observer cannot determine which sender maps to which receiver.
```

---

### 23.8 Layered Defense-in-Depth Architecture

No single solution is complete. The real answer is layering:

```
┌──────────────────────────────────────────────────────────────────┐
│  LAYER 5: Cover Traffic      → hides when communication occurs  │
│  LAYER 4: Message Mixing     → hides timing correlation         │
│  LAYER 3: Message Padding    → hides message size/type          │
│  LAYER 2: Tor / I2P          → hides real IP addresses          │
│  LAYER 1: Self-Hosted TURN   → hides IP from WebRTC peers       │
│  LAYER 0: AES-256-GCM E2E    → hides message content            │
└──────────────────────────────────────────────────────────────────┘

What a nation-state adversary observing the network sees at each layer:

  Layer 0 only:   Alice(1.2.3.4) → Bob(5.6.7.8): [ENCRYPTED]
                  (content hidden, identities fully exposed)

  + Layer 1:      Alice(1.2.3.4) → TURN(9.10.11.12): [ENCRYPTED]
                  (IPs hidden from each other, TURN sees both)

  + Layer 2 (Tor):Unknown → Unknown: [ENCRYPTED]
                  (all IPs hidden from everyone)

  + Layer 3:      Unknown → Unknown: [512-BYTE BLOB]
                  (message size reveals nothing)

  + Layer 4:      Unknown → Unknown at unpredictable time: [BLOB]
                  (timing reveals nothing)

  + Layer 5:      Constant stream Unknown → Unknown: [BLOBS]
                  (even silence reveals nothing — adversary learns NOTHING)
```

---

### 23.9 TransportPrivacyOrchestrator — Unified Implementation

```kotlin
@Singleton
class TransportPrivacyOrchestrator @Inject constructor(
    private val torTransport: TorTransportWrapper,
    private val webRtcTransport: WebRtcTransport,
    private val mixLayer: MixNetworkLayer,
    private val coverTrafficManager: CoverTrafficManager,
    private val paddingLayer: MessagePaddingLayer,
    private val config: PrivacyConfig
) {
    suspend fun sendPrivate(
        plaintext: ByteArray,
        peerId: String,
        privacyLevel: PrivacyLevel
    ) {
        val padded = when {
            privacyLevel >= PrivacyLevel.STANDARD -> paddingLayer.pad(plaintext)
            else -> plaintext
        }

        val encrypted = cryptoEngine.encrypt(padded, peerId)

        when (privacyLevel) {
            PrivacyLevel.BASIC    -> webRtcTransport.send(encrypted)
            PrivacyLevel.STANDARD -> torTransport.send(encrypted)
            PrivacyLevel.HIGH     -> mixLayer.sendViaMix(encrypted, peerId)
            PrivacyLevel.MAXIMUM  -> {
                coverTrafficManager.ensureRunning(peerId)
                mixLayer.sendViaMix(encrypted, peerId)
            }
        }
    }
}

enum class PrivacyLevel {
    BASIC,      // WebRTC direct — fast, peer IPs visible to each other
    STANDARD,   // Tor + padding — IPs hidden, ~300ms extra latency
    HIGH,       // Tor + padding + mixing — timing hidden, ~1–3s extra latency
    MAXIMUM     // Tor + padding + mixing + cover traffic — nothing observable
}
```

---

### 23.10 Privacy Level — User-Facing Settings

```
┌──────────────────────────────────────────────────────────┐
│  Privacy Level                                           │
│                                                          │
│  ○ Standard  (Recommended)                               │
│    Encrypted. Peers cannot see your IP.                  │
│    Routing: Tor. Latency: +200–400ms.                    │
│                                                          │
│  ○ High                                                  │
│    Standard + timing protection.                         │
│    Routing: Tor + Message Mixing. Latency: +1–3s.        │
│                                                          │
│  ○ Maximum  (Journalist / Activist mode)                 │
│    High + continuous cover traffic.                      │
│    Nothing observable — not even silence.                │
│    Battery: higher usage. Recommended on charger + WiFi. │
│                                                          │
│  ○ Direct  (Speed priority)                              │
│    Fastest. Peer IPs visible to each other.              │
│    Use only on trusted local networks.                   │
└──────────────────────────────────────────────────────────┘
```

---

### 23.11 Threat Coverage Matrix — Final Summary

| Threat | E2E Encryption | Self-Hosted TURN | Tor/I2P | Message Padding | Cover Traffic | Mix Network |
|---|---|---|---|---|---|---|
| Read message content | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked |
| Know peer's real IP | ✗ Exposed | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked |
| Know your real IP | ✗ Exposed | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked | ✓ Blocked |
| Know message size/type | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✓ Blocked | ✓ Blocked | ✓ Blocked |
| Know when you communicated | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✓ Blocked | ✓ Blocked |
| Timing correlation attack | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✓ Blocked | ✓ Blocked |
| Traffic analysis (frequency) | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✗ Exposed | ✓ Blocked | ✓ Blocked |

> **Reference Implementation:** The open-source Android app **Briar** (briarproject.org) implements Tor + padding + cover traffic in production. Its source code is the definitive real-world reference for this entire section.

---

*Document Version: 3.0 | Updated: Metadata Privacy & IP Anonymization (Section 23) added | Architecture: Clean Architecture + MVVM | Platform: Android (Kotlin) | Classification: Technical Design Document*
