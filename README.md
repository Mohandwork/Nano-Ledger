# Nano Ledger 🧾

Nano Ledger is a modern, AI-powered Android application designed to simplify expense tracking and corporate compliance management. By leveraging Google's Gemini AI, the app can intelligently "read" receipts, extract detailed transaction data, and verify them against corporate policies in real-time.

## 🚀 Features

- **AI-Powered Receipt Scanning**: Capture or upload receipt images to automatically extract merchant details, line items, prices, tax, and totals.
- **Corporate Compliance Engine**: Automatically analyzes line items to flag policy violations such as restricted purchases (alcohol, tobacco) or spending limit breaches.
- **Contextual Intelligence**: Integrates with calendar events to automatically suggest "Business Reasons" for transactions (e.g., matching a dinner receipt to a "Client Meeting" on your calendar).
- **Transaction Management**: A comprehensive dashboard and searchable history of all expenses.
- **Location Awareness**: Tag transactions with locations to provide a full audit trail.
- **Smart Resolutions**: Provides inline action suggestions for flagged items (e.g., "Mark as Personal" or "Exclude Item").

## 🛠 Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 design.
- **AI Integration**: 
    - [Google Gemini API](https://ai.google.dev/) for multimodal receipt analysis.
    - [Google ADK for Kotlin](https://github.com/google/ai-edge-android-sdk) for structured AI agent workflows.
- **Architecture**: MVVM (Model-View-ViewModel) following Clean Architecture principles.
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room) for persistent, offline-first storage.
- **Concurrency**: Kotlin Coroutines and StateFlow for reactive UI updates.
- **Image Processing**: [Coil](https://coil-kt.github.io/coil/) for efficient image loading.
- **Serialization**: Kotlinx Serialization and Moshi.
- **Dependency Management**: Gradle Version Catalogs (libs.versions.toml).

## 🏗 Architecture

The project is structured to promote separation of concerns and testability:

- **`ui/`**: Contains the Compose screens, components, and ViewModels. It follows a unidirectional data flow (UDF) pattern.
- **`ai/`**: Houses the `ReceiptProcessor` and AI model definitions. This layer handles the prompts and logic for interacting with Gemini.
- **`data/`**: Manages the data layer, including the Room database, DAOs, and the Repository that abstracts data sources from the rest of the app.
- **`service/`**: Integration points for system services like `CalendarService` and `LocationService`.

## 🚦 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Mohandwork/Nano-Ledger.git
   ```
2. **Add Gemini API Key**:
   Create a `local.properties` file in the root directory (if it doesn't exist) and add your API key:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
3. **Build and Run**:
   Open the project in Android Studio (Ladybug or newer recommended) and run the `app` module.

## ⚖️ License

Distributed under the MIT License. See `LICENSE` for more information.
