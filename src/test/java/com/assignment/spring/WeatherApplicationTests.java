package com.assignment.spring;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class WeatherApplicationTests {

    @Test
    public void contextLoads() {
        WeatherApplication.main( new String[] { "--spring.profiles.active=test" } );
    }

}
