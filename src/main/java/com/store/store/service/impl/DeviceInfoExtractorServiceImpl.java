package com.store.store.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service implementation responsible for extracting device information from a given User-Agent string.
 * The service parses the User-Agent to determine the operating system, browser,
 * and device type to provide meaningful insights about the client device.
 *
 * @author Kardigué
 * @version 3.0 (JWT + Refresh Token + Cookies)
 * @since 2025-11-01
 */
@Slf4j
@Service
public class DeviceInfoExtractorServiceImpl {

    /**
     * Extracts device information such as operating system, browser, and device type
     * from a given User-Agent string.
     *
     * @param userAgent the User-Agent string used to identify the device's operating system,
     *                  browser, and type. May be null or empty.
     * @return a formatted string containing the operating system, browser, and device type,
     *         separated by hyphens, e.g., "OS - Browser - DeviceType".
     *         Returns "Unknown Device" if extraction fails, or null if the input User-Agent
     *         is null or empty.
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
     * Extracts the operating system name and version from the provided User-Agent string.
     *
     * @param userAgent the User-Agent string used to identify the operating system.
     *                  It can include details about the device's OS such as "Windows NT 10.0",
     *                  "Mac OS X 10_15_7", "Android 13", etc. May be null or empty.
     * @return a string containing the extracted operating system name and version,
     *         such as "Windows 10", "macOS Catalina", or "Android 13".
     *         Returns "Unknown OS" if the operating system cannot be determined,
     *         or null if the input User-Agent is null or empty.
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
     * Extracts the browser name based on the given User-Agent string.
     * This method analyzes the User-Agent to determine the browser type,
     * including specific cases such as distinguishing between desktop and mobile browsers.
     *
     * @param userAgent the User-Agent string used to identify the browser. It may include
     *                  details like browser name, version, and whether it is mobile or desktop.
     *                  Can be null or empty.
     * @return a string representing the browser name. Possible values include:
     *         "Edge", "Chrome", "Chrome Mobile", "Firefox", "Firefox Mobile", "Safari",
     *         "Safari Mobile", "Opera", "Internet Explorer", or "Unknown Browser".
     *         Returns "Unknown Browser" if the browser cannot be determined,
     *         or null if the input User-Agent is null or empty.
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
     * Determines the type of device (Mobile, Tablet, or Desktop) based on the provided User-Agent string.
     *
     * @param userAgent the User-Agent string used to identify the device type. It may include keywords
     *                  such as "Mobile", "Android", "iPhone", "iPad", or "Tablet". Can be null or empty.
     * @return a string representing the device type. Possible return values are:
     *         - "Mobile" if the User-Agent contains indicators of a mobile device.
     *         - "Tablet" if the User-Agent contains indicators of a tablet device.
     *         - "Desktop" if none of the mobile or tablet indicators are present.
     *         Returns "Desktop" by default if no specific device indicators are found.
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
     * Extracts the browser name from the given User-Agent string.
     * This method removes the "Mobile" suffix from the browser name
     * if present to provide a simplified browser name.
     *
     * @param userAgent the User-Agent string used to identify the browser.
     *                  It may include details like browser name, version,
     *                  or device type. Can be null or empty.
     * @return a simplified string representing the browser name. Possible values include:
     *         "Edge", "Chrome", "Firefox", "Safari", "Opera", "Internet Explorer", etc.
     *         Returns "Unknown" if the input is null, empty, or the browser cannot be determined.
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