package com.store.store.constants;

public final class AuthMessageCodes {
    public static final String LOGIN_SUCCESS = "api.success.auth.login";
    public static final String REGISTER_SUCCESS = "api.success.auth.register";
    public static final String REFRESH_SUCCESS = "api.success.auth.refresh";
    public static final String LOGOUT_SUCCESS = "api.success.auth.logout";

    public static final String AUTH_REQUIRED = "api.error.auth.required";
    public static final String BAD_CREDENTIALS = "api.error.auth.bad.credentials";
    public static final String REFRESH_MISSING = "api.error.auth.refresh.missing";
    public static final String REFRESH_INVALID = "api.error.auth.refresh.invalid";
    public static final String REPLAY_ATTACK = "api.error.auth.replay.attack";
    public static final String RATE_LIMIT_EXCEEDED = "api.error.rate.limit.exceeded";

    private AuthMessageCodes() {}
}