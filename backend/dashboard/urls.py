from django.urls import path

from . import views

app_name = "dashboard"

urlpatterns = [
    path("", views.home, name="home"),
    path("task-lab/", views.task_lab, name="task_lab"),
    path("schedule-routine/", views.schedule_routine, name="schedule_routine"),
    path("schedule/", views.schedule_window, name="schedule"),
    path("progress/", views.progress, name="progress"),
    path("analysis/", views.progress, name="analysis"),
    path("patients/<int:patient_id>/settings/", views.patient_settings, name="patient_settings"),
    path("patients/<int:patient_id>/", views.patient_dashboard, name="patient_dashboard"),
    path("settings/profile/", views.profile_settings, name="profile_settings"),
    path("signout/", views.signout, name="signout"),
]
