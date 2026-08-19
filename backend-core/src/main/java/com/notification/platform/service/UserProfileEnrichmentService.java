package com.notification.platform.service;

import com.notification.platform.domain.model.UserProfile;

public interface UserProfileEnrichmentService {

  
    UserProfile enrich(String userId, String overrideCountry);

  
    void registerProfile(UserProfile profile);
}
