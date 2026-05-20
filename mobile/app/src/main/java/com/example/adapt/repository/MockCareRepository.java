package com.example.adapt.repository;

import com.example.adapt.model.CareTask;
import com.example.adapt.model.Patient;
import com.example.adapt.model.RoutineGroup;
import com.example.adapt.model.RoutineTask;
import com.example.adapt.model.ScheduleItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockCareRepository {

    public List<Patient> getPatients() {
        return Arrays.asList(
                new Patient(1, "Asha Verma", 68, "Female", "Penicillin allergy, Diabetes care",
                        "Focus on sugar control, BP monitoring, hydration, and safe activity pacing."),
                new Patient(2, "Sarah Jenkins", 78, "Female", "Hypertension, Type 2 Diabetes",
                        "Sarah is stable with excellent medication adherence and improving activity tolerance."),
                new Patient(3, "Robert Miller", 82, "Male", "Mild cognitive impairment, Osteoarthritis",
                        "Robert benefits from routine reinforcement and gentle mobility coaching."),
                new Patient(4, "Elena Rodriguez", 65, "Female", "Asthma",
                        "Awaiting enough history for a reliable summary.")
        );
    }

    public List<CareTask> getTasks() {
        return new ArrayList<>(Arrays.asList(
                new CareTask(1, "Insulin Dosage", "Administer 10 units before breakfast", "HIGH", "Pending", "8:00 AM"),
                new CareTask(2, "Vitals Check", "BP and SpO2 recording", "MED", "Complete", "9:30 AM"),
                new CareTask(3, "Physiotherapy", "30 minute mobility session", "LOW", "Scheduled", "10:30 AM"),
                new CareTask(4, "Wellness Check", "Hydration, mood, and appetite review", "MED", "Pending", "1:00 PM")
        ));
    }

    public List<RoutineGroup> getRoutineGroups() {
        return Arrays.asList(
                new RoutineGroup("Morning Wellness", "8 tasks assigned to Sarah", Arrays.asList(
                        new RoutineTask("Initial vitals check", "Blood pressure and oxygen saturation before breakfast.", true),
                        new RoutineTask("Medication confirmation", "Confirm insulin and morning tablets are complete.", true),
                        new RoutineTask("Hydration reminder", "Offer water and record intake.", false)
                ), true),
                new RoutineGroup("Post-Op Recovery", "12 tasks assigned to Robert", Arrays.asList(
                        new RoutineTask("Pain score", "Record pain score and mobility response.", true),
                        new RoutineTask("Assisted walk", "Short hallway walk with support.", false),
                        new RoutineTask("Wound observation", "Check dressing condition and note changes.", true)
                ), false),
                new RoutineGroup("Evening Cognitive Care", "5 tasks assigned to Elena", Arrays.asList(
                        new RoutineTask("Orientation prompt", "Review day, place, and care team names.", true),
                        new RoutineTask("Breathing review", "Log asthma triggers and response.", true)
                ), false)
        );
    }

    public List<ScheduleItem> getScheduleItems() {
        return Arrays.asList(
                new ScheduleItem("8:00 AM", "Morning Medication", "Sarah Jenkins", "Medication", false),
                new ScheduleItem("10:30 AM", "Physical Therapy", "Robert Miller", "Activity", false),
                new ScheduleItem("1:00 PM", "Wellness Check", "Asha Verma", "Attention", true),
                new ScheduleItem("3:30 PM", "Cognitive Exercises", "Robert Miller", "Cognitive", false)
        );
    }

    public int[] getCompletionTrend() {
        return new int[]{68, 74, 79, 83, 87, 82, 90};
    }
}
