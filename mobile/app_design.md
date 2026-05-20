# ADAPT Patient Care Assistant - Android App Design & Implementation Guidelines

This document serves as the master blueprint for generating the ADAPT mobile application using Codex. To avoid exceeding context windows and to ensure a high-fidelity output that matches the Figma designs, the project must be built **step-by-step**, starting with a robust architectural skeleton.

## 1. Project Overview & Architecture Backbone

*   **Language:** Java
*   **Architecture:** MVVM (Model-View-ViewModel)
*   **UI Toolkit:** Standard Android XML Layouts (or Jetpack Compose if preferred, but assuming XML for standard Java MVVM approach).
*   **Data Binding:** ViewBinding enabled.
*   **Navigation:** Android Navigation Component (Single Activity Architecture preferred).

## 2. Implementation Strategy for Codex

**CRITICAL RULE FOR CODEX:** Do NOT attempt to build the entire app in one prompt. Follow the phased approach below. Only proceed to the next phase when the current phase is fully functional and verified.

### Phase 1: Minimal Skeleton Architecture (The Backbone)
*   **Objective:** Establish the directory structure, build dependencies, and core base classes.
*   **Actionable Steps:**
    1.  Setup `build.gradle` (ViewBinding, Navigation Component, ViewModel/LiveData, Material Design).
    2.  Create package structure: `ui`, `viewmodel`, `model`, `repository`, `utils`, `adapter`.
    3.  Create `MainActivity.java` and `activity_main.xml`.
    4.  Implement a `NavHostFragment` in `activity_main.xml`.
    5.  Define placeholder fragments for the 5 main tabs.

### Phase 2: Global UI Elements & Navigation
*   **Objective:** Implement the persistent outer shell of the app.
*   **Actionable Steps:**
    1.  Create the **Persistent Top Bar**: Should include contextual information (e.g., Active Patient Name, App Title, Settings icon).
    2.  Create the **5-Tab Bottom Navigation**:
        *   Profile
        *   Task History
        *   Routine
        *   Schedule
        *   Progress
    3.  Implement the **Centralized Floating AI Assistant ("Assist Mode")**: A persistent FAB (Floating Action Button) centered above the bottom nav that opens a floating window/bottom sheet.

### Phase 3: Tab 1 - Profile (Patient Management)
*   **Objective:** Build the patient list and detail views.
*   **UI/UX Flow:**
    *   **Main View:** A searchable list of patients, logically grouped (e.g., Active, Discharged, Pending).
    *   **Action:** Tapping a patient navigates to the Patient Profile Detail view.
*   **Technical Details:** Implement a `RecyclerView` with sticky headers or grouped items. Use a `SearchView` in the Top Bar or at the top of the layout. Create `PatientViewModel` to supply mock data.

### Phase 4: Tab 2 - Task History & Add Task Workflow
*   **Objective:** Manage current and past tasks.
*   **UI/UX Flow:**
    *   **Main View:** Chronological list of tasks.
    *   **Action:** Exclusive "Add Task" button located *only* in this tab.
    *   **Add Task Workflow:** Tapping the button opens a standardized, fixed-proportion window (DialogFragment or BottomSheetDialogFragment) to quickly log a new task without losing screen context.
*   **Technical Details:** Implement `TaskAdapter`. Ensure the Add Task window is clean and adheres strictly to the defined proportions in the design.

### Phase 5: Tab 3 - Routine Builder
*   **Objective:** Manage complex patient routines.
*   **UI/UX Flow:**
    *   **Main View:** A dynamically expandable interface for routine management.
    *   **Action:** Users can expand a routine category to see individual tasks with toggles to enable/disable them for a specific patient.
*   **Technical Details:** Use `ExpandableListView` or a `RecyclerView` with expandable items.

### Phase 6: Tab 4 - Schedule (Time-based Mapping)
*   **Objective:** Precise scheduling interface.
*   **UI/UX Flow:**
    *   **Main View:**
        1.  **Top:** Collapsible, interactive Calendar Widget for quick date selection.
        2.  **Bottom:** Scrollable 24-hour timeline showing tasks mapped to specific times.
*   **Technical Details:** Implement a custom or library-based horizontal calendar. Create a custom view or use a specialized library for the 24-hour timeline timeline visualization.

### Phase 7: Tab 5 - Progress & Analytics
*   **Objective:** Visualize patient progress over time.
*   **UI/UX Flow:** Data visualization dashboards (charts, graphs).
*   **Technical Details:** Integrate a lightweight charting library (e.g., MPAndroidChart) using placeholder data in the `ProgressViewModel`.

## 3. UI/UX Design System Directives

### Design Templates & Theming
Codex must strictly follow these design blueprints for color mapping and component styling:
*   **Light Mode Template:** [ADAPT Design System (Light)](file:///d:/ADAPT/Dumps/stitch_adapt_patient_care_assistant/adapt_design_system/DESIGN.md) - Use for the default daytime theme.
*   **Dark Mode Template:** [Premium Dark Narrative (Dark)](file:///d:/ADAPT/Dumps/stitch_adapt_patient_care_assistant/premium_dark_narrative/DESIGN.md) - Use for the high-end dark mode experience.

*   **Colors:** Do not use generic colors. Apply the harmonious tokens from the templates above.
*   **Typography:** Use modern, clean fonts (e.g., Inter, Roboto) via Google Fonts.
*   **Components:** Use Material 3 components modified for premium aesthetics (rounded corners, subtle elevations).
*   **Animations:** Include subtle micro-animations (ripples, smooth transitions).

---

## Example Codex Prompt: Phase 1 (Start Here)

> **"Codex, I am building the ADAPT Patient Care Assistant Android App in Java. Please act as a senior Android Developer.
> We are using MVVM, ViewBinding, and the Navigation Component.**
>
> **Execute Phase 1:**
> 1. Generate the `build.gradle` dependencies required for Material Design, Navigation Component, and ViewBinding.
> 2. Create the package structure (`ui`, `viewmodel`, `model`, `repository`).
> 3. Generate the `MainActivity.java` and `activity_main.xml`. The XML should contain a `FragmentContainerView` for navigation and a `BottomNavigationView`.
> 4. Generate 5 blank placeholder Fragments (ProfileFragment, TaskHistoryFragment, RoutineFragment, ScheduleFragment, ProgressFragment) and the corresponding navigation graph XML to link them.
>
> Do not generate any UI implementation for the fragments yet. Focus strictly on a compilable skeleton and navigation backbone."
