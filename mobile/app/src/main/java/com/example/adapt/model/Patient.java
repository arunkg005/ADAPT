package com.example.adapt.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Patient {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("age")
    private int age;

    @SerializedName("gender")
    private String gender;

    @SerializedName("allergies")
    private String allergies;

    @SerializedName("intolerances")
    private String intolerances;

    @SerializedName("current_scenario_description")
    private String currentScenarioDescription;

    @SerializedName("doctor_guidelines")
    private String doctorGuidelines;

    @SerializedName("ai_summary")
    private String aiSummary;

    // Full constructor for creating new patients
    public Patient(String name, int age, String gender, String allergies,
                   String currentScenarioDescription) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.allergies = allergies;
        this.currentScenarioDescription = currentScenarioDescription;
    }

    // Read constructor (from API)
    public Patient(int id, String name, int age, String gender, String allergies, String aiSummary) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.allergies = allergies;
        this.aiSummary = aiSummary;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getGender() { return gender; }
    public String getAllergies() { return allergies != null ? allergies : ""; }
    public String getIntolerances() { return intolerances != null ? intolerances : ""; }
    public String getCurrentScenarioDescription() { return currentScenarioDescription != null ? currentScenarioDescription : ""; }
    public String getDoctorGuidelines() { return doctorGuidelines != null ? doctorGuidelines : ""; }
    public String getAiSummary() { return aiSummary != null ? aiSummary : ""; }

    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setGender(String gender) { this.gender = gender; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public void setCurrentScenarioDescription(String desc) { this.currentScenarioDescription = desc; }
    public void setDoctorGuidelines(String guidelines) { this.doctorGuidelines = guidelines; }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.split(" ");
        if (parts.length > 1) return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
