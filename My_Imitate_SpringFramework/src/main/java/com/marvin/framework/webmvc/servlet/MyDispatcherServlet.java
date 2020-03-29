package com.marvin.framework.webmvc.servlet;


import com.marvin.framework.annotation.MyAutowired;
import com.marvin.framework.annotation.MyController;
import com.marvin.framework.annotation.MyRequestMapping;
import com.marvin.framework.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();

    private List<String> classNames = new ArrayList<String>();

    private Map<String,Object> ioc = new HashMap<String,Object>();

    private Map<String,Method> handlerMapping = new HashMap<String,Method>();



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("doGet");
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //6、等待请求
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception");
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //从这里开始启动

        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描所有相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3、初始化所有相关的类
        doInstance();

        //4、自动注入
        doAutoWired();
        

        //=====================spring 核心初始化成功====================



        //5、初始化HandlerMapping，属于springMVC的内容
        initHandlerMapping();

        System.out.println("My Spring init ...");
    }

    private void initHandlerMapping() {
        if(ioc.isEmpty()){return;}

        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyController.class)){continue;}

            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();
            }

            //扫描所有的公共方法
            for(Method method : clazz.getMethods()){
                if(!method.isAnnotationPresent(MyRequestMapping.class)){continue;}

                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String methodUrl = ("/"+baseUrl+requestMapping.value()).replaceAll("/+","/");

                handlerMapping.put(methodUrl,method);

                System.out.println("Mapping :"+methodUrl+","+method);


            }
        }

    }

    private void doAutoWired() {
        if(ioc.isEmpty()){return;}

        //循环IOC容器中所有的类，然后对需要自动赋值的属性进行赋值
        for(Map.Entry<String,Object> entry : ioc.entrySet()){

            //依赖注入，不管是什么权限，只要有注解就注入（破坏了封装性）
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(!field.isAnnotationPresent(MyAutowired.class)){ continue;}

                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();

                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }

                //反射中的暴力访问（setAccessible）
                field.setAccessible(true);

                try {
                    //反射的方式向类的属性中赋值
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }

            }


        }
    }

    private void doInstance() {
        if(classNames.isEmpty()){return;}

        try {
            for(String className : classNames){
                //利用反射进行实例化
                Class<?> clazz = Class.forName(className);

                //不是所有类都要实例化，只认加了注解的类
                if (clazz.isAnnotationPresent(MyController.class)) {
                    //key默认类名首字母小写
                    String beanName = lowerFirstCase(clazz.getName());
                    //存储类名和实例化类对象
                    ioc.put(beanName,clazz.newInstance());

                }else if(clazz.isAnnotationPresent(MyService.class)){

                    //1、如果自定义了名字，优先使用自定义名字
                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();

                    //2、默认使用采用首字母小写
                    if("".equals(beanName.trim())){
                        beanName = lowerFirstCase(clazz.getName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);

                    //3、根据接口类型来赋值
                    for(Class<?> i : clazz.getInterfaces()){
                        ioc.put(i.getName(),instance);
                    }

                }else{
                    continue;
                }

            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doScanner(String scanPackage) {
        //将.替换为/  目的是将包路径换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));

        //获取文件路径
        File classDir = new File(url.getFile());
        //遍历文件路径
        for(File file : classDir.listFiles()){
            if(file.isDirectory()){
                //如果是文件夹，则递归继续扫描该文件夹下的内容
                doScanner(scanPackage+"."+file.getName());
            }else{
                //如果是个文件，则获取完整类名
                String className =  scanPackage+"."+file.getName().replace(".class", "");
                classNames.add(className);
            }
        }

    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(null != inputStream){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception{
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");

        if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found");
            return;
        }

        Method method = handlerMapping.get(url);
        System.out.println(method);
        method.invoke();
    }


    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0] += 32; //大写字母char +32是对应的小写字母(ASCII码原理)
        return String.valueOf(chars);
    }
}
