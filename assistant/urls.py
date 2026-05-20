from django.urls import path

from . import views

app_name = "assistant"

urlpatterns = [
    path("", views.index, name="index"),
    path("patient/<int:patient_id>/", views.index, name="patient_index"),
    path("close-session/", views.close_session_on_tab_close, name="close_session"),
]
