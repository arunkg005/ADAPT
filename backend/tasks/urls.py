from django.urls import path

from . import views

app_name = "tasks"

urlpatterns = [
    path("", views.index, name="index"),
    path("patient/<int:patient_id>/<str:item_type>/", views.item_list, name="item_list"),
    path("patient/<int:patient_id>/<str:item_type>/add/", views.item_create, name="item_create"),
    path("patient/<int:patient_id>/<str:item_type>/<int:item_id>/edit/", views.item_edit, name="item_edit"),
    path("patient/<int:patient_id>/<str:item_type>/<int:item_id>/delete/", views.item_delete, name="item_delete"),
]
