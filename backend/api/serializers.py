from rest_framework import serializers
from patients.models import Patient, PatientDisease, PatientMedication, PatientSensitivity, PatientDocument
from tasks.models import CareItem

class PatientDiseaseSerializer(serializers.ModelSerializer):
    class Meta:
        model = PatientDisease
        fields = '__all__'

class PatientMedicationSerializer(serializers.ModelSerializer):
    class Meta:
        model = PatientMedication
        fields = '__all__'

class PatientSensitivitySerializer(serializers.ModelSerializer):
    class Meta:
        model = PatientSensitivity
        fields = '__all__'

class PatientDocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = PatientDocument
        fields = '__all__'

class PatientSerializer(serializers.ModelSerializer):
    disease_entries = PatientDiseaseSerializer(many=True, read_only=True)
    medication_entries = PatientMedicationSerializer(many=True, read_only=True)
    sensitivity_entries = PatientSensitivitySerializer(many=True, read_only=True)
    documents = PatientDocumentSerializer(many=True, read_only=True)

    class Meta:
        model = Patient
        fields = '__all__'
        extra_kwargs = {
            'user': {'read_only': True},  # Auto-assigned from request.user
        }

class CareItemSerializer(serializers.ModelSerializer):
    class Meta:
        model = CareItem
        fields = '__all__'
