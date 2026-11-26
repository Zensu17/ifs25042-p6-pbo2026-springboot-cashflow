package org.delcom.ifs25042springbootcashflow.config;

import org.delcom.ifs25042springbootcashflow.adapter.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void uploadResourceConfigurerHandlesExistingAndNonExistingDirectoryUris() {
        AppConfig config = new AppConfig();

        FileStorage existingStorage = new FileStorage(tempDir.toString());
        WebMvcConfigurer existingConfigurer = config.uploadResourceConfigurer(existingStorage);
        ResourceHandlerRegistry registry1 = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration1 = mock(ResourceHandlerRegistration.class);
        when(registry1.addResourceHandler("/uploads/**")).thenReturn(registration1);

        existingConfigurer.addResourceHandlers(registry1);
        String expectedExisting = existingStorage.root().toUri().toString();
        verify(registration1).addResourceLocations(expectedExisting);

        Path missingDir = tempDir.resolve("not-created-yet");
        FileStorage missingStorage = new FileStorage(missingDir.toString());
        WebMvcConfigurer missingConfigurer = config.uploadResourceConfigurer(missingStorage);
        ResourceHandlerRegistry registry2 = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration registration2 = mock(ResourceHandlerRegistration.class);
        when(registry2.addResourceHandler("/uploads/**")).thenReturn(registration2);

        missingConfigurer.addResourceHandlers(registry2);
        String expectedMissing = missingStorage.root().toUri().toString() + "/";
        verify(registration2).addResourceLocations(expectedMissing);
    }
}
