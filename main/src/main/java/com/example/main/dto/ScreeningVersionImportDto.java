package com.example.main.dto;

import java.util.List;

public class ScreeningVersionImportDto {
    private String version;
    private List<String> times;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times;
    }
}