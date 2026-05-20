import re

with open('dashboard/views.py', 'r', encoding='utf-8') as f:
    content = f.read()

# Add imports
imports = """from django.contrib.auth.decorators import login_required
from django.contrib.auth import login
from django.contrib.auth.forms import UserCreationForm
"""
content = content.replace('from django.utils import timezone\n', 'from django.utils import timezone\n' + imports)

# _dashboard_context
content = content.replace('def _dashboard_context(profile, active_nav, extra=None):', 'def _dashboard_context(request, profile, active_nav, extra=None):')
content = content.replace('"patients": _all_patients_for_dashboard(),', '"patients": _all_patients_for_dashboard(request.user),')

# Update references to _dashboard_context
content = re.sub(r'_dashboard_context\(\s*caretaker_profile,', r'_dashboard_context(request,\n\t\t\tcaretaker_profile,', content)
content = re.sub(r'_dashboard_context\(\s*profile,', r'_dashboard_context(request,\n\t\t\tprofile,', content)

# _selected_patient_instance_from_request
content = content.replace('_resolve_patient_instance(int(patient_id))', '_resolve_patient_instance(int(patient_id), request.user)')

# _ensure_demo_patients
content = content.replace('def _ensure_demo_patients():', 'def _ensure_demo_patients(user):')
content = content.replace('name="Asha Verma (Demo)",\n\t\tdefaults={', 'name="Asha Verma (Demo)",\n\t\tuser=user,\n\t\tdefaults={')
content = content.replace('name="Raghav Singh (Demo)",\n\t\tdefaults={', 'name="Raghav Singh (Demo)",\n\t\tuser=user,\n\t\tdefaults={')
content = content.replace('Patient.objects.filter(name__in=["Asha Verma (Demo)", "Raghav Singh (Demo)"])', 'Patient.objects.filter(user=user, name__in=["Asha Verma (Demo)", "Raghav Singh (Demo)"])')

# _routine_workspace_payload
content = content.replace('def _routine_workspace_payload(request, selected_patient_instance):\n\tpatients = _all_patients_for_dashboard()', 'def _routine_workspace_payload(request, selected_patient_instance):\n\tpatients = _all_patients_for_dashboard(request.user)')

# _schedule_workspace_payload
content = content.replace('def _schedule_workspace_payload(selected_patient_instance):\n\tpatients = _all_patients_for_dashboard()', 'def _schedule_workspace_payload(user, selected_patient_instance):\n\tpatients = _all_patients_for_dashboard(user)')

# _get_or_create_caretaker_profile
content = content.replace('def _get_or_create_caretaker_profile():', 'def _get_or_create_caretaker_profile(user):')
content = content.replace('profile = CaretakerProfile.objects.order_by("id").first()', 'profile = CaretakerProfile.objects.filter(user=user).first()')
content = content.replace('return CaretakerProfile.objects.create(**CARETAKER_DEFAULTS)', 'return CaretakerProfile.objects.create(user=user, **CARETAKER_DEFAULTS)')

# _resolve_patient_instance
content = content.replace('def _resolve_patient_instance(patient_id):', 'def _resolve_patient_instance(patient_id, user):')
content = content.replace('_ensure_demo_patients()', '_ensure_demo_patients(user)')
content = content.replace('Patient.objects.prefetch_related("documents").get(id=patient_id)', 'Patient.objects.prefetch_related("documents").get(id=patient_id, user=user)')
content = content.replace('Patient.objects.prefetch_related("documents").get(name=legacy_name)', 'Patient.objects.prefetch_related("documents").get(name=legacy_name, user=user)')

# _find_patient_or_404
content = content.replace('def _find_patient_or_404(patient_id):', 'def _find_patient_or_404(patient_id, user):')
content = content.replace('_resolve_patient_instance(patient_id)', '_resolve_patient_instance(patient_id, user)')

# _selected_patient_from_request
content = content.replace('_find_patient_or_404(int(patient_id))', '_find_patient_or_404(int(patient_id), request.user)')

# _all_patients_for_dashboard
content = content.replace('def _all_patients_for_dashboard():', 'def _all_patients_for_dashboard(user):')
content = content.replace('Patient.objects.prefetch_related("documents").order_by("name")', 'Patient.objects.prefetch_related("documents").filter(user=user).order_by("name")')

# views
content = content.replace('def home(request):', '@login_required\ndef home(request):')
content = content.replace('def patient_dashboard(request, patient_id):', '@login_required\ndef patient_dashboard(request, patient_id):')
content = content.replace('def patient_settings(request, patient_id):', '@login_required\ndef patient_settings(request, patient_id):')
content = content.replace('def task_lab(request):', '@login_required\ndef task_lab(request):')
content = content.replace('def schedule_routine(request):', '@login_required\ndef schedule_routine(request):')
content = content.replace('def schedule_window(request):', '@login_required\ndef schedule_window(request):')
content = content.replace('def progress(request):', '@login_required\ndef progress(request):')
content = content.replace('def profile_settings(request):', '@login_required\ndef profile_settings(request):')

content = content.replace('_all_patients_for_dashboard()', '_all_patients_for_dashboard(request.user)')
content = content.replace('_get_or_create_caretaker_profile()', '_get_or_create_caretaker_profile(request.user)')
content = content.replace('_find_patient_or_404(patient_id)', '_find_patient_or_404(patient_id, request.user)')
content = content.replace('_resolve_patient_instance(patient_id)', '_resolve_patient_instance(patient_id, request.user)')
content = content.replace('_schedule_workspace_payload(selected_patient_instance)', '_schedule_workspace_payload(request.user, selected_patient_instance)')

# signout
content = content.replace('def signout(request):', '@login_required\ndef signout(request):')

# register view
register_code = """
def register(request):
	if request.method == "POST":
		form = UserCreationForm(request.POST)
		if form.is_valid():
			user = form.save()
			login(request, user)
			return redirect("dashboard:home")
	else:
		form = UserCreationForm()
	return render(request, "registration/register.html", {"form": form})
"""
content += register_code

with open('dashboard/views.py', 'w', encoding='utf-8') as f:
    f.write(content)
