package com.github.senjars.carsharing;

import com.github.senjars.carsharing.config.SecurityConfig;
import com.github.senjars.carsharing.config.TestSecurityConfig;
import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class JvPortfolioCarSharingServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
