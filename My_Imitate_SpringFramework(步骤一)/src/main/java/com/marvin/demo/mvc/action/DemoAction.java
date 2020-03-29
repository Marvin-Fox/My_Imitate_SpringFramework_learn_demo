package com.marvin.demo.mvc.action;

import com.marvin.demo.service.IDemoService;
import com.marvin.framework.annotation.MyAutowired;
import com.marvin.framework.annotation.MyController;
import com.marvin.framework.annotation.MyRequestMapping;
import com.marvin.framework.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("/demo")
public class DemoAction {

    @MyAutowired
    private IDemoService demoService;

    @MyRequestMapping("/query.json")
    public void query(HttpServletRequest request, HttpServletResponse response,
                      @MyRequestParam("name") String name){
        String resoult = demoService.get(name);
        try {
            response.getWriter().write(resoult);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}
