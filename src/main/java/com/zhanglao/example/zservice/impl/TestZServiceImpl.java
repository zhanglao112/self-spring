package com.zhanglao.example.zservice.impl;

import com.zhanglao.example.zservice.ITestZService;
import com.zhanglao.zspring.annotation.ZService;

@ZService
public class TestZServiceImpl implements ITestZService {
    @Override
    public String listClassName() {
        return "TestZServiceImpl test";
    }
}
