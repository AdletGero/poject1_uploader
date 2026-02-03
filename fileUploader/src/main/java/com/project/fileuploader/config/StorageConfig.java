package com.project.fileuploader.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StorageConfig {

    @Bean
    public Path tmpDir(@Value("${app.tmp-dir}") String tmpDir) {
        return Paths.get(tmpDir);
    }
}