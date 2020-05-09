package com.zhanglao.zspring.servlet;

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

import com.zhanglao.zspring.annotation.ZAutowired;
import com.zhanglao.zspring.annotation.ZController;
import com.zhanglao.zspring.annotation.ZRequestMapping;
import com.zhanglao.zspring.annotation.ZService;


public class ZDispatchServlet extends HttpServlet {

    private static final String EMPTY_STRING = "";
    private static final String SLASH_STRING = "/";
    private static final String SPOT_STRING = ".";

    private Properties contextConfig = new Properties();

    private List<String> classNameList = new ArrayList<>();

    Map<String, Object> iocMap = new HashMap<String, Object>();

    Map<String, Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            doDispatch(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("500 SERVER ERROR Detail:\n" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws InvocationTargetException, IllegalAccessException {
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();

        url = url.replaceAll(contextPath, EMPTY_STRING).replaceAll("/+", SLASH_STRING);

        if (!this.handlerMapping.containsKey(url)) {
            try {
                response.getWriter().write("404 NOT FOUND!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Method method = this.handlerMapping.get(url);
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(iocMap.get(beanName), request, response);
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));

        doScanner(contextConfig.getProperty("scan-package"));

        doInstance();

        doAutowired();

        initHandlerMapping();
    }

    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    private void doLoadConfig(String contextConfigLocation) {
        contextConfigLocation = contextConfigLocation.replace("classpath:", EMPTY_STRING);
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String scanPackage) {
        URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", SLASH_STRING));
        if (resourcePath == null) {
            return;
        }

        File classPath = new File(resourcePath.getFile());
        for (File file : classPath.listFiles()) {
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }

                String className = (scanPackage + "." + file.getName()).replace(".class", EMPTY_STRING);
                classNameList.add(className);
            }
        }
    }

    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }

        try {
            for (String className : classNameList) {
                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(ZController.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.getDeclaredConstructor().newInstance();

                    iocMap.put(beanName, instance);
                } else if (clazz.isAnnotationPresent(ZService.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    ZService zService = clazz.getAnnotation(ZService.class);
                    if (!"".equals(zService.value())) {
                        beanName = zService.value();
                    }

                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    iocMap.put(beanName, instance);

                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The bean name is exist.");
                        }

                        iocMap.put(i.getName(), instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(ZAutowired.class)) {
                    continue;
                }

                ZAutowired zAutowired = field.getAnnotation(ZAutowired.class);
                String beanName = zAutowired.value().trim();

                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }

                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), iocMap.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void initHandlerMapping() {
        if (iocMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(ZController.class)) {
                continue;
            }

            String baseUrl = "";

            if (clazz.isAnnotationPresent(ZRequestMapping.class)) {
                ZRequestMapping zRequestMapping = clazz.getAnnotation(ZRequestMapping.class);
                baseUrl = zRequestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(ZRequestMapping.class)) {
                    continue;
                }

                ZRequestMapping zRequestMapping = method.getAnnotation(ZRequestMapping.class);
                String url = ("/" + baseUrl + "/" + zRequestMapping.value()).replaceAll("/+", SLASH_STRING);
                handlerMapping.put(url, method);
            }
        }
    }
}