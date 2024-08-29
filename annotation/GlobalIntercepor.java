package com.easychat.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//作用在方法上
@Target(ElementType.METHOD)
//在运行时生效
@Retention(RetentionPolicy.RUNTIME)
public @interface GlobalIntercepor {

    boolean checkLogin() default true;

    boolean checkAdmin() default false;
}
