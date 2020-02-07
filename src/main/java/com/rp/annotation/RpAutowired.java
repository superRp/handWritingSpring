/*
 * Copyright (c) ACCA Corp.
 * All Rights Reserved.
 */
package com.rp.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Rui Peng, 2020/2/3
 * @version OPRA v1.0
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RpAutowired {
    String value() default "";
}
