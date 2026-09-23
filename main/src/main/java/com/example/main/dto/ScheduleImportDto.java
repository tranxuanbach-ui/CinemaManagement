package com.example.main.dto;

import java.util.List;

public class ScheduleImportDto {
    private String date;
    private List<ScreeningVersionImportDto> versions;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public List<ScreeningVersionImportDto> getVersions() {
        return versions;
    }

    public void setVersions(List<ScreeningVersionImportDto> versions) {
        this.versions = versions;
    }
}