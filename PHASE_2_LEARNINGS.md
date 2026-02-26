# Phase 2 Learnings

## Technical Challenges & Solutions

1.  **WorkManager & Hilt Injection**:
    -   **Issue**: App crashed on startup with .
    -   **Cause**: Hilt's  was not being provided to WorkManager. Default initialization was failing.
    -   **Solution**: Implemented  in  (Application class) and returned a  using the injected . Disabled default initialization in  (implied/standard practice).

2.  **Room Foreign Key Constraints**:
    -   **Issue**: Crash when receiving a message from a new peer: .
    -   **Cause**:  has a foreign key to . When a message arrived from a new peer, the  didn't exist yet.
    -   **Solution**: Updated  (and ) to check for thread existence. If missing, it creates a new  (type 1:1) before inserting the message.

3.  **Navigation & UI Gaps**:
    -   **Issue**: Users could connect to peers but had no way to initiate a chat from the UI.
    -   **Solution**: Added a "Start Chat" button to  and wired the navigation in  to open the  with the peer's ID.

4.  **Protocol & Data Model**:
    -   **Reactions**: Implemented  message type in . Added storage in  as a JSON map.
    -   **Disappearing Messages**: Added  to  and  to . Implemented logic to calculate expiration based on thread settings.

## Architecture Validation
-   The clean architecture with  (crypto, network, storage) and  modules is holding up well.
-    handles a lot of complexity (encryption, db, network). It might be worth refactoring  logic further to decouple it from ViewModels if it grows.

## Next Steps (Phase 3)
-   Refine UI/UX (e.g., better feedback for connection status).
-   Comprehensive testing (Unit & UI tests).
-   Performance tuning (large message lists, image loading).
