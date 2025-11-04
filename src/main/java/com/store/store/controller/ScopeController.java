package com.store.store.controller;

import com.store.store.scopes.ApplicationScopedBean;
import com.store.store.scopes.RequestScopedBean;
import com.store.store.scopes.SessionScopedBean;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A REST controller that demonstrates the usage of beans with different scopes
 * such as request scope, session scope, and application scope. The controller
 * exposes endpoints to test the behavior and lifecycle of these scoped beans.
 *
 * @author Kardigué
 *  * @version 3.0
 *  * @since 2025-10-01
 */
@RestController
@RequestMapping("api/v1/scope")
@RequiredArgsConstructor
public class ScopeController {

    private final RequestScopedBean requestScopedBean;
    private final SessionScopedBean sessionScopedBean;
    private final ApplicationScopedBean applicationScopedBean;

    /**
     * Handles a GET request to the "/request" endpoint, demonstrating the usage of a request-scoped bean.
     * Sets the user name in the request-scoped bean and retrieves it to verify the bean's behavior within a single request scope.
     *
     * @return a {@link ResponseEntity} containing the user name from the request-scoped bean.
     */
    @GetMapping("/request")
    public ResponseEntity<String> testResquestScope() {
        requestScopedBean.setUserName("John Doe");
        return ResponseEntity.ok().body(requestScopedBean.getUserName());
    }

    /**
     * Handles a GET request to the "/session" endpoint, demonstrating the usage of a session-scoped bean.
     * Sets the user name in the session-scoped bean and retrieves it to verify the bean's behavior within
     * a session scope.
     *
     * @return a {@link ResponseEntity} containing the user name from the session-scoped bean.
     */
    @GetMapping("/session")
    public ResponseEntity<String> testSessionScope() {
        sessionScopedBean.setUserName("John Doe");
        return ResponseEntity.ok().body(sessionScopedBean.getUserName());
    }

    /**
     * Handles a GET request to the "/application" endpoint, demonstrating the usage of an application-scoped bean.
     * Increments the visitor count in the application scope and retrieves the updated count to verify the bean's
     * persistent behavior across the entire application lifecycle.
     *
     * @return a {@link ResponseEntity} containing the updated visitor count from the application-scoped bean.
     */
    @GetMapping("/application")
    public ResponseEntity<Integer> testApplicationScope() {
        applicationScopedBean.incrementVisitorCount();
        return ResponseEntity.ok().body(applicationScopedBean.getVisitorCount());
    }

    /**
     * Handles a GET request to the "/test" endpoint, retrieving the current visitor count
     * from an application-scoped bean. This demonstrates the behavior of a bean with
     * application-level scope, where its state is shared and persisted across the entire
     * application's lifecycle.
     *
     * @return a {@link ResponseEntity} containing the current visitor count managed by the application-scoped bean.
     */
    @GetMapping("/test")
    public ResponseEntity<Integer> testScope() {
        return ResponseEntity.ok().body(applicationScopedBean.getVisitorCount());
    }
}