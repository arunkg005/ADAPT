# ADAPT: Advanced Digital Assistant for Patient Tracking

ADAPT is a comprehensive, multi-platform caretaker application designed to centralize patient care management. It leverages a powerful Django backend to serve both a web dashboard and a native Android mobile application, providing intelligent assistance through a Gemini-powered AI engine.

## 🌟 Platform Ecosystem

ADAPT operates on a **3-tier architecture**:
1.  **Centralized Backend API**: A Django + PostgreSQL core that handles all business logic, database operations, and AI processing.
2.  **Web Dashboard**: A professional interface for administrative control and detailed clinical management.
3.  **Native Mobile App**: A Java-based Android application designed for on-the-go care, featuring an MVVM architecture and real-time AI assistance.

---

## 🚀 Key Features

### 1. Centralized Patient & Clinical Records
*   **Comprehensive Profiles**: Manage patient data, medical history, and clinical sensitivities.
*   **AI Summaries**: Automatically generate context-aware patient summaries using Gemini AI.
*   **Real-time Synchronization**: Unified data across Web and Mobile platforms via REST API.

### 2. Intelligent Task & Routine Management
*   **Flexible Scheduling**: Manage tasks, routines, and clinical schedules with complex recurrence patterns.
*   **Prioritization Engine**: Intelligent task tracking with visual priority indicators.
*   **Routine Builder**: Streamlined workflow for creating and assigning care routines.

### 3. Gemini-Powered AI Assistant
*   **Contextual Assistance**: An AI assistant that understands the patient's full clinical profile.
*   **Cross-Platform Chat**: Real-time AI chat interface on both Web and Mobile.
*   **Action Proposals**: The AI can propose actionable tasks or schedule changes directly within the chat for caretaker approval.

### 4. Robust Security & API
*   **JWT Authentication**: Secure, stateless authentication for the mobile client using `djangorestframework-simplejwt`.
*   **CORS Enabled**: Configured for seamless cross-origin communication between platforms.

---

## 🛠 Tech Stack

### Backend (API & Logic)
*   **Framework**: Django 6.x, Django REST Framework (DRF)
*   **Database**: PostgreSQL + `pgvector` (for AI embeddings)
*   **AI**: Google Gemini API (`google-generativeai`)
*   **Auth**: SimpleJWT (JSON Web Tokens)

### Mobile Client
*   **Language**: Java (Android SDK)
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Networking**: Retrofit 2 + OkHttp + Gson

---

## 📁 Project Structure

```
ADAPT/
├── adapt/           # Core Django settings and project configuration
├── api/             # REST API app (Serializers, ViewSets, JWT routing)
├── assistant/       # AI Engine, chat sessions, and Gemini integration
├── patients/        # Patient clinical records and AI summaries
├── tasks/           # Care items, routines, and scheduling logic
├── dashboard/       # Web-based caretaker dashboard
├── Adapt_app/       # Native Android Application source code
│   └── app/src/main/java/com/example/adapt/
│       ├── api/        # Retrofit API clients and service definitions
│       ├── model/      # Java data models mapped from Django serializers
│       ├── repository/ # Data repositories (API + Mock)
│       └── viewmodel/  # Logic for UI fragments
└── manage.py        # Django CLI entrypoint
```

---

## ⚙️ Setup & Installation

### 1. Backend Setup
1.  **Environment**: Create a `.env` file with `DATABASE_URL`, `GEMINI_API_KEY`, and `SECRET_KEY`.
2.  **Dependencies**: `pip install -r requirements.txt`
3.  **Database**: 
    ```bash
    python manage.py makemigrations
    python manage.py migrate
    ```
4.  **Admin User**: `python manage.py createsuperuser`
5.  **Run**: `python manage.py runserver`

### 2. Mobile App Setup
1.  **Android Studio**: Open the `Adapt_app` directory.
2.  **API URL**: Ensure `RetrofitClient.java` points to your local IP or `10.0.2.2` for the emulator.
3.  **Build**: Sync Gradle and run on an Android device or emulator.

---

## 🔮 Future Roadmap
*   **Role-Based Access**: Specialized views for Doctors vs. Caretakers.
*   **Voice Integration**: Voice-to-task commands for the mobile app.
*   **Compliance**: HIPAA/GDPR readiness hardening.
*   **Offline Mode**: local SQLite caching for the mobile app.
