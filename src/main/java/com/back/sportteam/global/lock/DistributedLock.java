package com.back.sportteam.global.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    // SpEL 표현식으로 락 키를 만든다. 예: "'match:join:' + #matchId"
    String key();

    // 같은 락을 잡은 요청이 끝나길 기다리는 최대 시간
    long waitTime() default 3L;

    // 락을 잡은 요청이 비정상 종료되어도 자동 해제되도록 하는 시간
    long leaseTime() default 5L;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
