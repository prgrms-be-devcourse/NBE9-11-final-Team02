package com.back.sportteam.domain.notification.repository;

import com.back.sportteam.domain.notification.entity.Notification;
import com.back.sportteam.domain.notification.entity.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    boolean existsByUserIdAndTypeAndReferenceId(String userId, NotificationType type, String referenceId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);
}
