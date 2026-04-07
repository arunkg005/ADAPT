package com.example.adapt.data.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DeviceCreateRequest {

    @SerializedName("patient_id")
    private final String patientId;

    @SerializedName("device_name")
    private final String deviceName;

    @SerializedName("device_type")
    private final String deviceType;

    private final String os;

    private final List<String> capabilities;

    public DeviceCreateRequest(
            String patientId,
            String deviceName,
            String deviceType,
            String os,
            List<String> capabilities
    ) {
        this.patientId = patientId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.os = os;
        this.capabilities = capabilities;
    }
}
