from django.urls import path

from . import views

app_name = "patients"

urlpatterns = [
    path("", views.index, name="index"),
    path("add/", views.add_patient, name="add_patient"),
    path("<int:patient_id>/diseases/", views.disease_management, name="disease_management"),
    path("<int:patient_id>/diseases/<int:disease_id>/edit/", views.disease_edit, name="disease_edit"),
    path("<int:patient_id>/diseases/<int:disease_id>/delete/", views.disease_delete, name="disease_delete"),
    path("<int:patient_id>/sensitivities/<int:sensitivity_id>/edit/", views.sensitivity_edit, name="sensitivity_edit"),
    path("<int:patient_id>/sensitivities/<int:sensitivity_id>/delete/", views.sensitivity_delete, name="sensitivity_delete"),
    path("<int:patient_id>/medications/", views.medication_management, name="medication_management"),
    path("<int:patient_id>/medications/<int:medication_id>/edit/", views.medication_edit, name="medication_edit"),
    path("<int:patient_id>/medications/<int:medication_id>/delete/", views.medication_delete, name="medication_delete"),
]
