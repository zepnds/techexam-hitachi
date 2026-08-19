package com.notification.platform.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
    private String userId;
    private String name;
    private String email;
    private String phoneNumber;
    private String country;
    private ZoneId timezone;
    private String tier; 
    private boolean quietHoursOptIn;
}
