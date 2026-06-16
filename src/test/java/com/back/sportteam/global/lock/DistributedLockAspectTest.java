package com.back.sportteam.global.lock;

import com.back.sportteam.global.exception.BusinessException;
import com.back.sportteam.global.exception.errorcode.CommonErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DistributedLockAspectTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final DistributedLockAspect aspect = new DistributedLockAspect(redissonClient);

    @Test
    void 락을_획득하면_대상_메서드를_실행하고_락을_해제한다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("match-id");
        DistributedLock distributedLock = distributedLock();
        when(redissonClient.getLock("lock:match:join:match-id")).thenReturn(lock);
        when(lock.tryLock(3L, 5L, TimeUnit.SECONDS)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("success");

        Object result = aspect.applyLock(joinPoint, distributedLock);

        assertThat(result).isEqualTo("success");
        verify(joinPoint).proceed();
        verify(lock).unlock();
    }

    @Test
    void 락을_획득하지_못하면_비즈니스_예외를_던지고_대상_메서드를_실행하지_않는다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("match-id");
        DistributedLock distributedLock = distributedLock();
        when(redissonClient.getLock("lock:match:join:match-id")).thenReturn(lock);
        when(lock.tryLock(3L, 5L, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> aspect.applyLock(joinPoint, distributedLock))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.LOCK_ACQUISITION_FAILED);

        verify(joinPoint, never()).proceed();
        verify(lock, never()).unlock();
    }

    @Test
    void 락_대기_중_인터럽트되면_인터럽트_상태를_복구하고_비즈니스_예외를_던진다() throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint("match-id");
        DistributedLock distributedLock = distributedLock();
        when(redissonClient.getLock("lock:match:join:match-id")).thenReturn(lock);
        when(lock.tryLock(3L, 5L, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        assertThatThrownBy(() -> aspect.applyLock(joinPoint, distributedLock))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.LOCK_ACQUISITION_FAILED);

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
        verify(joinPoint, never()).proceed();
        verify(lock, never()).unlock();
    }

    private ProceedingJoinPoint joinPoint(String matchId) throws NoSuchMethodException {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        LockTarget target = new LockTarget();
        Method method = LockTarget.class.getMethod("join", String.class);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(new Object[]{matchId});
        when(signature.getMethod()).thenReturn(method);
        return joinPoint;
    }

    private DistributedLock distributedLock() throws NoSuchMethodException {
        return LockTarget.class.getMethod("join", String.class).getAnnotation(DistributedLock.class);
    }

    private static class LockTarget {

        @DistributedLock(key = "'match:join:' + #matchId", waitTime = 3L, leaseTime = 5L)
        public void join(String matchId) {
        }
    }
}
