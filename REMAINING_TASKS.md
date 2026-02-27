# Remaining Tasks for P2P Chat App (Post-Phase 3)

Following the completion of Phase 3 (Group Messaging), the following components from `P2P_Chat_App_Architecture-1.md` remain to be implemented:

## 1. Privacy & Anonymity (Section 23) - **High Priority**
The core differentiation of this app is metadata privacy. Currently, we use direct IP (WebRTC/WiFi Direct).
- [ ] **Tor Transport:** Implement `TorTransportWrapper` using `tor-android` (Section 23.2).
- [ ] **Message Padding:** Implement `MessagePadding` to pad messages to fixed block sizes (512b) to prevent size fingerprinting (Section 23.5).
- [ ] **Cover Traffic:** Implement `CoverTrafficManager` to send dummy messages during idle periods (Section 23.6).
- [ ] **Privacy Settings UI:** Add screens to configure Privacy Level (Standard/High/Max) (Section 23.10).

## 2. Advanced Media Features
- [ ] **Voice Notes:** Implement audio recorder, encrypted blob storage, and playback UI (Section 20).
- [ ] **Message Reactions:**
    - Full implementation of `Reaction` storage (currently a placeholder in `MessageProcessor`).
    - UI: Long-press context menu on messages to pick emoji.
    - UI: Display reactions bubble with counts.

## 3. Identity Management (Section 22)
- [ ] **Multi-Identity / Burner System:**
    - UI to create "Burner" identities.
    - Logic to separate database contexts or tag data by `local_identity_id`.
    - "Burn" action to wipe keys and associated data.
- [ ] **Key Verification Ceremony:**
    - Enhance `PeerDetailsScreen` to show the 12-word safety number (implemented basic view).
    - Add QR Code scanning specifically for *verifying* an existing peer (comparing fingerprints).

## 4. UI/UX Refinements
- [ ] **True Threaded View:** Upgrade `ChatScreen` from a flat list to a recursive tree view (or collapsible threads) as described in Section 9.4.
- [ ] **Search Navigation:** ensure clicking a search result in `ConversationsScreen` jumps to the specific message in history, loading surrounding context (Section 11.1).

## 5. Testing Strategy (Section 18)
- [ ] **Unit Tests:** Expand coverage for `RatchetEngine`, `GroupCipher`, and `MessageRepository`.
- [ ] **UI Tests:** Add Espresso/Compose tests for the critical "Alice messages Bob" flow.
