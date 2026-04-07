package com.example.adapt.data.network.dto;

public class TelemetryIngestResponse {
    private String message;
    private BackendTelemetry telemetry;

    public String getMessage() {
        return message;
    }

    public BackendTelemetry getTelemetry() {
        return telemetry;
    }
}
