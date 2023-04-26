package com.insight.base.auth;

import com.insight.base.auth.common.Core;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AuthApplicationTests {
    @Autowired
    private Core core;

    @Test
    public void contextLoads() {
    }
}
