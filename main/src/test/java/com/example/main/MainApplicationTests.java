package com.example.main;

import com.example.main.dto.MovieResponse;
import com.example.main.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class MainApplicationTests {

	@Autowired
	private RecommendationService recommendationService;

	@Test
	void contextLoads() {
	}

	@Test
	void testRecommendationColdStart() {
		List<MovieResponse> recommendations = recommendationService.getRecommendations(16);
		assertNotNull(recommendations);
		System.out.println("Recommendations count: " + recommendations.size());
	}
}
