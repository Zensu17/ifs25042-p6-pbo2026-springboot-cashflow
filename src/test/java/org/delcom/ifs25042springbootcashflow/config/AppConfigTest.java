package org.delcom.ifs25042springbootcashflow.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.delcom.ifs25042springbootcashflow.adapter.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void vaadinBuildCacheFilterAppliesImmutableCacheOnlyToBuildAssets() throws Exception {
        AppConfig config = new AppConfig();
        Filter filter = config.vaadinBuildCacheFilter();

        HttpServletRequest nullUriRequest = mock(HttpServletRequest.class);
        HttpServletResponse nullUriResponse = mock(HttpServletResponse.class);
        FilterChain nullUriChain = mock(FilterChain.class);
        filter.doFilter(nullUriRequest, nullUriResponse, nullUriChain);
        verify(nullUriChain).doFilter(nullUriRequest, nullUriResponse);
        verify(nullUriResponse, never()).setHeader(eq("Cache-Control"), anyString());

        HttpServletRequest loginRequest = mock(HttpServletRequest.class);
        when(loginRequest.getRequestURI()).thenReturn("/login");
        HttpServletResponse loginResponse = mock(HttpServletResponse.class);
        FilterChain loginChain = mock(FilterChain.class);
        filter.doFilter(loginRequest, loginResponse, loginChain);
        verify(loginChain).doFilter(loginRequest, loginResponse);
        verify(loginResponse, never()).setHeader(eq("Cache-Control"), anyString());

        HttpServletRequest buildRequest = mock(HttpServletRequest.class);
        when(buildRequest.getRequestURI()).thenReturn("/VAADIN/build/generated-flow-imports-abc.js");
        HttpServletResponse buildResponse = mock(HttpServletResponse.class);
        FilterChain buildChain = mock(FilterChain.class);
        doAnswer(invocation -> {
            HttpServletResponse wrapped = invocation.getArgument(1);
            wrapped.setHeader("Cache-Control", "max-age=3600");
            wrapped.setHeader("Content-Type", "application/javascript");
            return null;
        }).when(buildChain).doFilter(eq(buildRequest), org.mockito.ArgumentMatchers.any(HttpServletResponse.class));

        filter.doFilter(buildRequest, buildResponse, buildChain);
        verify(buildResponse).setHeader("Cache-Control", "public, max-age=31536000, immutable");
        verify(buildResponse, never()).setHeader("Cache-Control", "max-age=3600");
        verify(buildResponse).setHeader("Content-Type", "application/javascript");
    }
}
