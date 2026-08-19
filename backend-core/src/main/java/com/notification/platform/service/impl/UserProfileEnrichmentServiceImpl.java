package com.notification.platform.service.impl;

import com.notification.platform.domain.model.UserProfile;
import com.notification.platform.service.UserProfileEnrichmentService;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserProfileEnrichmentServiceImpl implements UserProfileEnrichmentService {

    private final Map<String, UserProfile> userProfileCache = new ConcurrentHashMap<>();

    public UserProfileEnrichmentServiceImpl() {
      
        userProfileCache.put("12345", UserProfile.builder()
                .userId("12345")
                .name("Maria Santos")
                .email("maria.santos@example.ph")
                .phoneNumber("+639171234567")
                .country("PHILIPPINES")
                .timezone(ZoneId.of("Asia/Manila"))
                .tier("VIP")
                .quietHoursOptIn(true)
                .build());

        userProfileCache.put("user_us_01", UserProfile.builder()
                .userId("user_us_01")
                .name("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("+12025550143")
                .country("UNITED_STATES")
                .timezone(ZoneId.of("America/New_York"))
                .tier("STANDARD")
                .quietHoursOptIn(false)
                .build());

        userProfileCache.put("user_ph_02", UserProfile.builder()
                .userId("user_ph_02")
                .name("Jose Rizal")
                .email("jose.rizal@example.ph")
                .phoneNumber("+639189876543")
                .country("PHILIPPINES")
                .timezone(ZoneId.of("Asia/Manila"))
                .tier("STANDARD")
                .quietHoursOptIn(true)
                .build());
    }

    @Override
    public UserProfile enrich(String userId, String overrideCountry) {
        UserProfile cached = userProfileCache.get(userId);
        if (cached != null) {
            if (overrideCountry != null && !overrideCountry.isBlank()) {
                return UserProfile.builder()
                        .userId(cached.getUserId())
                        .name(cached.getName())
                        .email(cached.getEmail())
                        .phoneNumber(cached.getPhoneNumber())
                        .country(overrideCountry)
                        .timezone(resolveTimezoneForCountry(overrideCountry))
                        .tier(cached.getTier())
                        .quietHoursOptIn(cached.isQuietHoursOptIn())
                        .build();
            }
            return cached;
        }

       
        String effectiveCountry = (overrideCountry != null && !overrideCountry.isBlank()) ? overrideCountry : "PHILIPPINES";
        return UserProfile.builder()
                .userId(userId)
                .name("User " + userId)
                .email(userId + "@notification.local")
                .phoneNumber("+63900" + Math.abs(userId.hashCode() % 10000000))
                .country(effectiveCountry)
                .timezone(resolveTimezoneForCountry(effectiveCountry))
                .tier("STANDARD")
                .quietHoursOptIn(true)
                .build();
    }

    @Override
    public void registerProfile(UserProfile profile) {
        userProfileCache.put(profile.getUserId(), profile);
    }

    private ZoneId resolveTimezoneForCountry(String country) {
        if (country == null) return ZoneId.of("UTC");
        return switch (country.toUpperCase()) {
            case "PHILIPPINES", "PH" -> ZoneId.of("Asia/Manila");
            case "UNITED_STATES", "USA", "US" -> ZoneId.of("America/New_York");
            case "SINGAPORE", "SG" -> ZoneId.of("Asia/Singapore");
            case "JAPAN", "JP" -> ZoneId.of("Asia/Tokyo");
            case "UNITED_KINGDOM", "UK", "GB" -> ZoneId.of("Europe/London");
            default -> ZoneId.of("UTC");
        };
    }
}
