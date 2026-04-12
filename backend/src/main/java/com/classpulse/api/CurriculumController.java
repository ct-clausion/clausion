package com.classpulse.api;

import com.classpulse.ai.CurriculumAnalyzer;
import com.classpulse.domain.course.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/courses/{courseId}")
@RequiredArgsConstructor
public class CurriculumController {

    private final CourseRepository courseRepository;
    private final CurriculumSkillRepository skillRepository;
    private final AsyncJobRepository asyncJobRepository;
    private final CurriculumAsyncService curriculumAsyncService;

    // --- DTOs ---

    public record SkillResponse(
            Long id, String name, String description, String difficulty,
            List<Long> prerequisiteIds
    ) {
        public static SkillResponse from(CurriculumSkill s) {
            List<Long> prereqs = s.getPrerequisites().stream()
                    .map(CurriculumSkill::getId).toList();
            return new SkillResponse(s.getId(), s.getName(), s.getDescription(), s.getDifficulty(), prereqs);
        }
    }

    public record UpdateSkillRequest(String name, String description, String difficulty) {}

    public record CreateSkillRequest(String name, String description, String difficulty) {}

    public record JobIdResponse(Long jobId) {}

    // --- Endpoints ---

    @PostMapping("/curriculum")
    public ResponseEntity<JobIdResponse> uploadCurriculum(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "target", required = false, defaultValue = "") String target,
            @RequestParam(value = "additionalPrompt", required = false, defaultValue = "") String additionalPrompt
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }

        AsyncJob job = AsyncJob.builder()
                .jobType("CURRICULUM_ANALYSIS")
                .status("PENDING")
                .inputPayload(Map.of(
                        "courseId", courseId,
                        "fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                        "contentLength", content.length()
                ))
                .build();
        job = asyncJobRepository.save(job);

        // target과 additionalPrompt를 objectives로 합쳐서 전달
        String objectives = "";
        if (!target.isBlank()) objectives += "대상: " + target + "\n";
        if (!additionalPrompt.isBlank()) objectives += "추가 요청: " + additionalPrompt + "\n";

        curriculumAsyncService.analyzeCurriculum(job.getId(), courseId, content, objectives);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new JobIdResponse(job.getId()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/skills")
    public ResponseEntity<List<SkillResponse>> getSkills(@PathVariable Long courseId) {
        List<CurriculumSkill> skills = skillRepository.findByCourseId(courseId);
        return ResponseEntity.ok(skills.stream().map(SkillResponse::from).toList());
    }

    @PutMapping("/skills/{skillId}")
    public ResponseEntity<SkillResponse> updateSkill(
            @PathVariable Long courseId,
            @PathVariable Long skillId,
            @RequestBody UpdateSkillRequest request
    ) {
        CurriculumSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));

        if (!skill.getCourse().getId().equals(courseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (request.name() != null) skill.setName(request.name());
        if (request.description() != null) skill.setDescription(request.description());
        if (request.difficulty() != null) skill.setDifficulty(request.difficulty());

        skill = skillRepository.save(skill);
        return ResponseEntity.ok(SkillResponse.from(skill));
    }

    @PostMapping("/skills")
    public ResponseEntity<SkillResponse> createSkill(
            @PathVariable Long courseId,
            @RequestBody CreateSkillRequest request
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        CurriculumSkill skill = CurriculumSkill.builder()
                .course(course)
                .name(request.name())
                .description(request.description())
                .difficulty(request.difficulty() != null ? request.difficulty() : "MEDIUM")
                .build();
        skill = skillRepository.save(skill);
        return ResponseEntity.status(HttpStatus.CREATED).body(SkillResponse.from(skill));
    }

    @PostMapping("/skills/defaults")
    @Transactional
    public ResponseEntity<List<SkillResponse>> createDefaultSkills(@PathVariable Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // 이미 스킬이 있으면 중복 생성 방지
        if (!skillRepository.findByCourseId(courseId).isEmpty()) {
            return ResponseEntity.ok(skillRepository.findByCourseId(courseId).stream()
                    .map(SkillResponse::from).toList());
        }

        var defaults = List.of(
                new String[]{"Python", "변수, 자료형, 리스트/딕셔너리, 함수, 클래스, 파일 I/O, 예외 처리", "EASY"},
                new String[]{"Java", "OOP, 제네릭, 컬렉션 프레임워크, 스트림 API, 예외 처리, 멀티스레딩", "MEDIUM"},
                new String[]{"JavaScript", "ES6+, 클로저, 프로토타입, Promise/async-await, DOM 조작", "MEDIUM"},
                new String[]{"TypeScript", "타입 시스템, 인터페이스, 제네릭, 유틸리티 타입, 타입 가드", "MEDIUM"},
                new String[]{"C", "포인터, 메모리 관리, 구조체, 파일 I/O, 전처리기", "HARD"},
                new String[]{"C++", "OOP, STL, 스마트 포인터, 템플릿, RAII, 이동 시맨틱", "HARD"},
                new String[]{"C#", ".NET 프레임워크, LINQ, async/await, 델리게이트, 제네릭", "MEDIUM"},
                new String[]{"Go", "고루틴, 채널, 인터페이스, 에러 처리, 패키지 시스템", "MEDIUM"},
                new String[]{"Rust", "소유권, 빌림, 라이프타임, 트레이트, 패턴 매칭, Result/Option", "HARD"},
                new String[]{"Kotlin", "null 안전성, 코루틴, 확장 함수, 데이터 클래스, 시퀀스", "MEDIUM"},
                new String[]{"Swift", "옵셔널, 프로토콜, 클로저, 구조체 vs 클래스, 에러 처리", "MEDIUM"},
                new String[]{"SQL", "SELECT/JOIN, 서브쿼리, 인덱스, 트랜잭션, 정규화, 집계 함수", "EASY"},
                new String[]{"HTML/CSS", "시맨틱 마크업, Flexbox, Grid, 반응형 디자인, 접근성", "EASY"},
                new String[]{"React", "컴포넌트, Hooks, 상태 관리, JSX, 라이프사이클, Context API", "MEDIUM"},
                new String[]{"Spring Boot", "DI/IoC, REST API, JPA, Security, AOP, 테스트", "HARD"},
                new String[]{"Node.js", "Express, 미들웨어, 비동기 I/O, npm, REST API 설계", "MEDIUM"},
                new String[]{"알고리즘", "정렬, 탐색, DP, 그래프, 그리디, 분할 정복, 시간 복잡도", "HARD"},
                new String[]{"자료구조", "배열, 연결 리스트, 스택, 큐, 트리, 해시맵, 그래프", "HARD"},
                new String[]{"Git", "브랜치, 머지, 리베이스, 충돌 해결, PR, 커밋 전략", "EASY"},
                new String[]{"Docker", "컨테이너, 이미지, Dockerfile, docker-compose, 볼륨, 네트워크", "MEDIUM"}
        );

        List<CurriculumSkill> created = new java.util.ArrayList<>();
        for (String[] d : defaults) {
            CurriculumSkill skill = CurriculumSkill.builder()
                    .course(course)
                    .name(d[0])
                    .description(d[1])
                    .difficulty(d[2])
                    .build();
            created.add(skillRepository.save(skill));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(created.stream().map(SkillResponse::from).toList());
    }

    @DeleteMapping("/skills/{skillId}")
    public ResponseEntity<Void> deleteSkill(
            @PathVariable Long courseId,
            @PathVariable Long skillId
    ) {
        CurriculumSkill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId));

        if (!skill.getCourse().getId().equals(courseId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        skillRepository.delete(skill);
        return ResponseEntity.noContent().build();
    }

    // --- Async Service (inner class) ---

    @Slf4j
    @Service
    @RequiredArgsConstructor
    static class CurriculumAsyncService {

        private final AsyncJobRepository asyncJobRepository;
        private final CurriculumSkillRepository skillRepository;
        private final CourseRepository courseRepository;
        private final CurriculumAnalyzer curriculumAnalyzer;

        @Async("aiTaskExecutor")
        public void analyzeCurriculum(Long jobId, Long courseId, String content, String objectives) {
            AsyncJob job = asyncJobRepository.findById(jobId).orElseThrow();
            try {
                job.setStatus("PROCESSING");
                asyncJobRepository.save(job);

                log.info("Analyzing curriculum for course {} via CurriculumAnalyzer (content length: {})",
                        courseId, content.length());

                Map<String, Object> analysisResult = curriculumAnalyzer.analyze(courseId, content, objectives);

                // 전체 AI 분석 결과를 job resultPayload에 저장
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("courseId", courseId);
                result.put("message", "Curriculum analysis completed");
                result.put("skillsExtracted", analysisResult.getOrDefault("skillCount", 0));
                result.put("weekly_concepts", analysisResult.getOrDefault("weekly_concepts", List.of()));
                result.put("common_misconceptions", analysisResult.getOrDefault("common_misconceptions", List.of()));
                result.put("review_points", analysisResult.getOrDefault("review_points", List.of()));

                job.complete(result);
                asyncJobRepository.save(job);

            } catch (Exception e) {
                log.error("Curriculum analysis failed for job {}", jobId, e);
                job.fail(e.getMessage());
                asyncJobRepository.save(job);
            }
        }
    }
}
