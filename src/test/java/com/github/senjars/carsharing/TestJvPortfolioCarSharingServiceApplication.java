package com.github.senjars.carsharing;

import com.github.senjars.carsharing.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestJvPortfolioCarSharingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(JvPortfolioCarSharingServiceApplication::main).with(
                TestcontainersConfiguration.class).run(args);
    }
}
