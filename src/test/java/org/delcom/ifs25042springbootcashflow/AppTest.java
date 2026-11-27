package org.delcom.ifs25042springbootcashflow;

import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.lumo.Lumo;
import org.delcom.ifs25042springbootcashflow.domain.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class AppTest {

    @Autowired
    private IUserRepository userRepository;

    @Test
    void contextLoadsAndSeedsRootAdmin() {
        assertThat(userRepository.findByUsername("admin")).isPresent();
    }

    @Test
    void mainLoadsDotEnvAndRunsSpringApplication() throws Exception {
        String[] args = {"--test-arg"};
        try (MockedStatic<SpringApplication> springApp = Mockito.mockStatic(SpringApplication.class)) {
            springApp.when(() -> SpringApplication.run(App.class, args)).thenReturn(null);
            App.main(args);
            springApp.verify(() -> SpringApplication.run(App.class, args));
        }
    }

    @Test
    void configurePageAddsMetaDescriptionAndRobotsTxtExists() {
        App app = new App();
        AppShellSettings settings = mock(AppShellSettings.class);
        app.configurePage(settings);
        verify(settings).addMetaTag(eq("description"), anyString());
        verify(settings).addLink(eq(Lumo.STYLESHEET + "?v-c=25.3.4"), anyMap());
        verify(settings).addLink(eq("styles.css?v-c=1"), anyMap());
        verify(settings).addInlineWithContents(anyString(), eq(Inline.Wrapping.NONE));
        assertThat(new ClassPathResource("META-INF/resources/robots.txt").exists()).isTrue();
    }
}
