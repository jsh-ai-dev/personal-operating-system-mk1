package com.jsh.pos.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 시간 관련 설정입니다.
 * 
 * Clock을 주입하는 이유:
 * - Instant.now()를 코드 곳곳에 직접 쓰면 테스트할 때 시간을 고정할 수 없음
 * - 예: "2026-04-02 12:00:00일 때 동작 확인"이 불가능
 * - Clock을 DI하면 테스트에서 시간을 임의로 설정 가능
 * 
 * 프로덕션에서:
 * - Clock.systemUTC(): 실제 시간 사용
 * 
 * 테스트에서:
 * - Clock.fixed(...): 특정 시간 고정 가능
 * - 예: Clock.fixed(Instant.parse("2026-04-02T00:00:00Z"), ZoneOffset.UTC)
 */
@Configuration
class ClockConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()  // 시스템의 실제 시간 (UTC)
}


