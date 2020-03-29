package com.marvin.framework.webmvc.servlet;


import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MyDispatcherServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //6、等待请求
        doDispatch();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //从这里开始启动

        //1、加载配置文件
        doLoadConfig();

        //2、扫描所有相关的类
        doScanner();

        //3、初始化所有相关的类
        doInstance();

        //4、自动注入
        doAutoWired();
        

        //=====================spring 核心初始化成功====================



        //5、初始化HandlerMapping，属于springMVC的内容
        initHandlerMapping();
    }

    private void initHandlerMapping() {
    }

    private void doAutoWired() {
    }
    private void doInstance() {
    }
    private void doScanner() {
    }

    private void doLoadConfig() {
    }

    private void doDispatch(){}
}
