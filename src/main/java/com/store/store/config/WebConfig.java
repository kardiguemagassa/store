package com.store.store.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    //uploads/products/xxx.png  ou l'images est stocker
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

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