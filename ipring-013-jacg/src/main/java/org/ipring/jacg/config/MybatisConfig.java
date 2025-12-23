package org.ipring.jacg.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@MapperScan({"org.ipring.jacg.mapper.**"})
@Configuration
public class MybatisConfig {
}
