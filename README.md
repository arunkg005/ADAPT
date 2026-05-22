# ADAPT: Advanced Digital Assistant for Patient Tracking

ADAPT is a comprehensive caretaker application designed to centralize patient care management. It leverages a powerful Django backend and a modern, responsive web dashboard to serve caregivers on both desktop and mobile devices, providing intelligent assistance through a Gemini-powered AI engine.

## 🌟 Platform Ecosystem

ADAPT operates on a **unified web architecture**:
1.  **Centralized Backend**: A Django + PostgreSQL core that handles all business logic, database operations, and AI processing.
2.  **Responsive Web Dashboard**: A professional interface optimized for all screen sizes (desktop, tablet, mobile) for administrative control, detailed clinical management, task tracking, and real-time AI assistance.

---

## 🚀 Key Features

### 1. Centralized Patient & Clinical Records
*   **Comprehensive Profiles**: Manage patient data, medical history, and clinical sensitivities.
*   **AI Summaries**: Automatically generate context-aware patient summaries using Gemini AI.
*   **Unified Interface**: Consistent data access across all caregiver devices.

### 2. Intelligent Task & Routine Management
*   **Flexible Scheduling**: Manage tasks, routines, and clinical schedules with complex recurrence patterns.
*   **Prioritization Engine**: Intelligent task tracking with visual priority indicators.
*   **Routine Builder**: Streamlined workflow for creating and assigning care routines.

### 3. Gemini-Powered AI Assistant ("Assist Mode")
*   **Contextual Assistance**: An AI assistant that understands the patient's full clinical profile.
*   **Dynamic Chat**: Real-time AI chat interface embedded as a sliding drawer on desktop and mobile viewports.
*   **Action Proposals**: The AI can propose actionable tasks or schedule changes directly within the chat for caretaker approval.

### 4. Robust Security & Extensions
*   **CORS Enabled**: Configured for secure cross-origin communication between backend services.
*   **JWT Capability**: Django REST Framework JWT authentication configured for decoupled endpoints.

---

## 🛠 Tech Stack

### Backend & Frontend
*   **Backend Framework**: Django 6.x, Django REST Framework (DRF)
*   **Frontend UI**: Django Templates, Vanilla CSS/JS, TailwindCSS (for select views)
*   **Database**: PostgreSQL + `pgvector` (for AI embeddings)
*   **AI Engine**: Google Gemini API (`google-generativeai`)

---

## 📁 Project Structure

```
ADAPT/
├── backend/
│   ├── adapt/         # Core Django settings and project configuration
│   ├── api/           # REST API app (Serializers, ViewSets, JWT routing)
│   ├── assistant/     # AI Engine, chat sessions, and Gemini integration
│   ├── patients/      # Patient clinical records and AI summaries
│   ├── tasks/         # Care items, routines, and scheduling logic
│   └── dashboard/     # Web-based caretaker dashboard
├── web/
│   └── templates/     # Django templates (landing, dashboard, assistant, etc.)
└── manage.py          # Django CLI entrypoint
```

---

## ⚙️ Setup & Installation

### Setup Instructions
1.  **Environment**: Create a `.env` file inside the `backend` directory with `DATABASE_URL`, `GEMINI_API_KEY`, and `SECRET_KEY`.
2.  **Dependencies**: Navigate to `backend` and install dependencies:
    ```bash
    pip install -r requirements.txt
    ```
3.  **Database**: Run migrations:
    ```bash
    python manage.py makemigrations
    python manage.py migrate
    ```
4.  **Admin User**: Create a superuser to access the admin site:
    ```bash
    python manage.py createsuperuser
    ```
5.  **Run Development Server**: Run the Django server:
    ```bash
    python manage.py runserver
    ```
6.  **Access App**: Open `http://127.0.0.1:8000` in your web browser. Resize the window or inspect with mobile developer tools to view the optimized mobile layout.

---

## 🔮 Future Roadmap
*   **Role-Based Access**: Specialized views for Doctors vs. Caretakers.
*   **Web Voice Integration**: Hands-free voice-to-task commands natively in the web application.
*   **Compliance**: HIPAA/GDPR readiness hardening.
