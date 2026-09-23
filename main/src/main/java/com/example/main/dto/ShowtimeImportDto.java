package com.example.main.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ShowtimeImportDto {
    @JsonProperty("cinema_name")
    private String cinemaName;
    private List<ScheduleImportDto> schedule;

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public List<ScheduleImportDto> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<ScheduleImportDto> schedule) {
        this.schedule = schedule;
    }
}