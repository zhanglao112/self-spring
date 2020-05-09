package com.zhanglao.example.zcontroller;

import com.zhanglao.example.zservice.ITestZService;
import com.zhanglao.zspring.annotation.ZAutowired;
import com.zhanglao.zspring.annotation.ZController;
import com.zhanglao.zspring.annotation.ZRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@ZController
@ZRequestMapping("/test")
public class TestController {

    @ZAutowired
    ITestZService testZService;

    @ZRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response) {
        if (request.getParameter("zlp") == null) {
            try {
                response.getWriter().write("param zlp is null");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String paramName = request.getParameter("zlp");

            try {
                response.getWriter().write("param zlp is " + paramName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @ZRequestMapping("/listClassName")
    public void listClassName(HttpServletRequest request, HttpServletResponse response) {
        String str = testZService.listClassName();

        try {
            response.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
