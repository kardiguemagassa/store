package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuration class for custom Web MVC settings in the application.
 *
 * This class implements the `WebMvcConfigurer` interface and provides
 * configuration for handling static resource requests, specifically for
 * serving uploaded files located in the application's defined uploads directory.
 *
 * The upload directory used for serving files can be configured using the
 * `app.file.upload-dir` property. By default, it resolves relative to the
 * current working directory.
 *
 * @author Kardigué
 * @version 1.1 - FIXED
 * @since 2025-10-27
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    //uploads/products/xxx.png  ou l'images est stocker
    /**
     * The directory path where uploaded files will be stored.
     *
     * This variable is configured using the `app.file.upload-dir` property in the application's
     * configuration file. When the property is not explicitly set, it defaults to `./uploads`.
     *
     * The path can be either absolute or relative. If it is relative (e.g., starts with `./`), it will
     * be resolved against the application's working directory (retrieved via `System.getProperty("user.dir")`).
     *
     * This directory is intended to hold all files uploaded to the application and may be exposed via
     * resource handlers to serve the files in response to HTTP requests.
     */
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * Configures resource handlers for serving static resources. This method is specifically
     * designed to expose the application's upload directory as a resource handler, allowing
     * files in the upload directory to be accessible via HTTP requests.
     *
     * It determines the absolute path for the upload directory based on the configured
     * upload directory path, logs various debug information regarding the directory's existence
     * and contents, and configures a resource handler for serving files located in this directory.
     *
     * @param resourceHandlerRegistry the registry for configuring resource handlers. This parameter
     *                                provides methods to register resource handlers and specify
     *                                locations from which static resources should be served.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {

        String absoluteUploadPath = uploadDir.startsWith("./")
                ? System.getProperty("user.dir") + uploadDir.substring(1)
                : uploadDir;

        log.debug("  --> Debug WebConfig:");
        log.debug("  --> user.dir: " + System.getProperty("user.dir"));
        log.debug("  --> uploadDir: " + uploadDir);
        log.debug("  --> absoluteUploadPath: " + absoluteUploadPath);

        java.io.File dir = new java.io.File(absoluteUploadPath);
        log.debug("   --> Dossier existe: " + dir.exists());
        log.debug("   -->  Dossier est readable: " + dir.canRead());

        if (dir.exists()) {
            String[] files = dir.list();
            log.debug("   --> Fichiers dans uploads: " + (files != null ? java.util.Arrays.toString(files) : "aucun"));
        }

        resourceHandlerRegistry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + "/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }
}