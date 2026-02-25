# Remaining Tasks for P2P Chat App

Based on the architectural blueprint (`P2P_Chat_App_Architecture-1.md`) and the current codebase state, the following features and components remain to be implemented:

## 1. Core Features
- **Group Messaging (Sender Keys):** The current implementation supports 1:1 Double Ratchet encryption. The architecture (Section 6.3) requires Sender Keys for efficient multi-party encryption.
- **Voice Notes:** Section 20 mentions encrypted audio blobs. Recording and playback UI + attachment handling need implementation.
- **Message Reactions:** The data model includes `reactions`, but the UI and protocol handling for adding/removing reactions are missing.

## 2. Privacy & Security
- **Safety Number Verification:** Section 15.2 and 22.6 describe a key verification ceremony (QR/Fingerprint comparison) to prevent MITM attacks. The UI for comparing safety numbers is missing.
- **Self-Hosted TURN:** Section 23.4 describes configuring custom TURN servers for WebRTC privacy. Currently, Google's STUN is hardcoded.
- **I2P Transport:** Section 23.3 lists I2P as an alternative anonymity network. Only Tor is implemented.

## 3. UI/UX Refinement
- **Threaded View:** Section 9.4 describes a recursive tree view for nested replies. The current `ChatScreen` uses a flat list (Paging 3).
- **Topic Filtering:** While tagging exists, a dedicated `TopicsScreen` (Section 11.1) to browse and filter messages by tag is missing.
- **Deep Link Handling:** `MainActivity` has basic intent handling, but the full parsing of `p2pchat://` URIs with identity keys and signatures (Section 21.2) needs full verification and wiring to `PeersViewModel`.

## 4. Search
- **Full Text Search Wiring:** `MessageFtsEntity` and `SearchBar` exist, but the complete flow from UI query -> ViewModel -> Repository -> FTS Query -> UI Update needs final wiring and testing.

## 5. Testing
- **UI/E2E Tests:** Section 18.1 specifies a 10% allocation for UI/E2E tests (Espresso/Compose Test), which are currently minimal.
