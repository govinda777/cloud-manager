package com.govinda777.execution.bdd;

import com.govinda777.execution.infrastructure.db.JpaAccountRepository;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @Autowired
    private JpaAccountRepository jpaAccountRepository;

    @Before
    public void cleanDatabase() {
        jpaAccountRepository.deleteAll();
    }
}
