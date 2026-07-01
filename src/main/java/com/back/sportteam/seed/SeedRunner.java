package com.back.sportteam.seed;

import com.back.sportteam.seed.marker.SeedRun;
import com.back.sportteam.seed.marker.SeedRunRepository;
import com.back.sportteam.seed.support.SeedConstants;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 시더 진입점. seed-fixture / seed-bulk 프로파일이 켜졌을 때만 해당 SeedTask 빈이
 * 등록되며, 어떤 프로파일도 켜지지 않으면 tasks가 비어 아무 일도 하지 않는다.
 *
 * 각 SeedTask는 seed_run 마커로 멱등성을 보장한다. 이미 실행된 시더는 즉시 skip.
 *
 * 실행 예: SPRING_PROFILES_ACTIVE=prod,seed-fixture,seed-bulk
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedRunner implements ApplicationRunner {

    private final List<SeedTask> tasks;
    private final SeedRunRepository seedRunRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (tasks.isEmpty()) {
            return;
        }

        for (SeedTask task : tasks) {
            String type = task.markerType();
            if (seedRunRepository.existsBySeedTypeAndSeedVersion(type, SeedConstants.VERSION)) {
                log.info("[Seed] {} v{} 이미 실행됨 - skip", type, SeedConstants.VERSION);
                continue;
            }

            log.info("[Seed] {} v{} 시작", type, SeedConstants.VERSION);
            task.seed();
            seedRunRepository.save(SeedRun.of(type, SeedConstants.VERSION));
            log.info("[Seed] {} v{} 완료", type, SeedConstants.VERSION);
        }
    }
}
