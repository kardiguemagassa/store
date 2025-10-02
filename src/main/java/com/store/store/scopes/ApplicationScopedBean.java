package com.store.store.scopes;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

@Component
@ApplicationScope
@Getter
@Slf4j
public class ApplicationScopedBean {

    private int visitorCount;

    public void incrementVisitorCount() {
        visitorCount++;
    }

    public ApplicationScopedBean() {
        log.info("ApplicationScopedBean initialized");
    }
}