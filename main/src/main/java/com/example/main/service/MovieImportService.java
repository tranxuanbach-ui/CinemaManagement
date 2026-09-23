package com.example.main.service;

import com.example.main.dto.MovieImportDto;
import com.example.main.dto.ScheduleImportDto;
import com.example.main.dto.ScreeningVersionImportDto;
import com.example.main.dto.ShowtimeImportDto;
import com.example.main.entity.cinema.Cinema;
import com.example.main.entity.cinema.Schedule;
import com.example.main.entity.cinema.ScreeningVersion;
import com.example.main.entity.cinema.Showtime;
import com.example.main.entity.movie.Genre;
import com.example.main.entity.movie.Movie;
import com.example.main.repository.CinemaRepository;
import com.example.main.repository.GenreRepository;
import com.example.main.repository.MovieRepository;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MovieImportService {
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final CinemaRepository cinemaRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public MovieImportService(MovieRepository movieRepository, GenreRepository genreRepository, CinemaRepository cinemaRepository, ObjectMapper objectMapper) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.cinemaRepository = cinemaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void importAllMovies(){
        try{
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            Resource[] resources = resolver.getResources("classpath:Data/*.json");
            for (Resource resource : resources) {
                importFile(resource);
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot import movies data :(", e);
        }
    }
    private void importFile(Resource resource){
        try (InputStream inputStream = resource.getInputStream()) {
            List<MovieImportDto> movies =
                    objectMapper.readValue(
                            inputStream,
                            new TypeReference<List<MovieImportDto>>() {}
                    );
            for (MovieImportDto movieDto : movies) {
                importMovie(movieDto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error when reading file: " + resource.getFilename() + ":<", e);
        }
    }
    private void importMovie(MovieImportDto dto){
        Movie movie = movieRepository.findByDetailLink(dto.getDetailLink()).orElseGet(Movie::new);
        movie.setTitle(dto.getTitle());
        LocalDate releaseDate = parseDate(dto.getReleaseDate());
        movie.setReleaseDate(releaseDate);
        movie.setDescription(dto.getDescription());
        movie.setPosterUrl(dto.getPosterUrl());
        movie.setTrailer(dto.getTrailer());
        movie.setDetailLink(dto.getDetailLink());
        movie.setRating(dto.getRating());
        movie.setRuntimeMinutes(dto.getRuntimeMinutes());

        movie.setGenres(importGenres(dto.getGenres()));
        movie.getShowtimes().clear();

        if (dto.getShowtimes() != null) {
            for (ShowtimeImportDto showtimeDto : dto.getShowtimes()) {
                Showtime showtime = createShowtime(movie, showtimeDto, releaseDate);
                movie.getShowtimes().add(showtime);
            }
        }

        movieRepository.save(movie);
    }
    private List<Genre> importGenres(List<String> genreNames){
        List<Genre> genres = new ArrayList<>();
        if (genreNames == null) return  genres;

        for (String genreName : genreNames){
            if (genreName == null) continue;

            genreName = genreName.trim();
            String finalGenreName = genreName;

            Genre genre = genreRepository
                    .findByName(finalGenreName)
                    .orElseGet(() -> {
                        Genre newGenre = new Genre();
                        newGenre.setName(finalGenreName);
                        return genreRepository.save(newGenre);
                    });
            genres.add(genre);
        }
        return genres;
    }
    private Showtime createShowtime(Movie movie, ShowtimeImportDto dto, LocalDate releaseDate) {
        Cinema cinema = cinemaRepository
                .findByName(dto.getCinemaName()).orElseGet(() -> {
                    Cinema newCinema = new Cinema();

                    newCinema.setName(dto.getCinemaName());

                    return cinemaRepository.save(newCinema);
                });

        Showtime showtime = new Showtime();

        showtime.setMovie(movie);
        showtime.setCinema(cinema);

        if (dto.getSchedule() != null) {
            for (ScheduleImportDto scheduleDto : dto.getSchedule()) {
                Schedule schedule = createSchedule(showtime, scheduleDto, releaseDate);

                showtime.getSchedules().add(schedule);
            }
        }

        return showtime;
    }

    private Schedule createSchedule(
            Showtime showtime,
            ScheduleImportDto dto,
            LocalDate releaseDate
    ) {

        Schedule schedule = new Schedule();

        schedule.setShowtime(showtime);
        schedule.setDate(parseScheduleDate(dto.getDate(), releaseDate));

        if (dto.getVersions() != null) {
            for (ScreeningVersionImportDto versionDto : dto.getVersions()) {
                ScreeningVersion version = createScreeningVersion(schedule, versionDto);

                schedule.getVersions().add(version);
            }
        }

        return schedule;
    }

    private ScreeningVersion createScreeningVersion(Schedule schedule, ScreeningVersionImportDto dto) {
        ScreeningVersion version = new ScreeningVersion();

        version.setSchedule(schedule);
        version.setVersion(dto.getVersion());

        List<LocalTime> times = new ArrayList<>();

        if (dto.getTimes() != null) {
            for (String time : dto.getTimes()) {
                if (time == null || time.isBlank()) continue;

                times.add(parseShowTime(time));
            }
        }
        version.setTimes(times);
        return version;
    }

    private LocalDate parseScheduleDate(String date, LocalDate releaseDate) {
        LocalDate parsed = parseDate(date);
        if (parsed != null) {
            return parsed;
        }
        if (releaseDate == null) {
            throw new IllegalArgumentException("Cannot locate release date: " + date);
        }
        return releaseDate;
    }

    private LocalTime parseShowTime(String time) {
        String[] parts = time.trim().split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return LocalTime.of(hour % 24, minute);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        String normalized = date.trim().replace('/', '-');
        return LocalDate.parse(normalized, DATE_FORMAT);
    }
}
