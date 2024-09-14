package org.ipring.dubbo;

import org.apache.dubbo.config.annotation.DubboService;
import org.ipring.api.DemoService;

@DubboService
public class DemoServiceImpl implements DemoService {
    @Override
    public String sayHello(String name) {
        return "Hello " + name;
    }
}