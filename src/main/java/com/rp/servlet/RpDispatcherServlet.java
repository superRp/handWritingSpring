/*
 * Copyright (c) ACCA Corp.
 * All Rights Reserved.
 */
package com.rp.servlet;

import com.rp.annotation.RpAutowired;
import com.rp.annotation.RpController;
import com.rp.annotation.RpRequestMapping;
import com.rp.annotation.RpService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author Rui Peng, 2020/2/3
 * @version OPRA v1.0
 **/
public class RpDispatcherServlet extends HttpServlet {

    private static final String LOCATION="contextConfigLocation";

    //保存配置信息
    private Properties properties=new Properties();

    //保存所有被扫描的类信息
    private List<String> classNames=new ArrayList<>();

    //ioc容器
    private Map<String,Object> ioc=new HashMap<>();

    //保存所有url和method的映射关系
    private Map<String,Method> handlerMapping=new HashMap<>();

    public RpDispatcherServlet() {
        super();
    }

    /**
     * 初始化，加载配置文件
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));
        //扫描相关类
        doScanner(properties.getProperty("scanPackage"));
        try {
            //初始化所有相关类的实例，并保存到ioc容器
            doInstance();
            //依赖注入
            doAutowired();
            //方法映射
            doMethodMapping();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }


    }

    private void doMethodMapping() throws ClassNotFoundException {
        for(String name:classNames){
            Class<?> clazz = Class.forName(name);
            if(clazz.isAnnotationPresent(RpController.class)){
                Method[] methods = clazz.getMethods();
                for(Method method:methods){
                    if(method.isAnnotationPresent(RpRequestMapping.class)){
                        RpRequestMapping annotation = method.getAnnotation(RpRequestMapping.class);
                        String value = annotation.value();
                        if ("".equals(value)){
                            throw new ClassNotFoundException();
                        }
                        handlerMapping.put(value,method);
                    }
                }
            }
        }
    }

    private void doAutowired() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        for(String  name:classNames){
            Class<?> clazz = Class.forName(name);
            Field[] fields = clazz.getDeclaredFields();
            for(Field field:fields){
                if(field.isAnnotationPresent(RpAutowired.class)){
                    RpAutowired rpAutowired = field.getAnnotation(RpAutowired.class);
                    String value = rpAutowired.value();
                    if("".equals(value)){
                        value=lowerFirstCase(field.getType().getSimpleName());
                    }
                    field.setAccessible(true);
                    String simpleName = clazz.getSimpleName();
                    field.set(ioc.get(lowerFirstCase(simpleName)),ioc.get(value));
                }
            }
        }
    }

    private void doInstance() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if(classNames.size()==0){
            return;
        }
        for(int i=0;i<classNames.size();i++){
                Class<?> clazz = Class.forName(classNames.get(i));
                if(clazz.isAnnotationPresent(RpController.class)){
                    ioc.put(lowerFirstCase(clazz.getSimpleName()),clazz.newInstance());
                }else if( clazz.isAnnotationPresent(RpService.class)){
                    RpService rpService = clazz.getAnnotation(RpService.class);
                    String name = lowerFirstCase(clazz.getSimpleName());
                    String value = rpService.value();
                    if(!"".equals(value.trim())){
                        name=value;
                    }
                    ioc.put(name,clazz.newInstance());
                }else{
                    continue;
                }

        }

    }

    /**
     * 将首字母小写
     * @return
     */
    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);

    }

    private void doScanner(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File file=new File(url.getFile());
        File[] files = file.listFiles();
        for(int i=0;i<files.length;i++){
            if(files[i].isDirectory()){
                doScanner(packageName+"."+files[i].getName());
            }else{
                classNames.add(packageName+"."+files[i].getName().replace(".class",""));
            }
        }
    }

    private void doLoadConfig(String location) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            if(null!=inputStream){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if(handlerMapping.isEmpty()){
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url=url.replace(contextPath,"").replaceAll("/+","/");
        if(handlerMapping.get(url)==null){
            resp.getWriter().write("404 not find");
        }
        Method method = handlerMapping.get(url);
        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        try {
            method.invoke(ioc.get(beanName),null);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }


}
