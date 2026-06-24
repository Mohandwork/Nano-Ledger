# Project Plan

An autonomous AI-powered personal finance tracker with dynamic UI, prohibited item filtering, and calendar/location enrichment.

## Project Brief
§
# Project Brief: Nano Ledger

Mini Ledger is an autonomous AI-driven finance assistant that transforms receipt images into context-aware expense reports. It dynamically adapts its interface and logic based on the specific details it discovers in your financial documents.

## Features
- **Dynamic AI Extraction & UI**: Leveraging generative AI to extract exhaustive data from any receipt. The application dynamically generates its input fields to match the AI's findings, allowing for flexible editing of category-specific metadata (e.g., flight numbers or hotel check-in dates).
- **Prohibited Item Intelligence**: An automated filtering system where the AI identifies and flags or excludes specific "prohibited" items (e.g., alcohol or personal luxury items) from business expense calculations.
- **Contextual Enrichment (Calendar & Location)**: Automatically cross-references transaction timestamps and locations with the user's calendar and GPS data to append event names, meeting descriptions, and verified merchant locations.
- **Adaptive Multi-pane Layout**: A responsive Material 3 interface built for phones and tablets, utilizing a list-detail pattern to manage the transaction review and permission handling workflows.

## High-Level Technical Stack
- **Kotlin**: Core language for high-performance Android development.
- **Jetpack Compose**: For building a fully dynamic UI that responds to AI-generated schemas.
- **Jetpack Navigation 3**: State-driven navigation to manage complex flows between scanning, dynamic editing, and history.
- **Compose Material Adaptive**: To provide a seamless, pane-based layout strategy across different screen sizes.
- **Google AI SDK (Gemini)**: The engine for autonomous receipt parsing, prohibited item classification, and dynamic UI schema generation.
- **Android Platform APIs**:
    - **Location Services**: For merchant verification and contextual correlation.
    - **Calendar Provider**: To fetch user events for transaction enrichment.
    - **Permissions API**: For secure handling of location and calendar access.
- **Kotlin Coroutines**: For non-blocking AI processing and system service queries.

## Implementation Steps
**Total Duration:** 36m 7s

### Task_1_DataLayer: Implement the core data layer including Room database, Transaction entity, DAO, and Repository.
- **Status:** COMPLETED
- **Updates:** Implemented Room database, Transaction entity, DAO, and Repository. Created MiniLedgerApplication for database initialization. Updated compileSdk and targetSdk to 35 for compatibility. Project builds successfully.
- **Acceptance Criteria:**
  - Room database and Transaction entity defined
  - DAO includes CRUD operations for transactions
  - Repository provides Flows for data
  - Project builds successfully
- **Duration:** 6m 2s

### Task_2_UI_Screens: Develop the Dashboard, Transaction List, and Add/Edit Transaction screens using Jetpack Compose and Material 3.
- **Status:** COMPLETED
- **Updates:** Implemented Dashboard, Transaction List, and Add/Edit screens using Jetpack Compose and Material 3. Created TransactionViewModel to manage state and repository interaction. Used a vibrant color scheme. Integrated screens into MainActivity with a simple navigation logic. Project builds successfully.
- **Acceptance Criteria:**
  - Dashboard displays balance, income, and expenses
  - Transaction list is scrollable and displays history
  - Add/Edit screen allows entering transaction details
  - ViewModel manages state and interacts with repository
- **Duration:** 3m 35s

### Task_3_Navigation_Adaptive: Set up Navigation 3 and implement an adaptive multi-pane layout with Edge-to-Edge support.
- **Status:** COMPLETED
- **Updates:** Integrated Navigation 3 and implemented an adaptive multi-pane layout using ListDetailPaneScaffold. Full Edge-to-Edge display implemented. App now adapts to phones, foldables, and tablets. Project builds successfully.
- **Acceptance Criteria:**
  - Navigation 3 handles screen transitions
  - Adaptive multi-pane layout (List-Detail) works on foldables and tablets
  - Full Edge-to-Edge display implemented
- **Duration:** 3m 32s

### Task_4_Gemini_Integration: Integrate Gemini AI SDK and implement AI-powered receipt scanning with specialized expense parsing.
- **Status:** COMPLETED
- **Updates:** Integrated Gemini AI SDK for receipt scanning. Implemented ReceiptProcessor using gemini-flash-latest. Integrated Photo Picker for receipt selection. Added review flow in AddTransactionScreen with auto-population of fields and specialized display for flight/hotel info. Securely handled API key via BuildConfig. Project builds successfully.
- **Acceptance Criteria:**
  - Gemini AI SDK integrated with API_KEY
  - Photo Picker implemented for receipt selection
  - AI extracts transaction details (amount, date, category) and specialized info (flights/hotels)
  - UI supports review and editing of AI-extracted data
  - Project builds successfully
- **Duration:** 12m 2s

### Task_5_Autonomous_AI_DynamicUI: Implement Dynamic AI Extraction, Dynamic UI generation, and Prohibited Item Intelligence using Gemini.
- **Status:** COMPLETED
- **Updates:** Refactored the app to a fully dynamic, AI-driven architecture. Removed all static fields (amount, category, isExpense) from the Transaction entity and UI. The UI is now entirely generated from the AI's response schema. Implemented heuristics to identify primary financial data (Amount, Type) from dynamic fields for dashboard calculations. Updated the data layer with dynamic persistence. Project builds successfully.
- **Acceptance Criteria:**
  - AI updated to extract exhaustive metadata and flag prohibited items (alcohol, luxury)
  - Compose UI dynamically generates input fields based on AI-generated schema
  - Prohibited items are identified and flagged in the transaction review flow
  - Project builds successfully
- **Duration:** 10m 56s

### Task_6_Contextual_Enrichment_Final_Polish: Implement Calendar/Location enrichment and finalize the app with UI polish and verification.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Merchant locations verified using GPS/Location services
  - Transaction timestamps cross-referenced with Calendar events for enrichment
  - Permissions for Location and Calendar handled gracefully
  - Vibrant Material 3 colors and adaptive app icon implemented
  - App builds and runs without crashes, all tests pass
  - Critic agent verifies stability and requirement alignment
- **StartTime:** 2026-06-11 19:55:28 EEST