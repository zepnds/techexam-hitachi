package com.notification.platform.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "userId")
public class UserProfileEntity {

    @Id
    @Column(name = "user_id", length = 128, nullable = false, updatable = false)
    private String userId;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 64)
    private String phoneNumber;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "tier", length = 32)
    @Builder.Default
    private String tier = "STANDARD";

    @Column(name = "quiet_hours_opt_in", nullable = false)
    @Builder.Default
    private boolean quietHoursOptIn = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
