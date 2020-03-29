package com.marvin.demo.service.impl;

import com.marvin.demo.service.IDemoService;
import com.marvin.framework.annotation.MyService;

//@MyService("aa")
@MyService
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is "+name;
    }
}
