package com.insight.base.auth;

import com.insight.base.auth.common.Core;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AuthApplicationTests {
@Autowired
private Core core;

    @Test
    public void contextLoads() {
    }

}
