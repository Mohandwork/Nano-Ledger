This is a file containing the prompts that led to this app to showcase prompt engineering
The Agent relies heavily on "plan.md" file as it contains the structure and reasoning the Agent followed while creating everything. 

# Prompt N.1
You are an expert Principal Android Engineer. Scaffold a complete, production-ready Android application named "Mini ledger" using Kotlin, Jetpack Compose, and Material 3 design systems.

Follow strict Clean Architecture and Unidirectional Data Flow (MVI) principles. The codebase must be highly structured, modular in package design, fully functional, and compile on the first try with no unresolved symbols or placeholders.

ASK FOR DEPENDENCIES OR VAGUE REQURESTS INSTEAD OF MAKING THINGS UP

### 1. Toolchain, Build Dependencies, & SDK Installation
Configure the project with the following library versions and tooling specifications in the build files:
- Compile & Target SDK: 36
- Min SDK: 32 (To support local LocalDateTime and Calendar provider APIs natively)
- Dependency Injection: Hilt or Koin (choose one and scaffold all necessary Modules/Annotations)
- Concurrency: Kotlin Coroutines & Flow
- Media & Serialization: Coil (for image rendering), Kotlinx-Serialization (JSON handling)
- Local Jetpack Capabilities: Jetpack Room (for caching historical expense results)

### 2. Autonomous Agent & ADK 2.0 Integration Requirements
The agent must configure and integrate the Agent Development Kit (ADK 2.0) into the data layer:
1. Ensure the app imports the official ADK 2.0 dependencies in `build.gradle.kts` dependencies {
   implementation("com.google.adk:google-adk-kotlin-core-android:0.1.0")
   ksp("com.google.adk:google-adk-kotlin-processor:0.1.0")
   }.
2. Build an `AgentConfig` manager that instantiates an on-device local execution agent powered by `GeminiModels.Nano` via ML Kit.
3. Configure the agent to operate strictly in `OperatingMode.SINGLE_TURN`, enforcing an output schema mapped to the Kotlin data class `SmartExpenseResult`.
4. Register two native Kotlin system functions as executable 'Skills/Tools' that the ADK runtime engine can invoke dynamically during text processing:
    - `fetchDeviceLocation()`: Queries current latitude/longitude coordinates.
    - `fetchActiveCalendarEvents()`: Reads the user's primary Android calendar to extract active event titles matching the current timestamp.

### 3. Permissions & System Hardware Integration
Implement robust, elegant runtime permission handling within the Compose UI layer using Accompanist Permissions or native Accompanist-free Contracts. The app must cleanly request, explain, and verify the following system manifest privileges:
1. Camera & Gallery Access: `android.permission.CAMERA`, and media reading capability (`READ_MEDIA_IMAGES` or `READ_EXTERNAL_STORAGE` depending on API level fallback) to allow taking a picture or picking a receipt invoice file.
2. Fine Location Access: `android.permission.ACCESS_FINE_LOCATION` and `android.permission.ACCESS_COARSE_LOCATION` to feed geospatial data into the context object.
3. Calendar Access: `android.permission.READ_CALENDAR` to pull active business meeting contexts.

Provide full rationale UI states (Material 3 Dialogs) explaining *why* these permissions are required if the user initially denies them, and handle graceful fallbacks (e.g., if calendar permission is blocked, the agent executes with a null calendar context instead of crashing).

### 4. Code Architecture Layering Blueprint
Organize the code into the following package structure:

- `data/model/`:
    - `SmartExpenseResult`: Type-safe Kotlin data class matching the structural schema: `merchantName: String`, `transactionDate: String`, `lineItems: List<ReceiptLineItem>`, `taxAmount: Double`, `totalAmount: Double`, `suggestedCategory: ExpenseCategory (Enum)`, `policyCheck: PolicyStatus (Enum)`.
    - Enums for `ExpenseCategory` (Food, Lodging, Transport, Other) and `PolicyStatus` (APPROVED, VIOLATION_EXCEEDED_LIMIT, VIOLATION_RESTRICTED_ITEM).
- `data/repository/`:
    - `ExpenseRepositoryImpl`: Implements the data extraction orchestration loop. It requests permissions, hooks into the location and calendar system providers, packages those inputs alongside the captured receipt bitmap, executes the ADK agent loop, evaluates policy constraints (flagging a violation if Category == Food && total > $50 or any item description contains "Alcohol"), and saves the resulting `SmartExpenseResult` into a local Room database entity.
- `presentation/`:
    - `ExpenseContract`: Contains immutable `ExpenseState` classes (Loading, Idle, RequestingPermissions, Success, Error) and UI `ExpenseIntent` entries (ScanReceipt, SelectImage, ResolvePolicyWarning).
    - `ExpenseViewModel`: Manages state transformations using Coroutine StateFlows. Handles asynchronous background execution loops so the main thread remains completely unblocked during AI inference.
- `ui/screens/`:
    - `DashboardScreen`: Features a clean spending tracking indicator layout, custom list cards showing historical scans, and a Floating Action Button that handles permission checks before prompting a simulated device camera/gallery file picker selector.
    - `DetailScreen`: A highly animated, visually rich UI that populates once the ADK agent completes execution. If `PolicyStatus` is flagged as a violation, display a prominent warning state card utilizing smooth Jetpack Compose layout animations to explicitly point out the non-compliant items.

Generate every single code block completely. Write all imports, remove any ambiguous comment placeholders like "// TODO: Implement logic here", and ensure all components are tightly bound via dependency injection.

# prompt N.2
Add ADK dependencies and leverage photo picker along with Gen AI to extract info and implement the logic

# prompt N.3
Let's do a few edits on the add entry screen. The idea is to make the agent make decisions on it's own and extract every and all possible info from different types of reciepts. So, let's remove the static fields from this area
we make the agent extract all the info and based on that the UI is created and fields are added with the possibility to edit them. We will also add a PROHIBITED ITEMS list as to make the agent take action on his own to not add them as expenses.
Finally, the last idea was to get location and calendar permission to cross-reference the calendar of these recipts and see if the user added any extra info ( Event name , location or any useful info ) that the agent can add in the expense report.


From here on it was small edits. It's not great to create a huge request for the Agent all at once as it might miss things or hallucinate.
It's advisable to make small changes one at a time to avoid long thinking process and token consumption.