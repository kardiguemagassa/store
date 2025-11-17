package com.store.store.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author Kardigué
 * @version 4.0 - Production Ready
 * @since 2025-01-06
 */
@Slf4j
@Service
public class DeviceInfoExtractorServiceImpl {

    // CONSTANTES

    private static final String UNKNOWN_DEVICE = "Unknown Device";
    private static final String UNKNOWN_OS = "Unknown OS";
    private static final String UNKNOWN_BROWSER = "Unknown Browser";
    private static final String DEVICE_TYPE_MOBILE = "Mobile";
    private static final String DEVICE_TYPE_TABLET = "Tablet";
    private static final String DEVICE_TYPE_DESKTOP = "Desktop";

    // EXTRACTION COMPLÈTE

    public String extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            log.debug("Empty User-Agent provided, skipping device extraction");
            return null;
        }

        try {
            String os = extractOS(userAgent);
            String browser = extractBrowser(userAgent);
            String deviceType = extractDeviceType(userAgent);

            String deviceInfo = String.format("%s - %s - %s", os, browser, deviceType);
            log.debug("Device info extracted: {}", deviceInfo);

            return deviceInfo;

        } catch (Exception e) {
            log.warn("Failed to extract device info from User-Agent: {}", e.getMessage());
            return UNKNOWN_DEVICE;
        }
    }

    // EXTRACTION - SYSTÈME D'EXPLOITATION

    private String extractOS(String userAgent) {
        // Windows
        if (userAgent.contains("Windows NT 10.0")) return "Windows 10";
        if (userAgent.contains("Windows NT 6.3")) return "Windows 8.1";
        if (userAgent.contains("Windows NT 6.2")) return "Windows 8";
        if (userAgent.contains("Windows NT 6.1")) return "Windows 7";
        if (userAgent.contains("Windows")) return "Windows";

        // macOS
        if (userAgent.contains("Mac OS X")) {
            if (userAgent.contains("Mac OS X 10_15")) return "macOS Catalina";
            if (userAgent.contains("Mac OS X 11")) return "macOS Big Sur";
            if (userAgent.contains("Mac OS X 12")) return "macOS Monterey";
            if (userAgent.contains("Mac OS X 13")) return "macOS Ventura";
            if (userAgent.contains("Mac OS X 14")) return "macOS Sonoma";
            if (userAgent.contains("Mac OS X 15")) return "macOS Sequoia";
            return "macOS";
        }

        // Android (avec extraction de version)
        if (userAgent.contains("Android")) {
            try {
                String[] parts = userAgent.split("Android ");
                if (parts.length > 1) {
                    String version = parts[1].split(";")[0].trim();
                    return "Android " + version;
                }
            } catch (Exception e) {
                log.debug("Failed to extract Android version: {}", e.getMessage());
            }
            return "Android";
        }

        // iOS
        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            if (userAgent.contains("OS 17")) return "iOS 17";
            if (userAgent.contains("OS 16")) return "iOS 16";
            if (userAgent.contains("OS 15")) return "iOS 15";
            return "iOS";
        }

        // Linux
        if (userAgent.contains("Ubuntu")) return "Ubuntu";
        if (userAgent.contains("CrOS")) return "Chrome OS";
        if (userAgent.contains("Linux")) return "Linux";

        return UNKNOWN_OS;
    }

    // EXTRACTION - NAVIGATEUR

    private String extractBrowser(String userAgent) {
        // Ordre important : Edge avant Chrome
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }

        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg")) {
            return userAgent.contains("Mobile") ? "Chrome Mobile" : "Chrome";
        }

        if (userAgent.contains("Firefox/")) {
            return userAgent.contains("Mobile") ? "Firefox Mobile" : "Firefox";
        }

        // Safari APRÈS Chrome (Chrome contient "Safari")
        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            return userAgent.contains("Mobile") ? "Safari Mobile" : "Safari";
        }

        if (userAgent.contains("Opera/") || userAgent.contains("OPR/")) {
            return "Opera";
        }

        if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "Internet Explorer";
        }

        return UNKNOWN_BROWSER;
    }

    // EXTRACTION - TYPE D'APPAREIL

    private String extractDeviceType(String userAgent) {
        // Mobile
        if (userAgent.contains(DEVICE_TYPE_MOBILE) ||
                userAgent.contains("Android") ||
                userAgent.contains("iPhone")) {
            return DEVICE_TYPE_MOBILE;
        }

        // Tablet
        if (userAgent.contains("iPad") ||
                userAgent.contains(DEVICE_TYPE_TABLET)) {
            return DEVICE_TYPE_TABLET;
        }

        // Desktop par défaut
        return DEVICE_TYPE_DESKTOP;
    }

    // UTILITAIRES PUBLICS

    public String extractBrowserName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String browser = extractBrowser(userAgent);
        return browser.replace(" Mobile", "");
    }
}
