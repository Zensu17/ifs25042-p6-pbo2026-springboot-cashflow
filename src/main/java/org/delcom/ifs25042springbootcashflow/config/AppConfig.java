package org.delcom.ifs25042springbootcashflow.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.delcom.ifs25042springbootcashflow.adapter.ai.OpenAiFinanceService;
import org.delcom.ifs25042springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25042springbootcashflow.adapter.repository.JdbcTransactionRepository;
import org.delcom.ifs25042springbootcashflow.adapter.repository.JdbcUserRepository;
import org.delcom.ifs25042springbootcashflow.adapter.repository.SchemaInitializer;
import org.delcom.ifs25042springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25042springbootcashflow.domain.repository.IFinanceAiService;
import org.delcom.ifs25042springbootcashflow.domain.repository.ITransactionRepository;
import org.delcom.ifs25042springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25042springbootcashflow.usecase.AuthUseCase;
import org.delcom.ifs25042springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25042springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25042springbootcashflow.usecase.UserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Composition root Spring: merakit dependency antar-layer.
 */
@Configuration
public class AppConfig {

    @Bean
    public IUserRepository userRepository(DataSource dataSource) {
        SchemaInitializer.initialize(dataSource);
        return new JdbcUserRepository(dataSource);
    }

    @Bean
    public ITransactionRepository transactionRepository(DataSource dataSource) {
        return new JdbcTransactionRepository(dataSource);
    }

    @Bean
    public FinanceUseCase financeUseCase(ITransactionRepository transactionRepository) {
        return new FinanceUseCase(transactionRepository);
    }

    @Bean
    public IFinanceAiService financeAiService(
            @Value("${OPENAI_ENDPOINT:https://delcom.org/api/v1}") String endpoint,
            @Value("${OPENAI_KEY:key}") String key,
            @Value("${OPENAI_MODEL:nama-model}") String model
    ) {
        return new OpenAiFinanceService(endpoint, key, model);
    }

    @Bean
    public FinanceAiUseCase financeAiUseCase(
            IFinanceAiService financeAiService,
            ITransactionRepository transactionRepository
    ) {
        return new FinanceAiUseCase(financeAiService, transactionRepository);
    }

    @Bean
    public AuthUseCase authUseCase(IUserRepository userRepository) {
        return new AuthUseCase(userRepository);
    }

    @Bean
    public UserUseCase userUseCase(IUserRepository userRepository) {
        return new UserUseCase(userRepository);
    }

    @Bean
    public FinancePresenter financePresenter() {
        return new FinancePresenter();
    }

    @Bean
    public FileStorage fileStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
        return new FileStorage(uploadDir);
    }

    @Bean
    public ApplicationRunner seedRootAdmin(
            AuthUseCase authUseCase,
            @Value("${app.root-username:admin}") String rootUsername,
            @Value("${app.root-password:admin}") String rootPassword
    ) {
        return args -> authUseCase.ensureRootAdmin(rootUsername, rootPassword);
    }

    @Bean
    public WebMvcConfigurer uploadResourceConfigurer(FileStorage fileStorage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String location = fileStorage.root().toUri().toString();
                if (!location.endsWith("/")) {
                    location = location + "/";
                }
                registry.addResourceHandler("/uploads/**").addResourceLocations(location);
            }
        };
    }

    @Bean
    public Filter vaadinBuildCacheFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain
            ) throws ServletException, IOException {
                String uri = request.getRequestURI();
                if (uri != null && uri.startsWith("/VAADIN/build/")) {
                    response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
                    HttpServletResponse wrapped = new HttpServletResponseWrapper(response) {
                        @Override
                        public void setHeader(String name, String value) {
                            if (!"Cache-Control".equalsIgnoreCase(name)) {
                                super.setHeader(name, value);
                            }
                        }
                    };
                    filterChain.doFilter(request, wrapped);
                    return;
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
