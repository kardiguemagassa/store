package com.store.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service pour extraire les informations du device depuis le User-Agent.
 *
 * Ce service analyse le User-Agent HTTP pour déterminer:
 * - Le système d'exploitation (Windows, macOS, Linux, Android, iOS)
 * - Le type d'appareil (Desktop, Mobile, Tablet)
 * - Le navigateur (Chrome, Firefox, Safari, Edge)
 *
 * @author Kardigué
 * @version 1.0
 * @since 2025-01-27
 */
@Slf4j
@Service
public class DeviceInfoExtractor {

    /**
     * Extrait les informations du device depuis le User-Agent.
     *
     * Exemple de retour:
     * - "Windows 10 - Chrome - Desktop"
     * - "Android 13 - Chrome Mobile - Mobile"
     * - "iOS 17 - Safari - Mobile"
     *
     * @param userAgent User-Agent HTTP header
     * @return String formatée avec les infos du device, ou null si userAgent est null
     */
    public String extractDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return null;
        }

        try {
            String os = extractOS(userAgent);
            String browser = extractBrowser(userAgent);
            String deviceType = extractDeviceType(userAgent);

            return String.format("%s - %s - %s", os, browser, deviceType);
        } catch (Exception e) {
            log.warn("Failed to extract device info from User-Agent: {}", e.getMessage());
            return "Unknown Device";
        }
    }

    /**
     * Extrait le système d'exploitation depuis le User-Agent.
     */
    private String extractOS(String userAgent) {
        if (userAgent.contains("Windows NT 10.0")) return "Windows 10";
        if (userAgent.contains("Windows NT 6.3")) return "Windows 8.1";
        if (userAgent.contains("Windows NT 6.2")) return "Windows 8";
        if (userAgent.contains("Windows NT 6.1")) return "Windows 7";
        if (userAgent.contains("Windows")) return "Windows";

        if (userAgent.contains("Mac OS X")) {
            // Extraire la version (ex: "Mac OS X 10_15_7")
            if (userAgent.contains("Mac OS X 10_15")) return "macOS Catalina";
            if (userAgent.contains("Mac OS X 11")) return "macOS Big Sur";
            if (userAgent.contains("Mac OS X 12")) return "macOS Monterey";
            if (userAgent.contains("Mac OS X 13")) return "macOS Ventura";
            if (userAgent.contains("Mac OS X 14")) return "macOS Sonoma";
            return "macOS";
        }

        if (userAgent.contains("Android")) {
            // Extraire la version (ex: "Android 13")
            String[] parts = userAgent.split("Android ");
            if (parts.length > 1) {
                String version = parts[1].split(";")[0].trim();
                return "Android " + version;
            }
            return "Android";
        }

        if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            // Extraire la version iOS (ex: "OS 17_0")
            if (userAgent.contains("OS 17")) return "iOS 17";
            if (userAgent.contains("OS 16")) return "iOS 16";
            if (userAgent.contains("OS 15")) return "iOS 15";
            return "iOS";
        }

        if (userAgent.contains("Linux")) return "Linux";
        if (userAgent.contains("Ubuntu")) return "Ubuntu";
        if (userAgent.contains("CrOS")) return "Chrome OS";

        return "Unknown OS";
    }

    /**
     * Extrait le navigateur depuis le User-Agent.
     */
    private String extractBrowser(String userAgent) {
        // Ordre important : vérifier Edge avant Chrome (Edge contient "Chrome")
        if (userAgent.contains("Edg/")) {
            return "Edge";
        }

        if (userAgent.contains("Chrome/") && !userAgent.contains("Edg")) {
            if (userAgent.contains("Mobile")) {
                return "Chrome Mobile";
            }
            return "Chrome";
        }

        if (userAgent.contains("Firefox/")) {
            if (userAgent.contains("Mobile")) {
                return "Firefox Mobile";
            }
            return "Firefox";
        }

        if (userAgent.contains("Safari/") && !userAgent.contains("Chrome")) {
            if (userAgent.contains("Mobile")) {
                return "Safari Mobile";
            }
            return "Safari";
        }

        if (userAgent.contains("Opera/") || userAgent.contains("OPR/")) {
            return "Opera";
        }

        if (userAgent.contains("MSIE") || userAgent.contains("Trident/")) {
            return "Internet Explorer";
        }

        return "Unknown Browser";
    }

    /**
     * Extrait le type d'appareil depuis le User-Agent.
     */
    private String extractDeviceType(String userAgent) {
        // Mobile
        if (userAgent.contains("Mobile") ||
                userAgent.contains("Android") ||
                userAgent.contains("iPhone")) {
            return "Mobile";
        }

        // Tablet
        if (userAgent.contains("iPad") ||
                userAgent.contains("Tablet")) {
            return "Tablet";
        }

        // Desktop par défaut
        return "Desktop";
    }

    /**
     * Extrait uniquement le nom du navigateur (pour comparaison simple).
     */
    public String extractBrowserName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown";
        }

        String browser = extractBrowser(userAgent);
        // Enlever "Mobile" du nom pour comparaison
        return browser.replace(" Mobile", "");
    }
}