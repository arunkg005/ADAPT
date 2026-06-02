import os
import json
import re
from typing import Iterable

from PIL import Image

try:
    import google.generativeai as genai
except ImportError:  # pragma: no cover - exercised on clean deploy targets
    genai = None


PLACEHOLDER_KEY = "replace-with-your-gemini-api-key"


def _build_prompt(patient, documents):
    report_count = sum(1 for doc in documents if doc.category == "report")
    prescription_count = sum(1 for doc in documents if doc.category == "prescription")
    medication_count = sum(1 for doc in documents if doc.category == "medication")

    return (
        "You are a clinical care assistant helping a caretaker. "
        "Create a detailed, practical patient summary in clear sections: "
        "Patient Overview, Disease History, Allergies and Risks, Current Scenario, "
        "Medication and Prescription Context, Care Priorities for Today, and Warnings. "
        "Use concise bullet points where possible.\n\n"
        f"Name: {patient.name}\n"
        f"Age: {patient.age}\n"
        f"Gender: {patient.get_gender_display()}\n"
        f"Allergies: {patient.allergies}\n"
        f"Disease history: {', '.join(patient.history_diseases)}\n"
        f"Current scenario: {patient.current_scenario_description}\n"
        f"Doctor guidelines: {patient.doctor_guidelines or 'Not provided'}\n"
        f"Uploaded report documents: {report_count}\n"
        f"Uploaded prescription documents: {prescription_count}\n"
        f"Uploaded medication documents: {medication_count}\n"
    )


def _fallback_summary(patient, documents):
    lines = [
        f"Patient: {patient.name} ({patient.age}, {patient.get_gender_display()})",
        f"Disease history: {', '.join(patient.history_diseases)}",
        f"Allergies: {patient.allergies}",
        f"Current scenario: {patient.current_scenario_description}",
        f"Doctor guidelines: {patient.doctor_guidelines or 'Not provided'}",
        "Document categories uploaded:",
    ]
    for doc in documents:
        lines.append(f"- {doc.get_category_display()}: {doc.file.name}")
    lines.append("AI summary was not generated because Gemini is unavailable or the API key is missing/invalid.")
    return "\n".join(lines)


def generate_patient_summary(patient, documents: Iterable):
    documents = list(documents)
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    prompt = _build_prompt(patient, documents)

    if genai is None or not api_key or api_key == PLACEHOLDER_KEY:
        return _fallback_summary(patient, documents)

    try:
        genai.configure(api_key=api_key)
        model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
        model = genai.GenerativeModel(model_name=model_name)

        content_parts = [prompt]
        for doc in documents:
            # Gemini image understanding uses PIL image objects.
            if doc.file.name.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
                with Image.open(doc.file.path) as img:
                    content_parts.append(img.copy())

        response = model.generate_content(content_parts)
        text = getattr(response, "text", "") or ""
        return text.strip() or _fallback_summary(patient, documents)
    except Exception:
        return _fallback_summary(patient, documents)


def _extract_json_block(text: str):
    if not text:
        return None
    match = re.search(r"\{[\s\S]*\}", text)
    if not match:
        return None
    try:
        return json.loads(match.group(0))
    except Exception:
        return None


def _fallback_structured_data(patient):
    allergies = [x.strip() for x in (patient.allergies or "").split(",") if x.strip()]
    intolerances = [x.strip() for x in (patient.intolerances or "").split(",") if x.strip()]
    return {
        "diseases": list(patient.history_diseases or []),
        "medications": [],
        "allergies": allergies,
        "intolerances": intolerances,
    }


def extract_structured_clinical_data(patient, documents: Iterable):
    documents = list(documents)
    api_key = os.getenv("GEMINI_API_KEY", "").strip()
    if genai is None or not api_key or api_key == PLACEHOLDER_KEY:
        return _fallback_structured_data(patient)

    prompt = (
        "Extract structured clinical context from provided patient details and document images. "
        "Return valid JSON only with keys: diseases (array of strings), "
        "medications (array of objects with keys name,dosage,instructions), "
        "allergies (array of strings), intolerances (array of strings).\n\n"
        f"Patient name: {patient.name}\n"
        f"Existing disease history: {', '.join(patient.history_diseases)}\n"
        f"Known allergies text: {patient.allergies}\n"
        f"Known intolerances text: {patient.intolerances}\n"
        f"Current scenario: {patient.current_scenario_description}\n"
    )

    try:
        genai.configure(api_key=api_key)
        model_name = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
        model = genai.GenerativeModel(model_name=model_name)

        content_parts = [prompt]
        for doc in documents:
            if doc.file.name.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
                with Image.open(doc.file.path) as img:
                    content_parts.append(img.copy())

        response = model.generate_content(content_parts)
        text = getattr(response, "text", "") or ""
        data = _extract_json_block(text)
        if not data:
            return _fallback_structured_data(patient)

        return {
            "diseases": data.get("diseases") or [],
            "medications": data.get("medications") or [],
            "allergies": data.get("allergies") or [],
            "intolerances": data.get("intolerances") or [],
        }
    except Exception:
        return _fallback_structured_data(patient)
