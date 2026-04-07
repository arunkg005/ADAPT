package com.example.adapt.data.network.dto;

import java.util.List;

public class ApiListResponse<T> {
    private List<T> data;
    private int total;
    private int limit;
    private int offset;

    public List<T> getData() {
        return data;
    }

    public int getTotal() {
        return total;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }
}
