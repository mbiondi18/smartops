package com.smartops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.dto.TaskDTO;
import com.smartops.model.Task;
import com.smartops.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key:placeholder}")
    private String anthropicApiKey;

    @Value("${anthropic.api.enabled:false}")
    private boolean apiEnabled;

    @Value("${anthropic.api.model:claude-haiku-4-5-20251001}")
    private String model;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", anthropicApiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    /**
     * Analyse a single task using Claude API.
     * Returns a mock response if the API is disabled.
     */
    public TaskDTO.AnalysisResponse analyseTask(Long taskId) {
        Task task = taskService.findTaskOrThrow(taskId);

        if (apiEnabled) {
            return callClaudeApi(task);
        } else {
            log.info("AI API disabled — returning mock analysis for task {}", taskId);
            return TaskDTO.AnalysisResponse.builder()
                    .taskId(taskId)
                    .summary("Mock summary: " + task.getTitle())
                    .suggestedPriority(Task.Priority.MEDIUM)
                    .suggestedCategory(Task.Category.WORK)
                    .build();
        }
    }

    /**
     * Scheduled job: analyse all tasks that haven't been analysed yet.
     * Runs every hour.
     */
    @Scheduled(fixedRateString = "${ai.analysis.interval:3600000}")
    public void analyseUnprocessedTasks() {
        List<Task> unanalysed = taskRepository.findByAiAnalysedFalse();
        log.info("Found {} unanalysed tasks — processing...", unanalysed.size());

        for (Task task : unanalysed) {
            try {
                TaskDTO.AnalysisResponse analysis = analyseTask(task.getId());
                taskService.applyAiAnalysis(task.getId(), analysis);
            } catch (Exception e) {
                log.error("Failed to analyse task {}: {}", task.getId(), e.getMessage());
            }
        }
    }

    /**
     * Calls the real Claude API, parses the JSON response, and returns an AnalysisResponse.
     */
    private TaskDTO.AnalysisResponse callClaudeApi(Task task) {
        String prompt = String.format("""
                Analyse this task and respond ONLY with a JSON object in this exact format, no other text:
                {
                  "summary": "one sentence summary",
                  "priority": "LOW|MEDIUM|HIGH",
                  "category": "WORK|PERSONAL|URGENT|OTHER"
                }

                Task title: %s
                Task description: %s
                """, task.getTitle(), task.getDescription());

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 150,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        try {
            log.info("Calling Claude API for task {}", task.getId());

            Map<?, ?> response = webClient.post()
                    .uri("/v1/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String text = extractText(response);
            log.debug("Claude raw response for task {}: {}", task.getId(), text);

            return parseResponse(task.getId(), text);

        } catch (WebClientResponseException e) {
            log.error("Claude API returned error {} for task {}: {}", e.getStatusCode(), task.getId(), e.getResponseBodyAsString());
            throw new RuntimeException("Claude API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Claude API call failed for task {}: {}", task.getId(), e.getMessage());
            throw new RuntimeException("AI analysis failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the text content from Anthropic's response envelope.
     * Response shape: { "content": [{ "type": "text", "text": "..." }] }
     */
    private String extractText(Map<?, ?> response) {
        List<?> content = (List<?>) response.get("content");
        Map<?, ?> firstBlock = (Map<?, ?>) content.get(0);
        return (String) firstBlock.get("text");
    }

    /**
     * Parses Claude's JSON text into an AnalysisResponse.
     * Also handles the case where Claude wraps the JSON in markdown code fences.
     */
    private TaskDTO.AnalysisResponse parseResponse(Long taskId, String text) {
        try {
            String json = text.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("```(json)?", "").trim();
            }

            Map<?, ?> parsed = objectMapper.readValue(json, Map.class);

            String summary = (String) parsed.get("summary");
            Task.Priority priority = Task.Priority.valueOf(((String) parsed.get("priority")).toUpperCase());
            Task.Category category = Task.Category.valueOf(((String) parsed.get("category")).toUpperCase());

            return TaskDTO.AnalysisResponse.builder()
                    .taskId(taskId)
                    .summary(summary)
                    .suggestedPriority(priority)
                    .suggestedCategory(category)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse Claude response for task {}: {}. Raw: {}", taskId, e.getMessage(), text);
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}
