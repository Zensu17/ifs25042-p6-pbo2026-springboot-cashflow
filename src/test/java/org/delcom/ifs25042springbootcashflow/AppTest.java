package org.delcom.ifs25042springbootcashflow;

import org.delcom.ifs25042springbootcashflow.domain.repository.IUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

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
}
