package com.example.main.entity.cinema;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "screening_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScreeningVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ElementCollection
    @CollectionTable(
            name = "screening_times",
            joinColumns = @JoinColumn(name = "screening_version_id")
    )
    @Column(name = "show_time")
    private List<LocalTime> times = new ArrayList<>();
}