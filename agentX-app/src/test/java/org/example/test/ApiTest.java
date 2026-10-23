package org.example.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * 上下文加载冒烟测试。
 *
 * <p>它会真实拉起 Spring 容器，因此依赖本机 MySQL、PostgreSQL 以及
 * {@code JWT_SECRET}、{@code DS_API_KEY} 两个环境变量。常规单测不该被环境卡住，
 * 所以默认忽略；需要验证「整套配置能否装配起来」时，去掉 {@code @Ignore} 再跑。</p>
 */
@Slf4j
@Ignore("需要本地 MySQL/PostgreSQL 与环境变量，作为环境联调用例手动启用")
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApiTest {

    @Test
    public void test() {
        log.info("测试完成");
    }

}
