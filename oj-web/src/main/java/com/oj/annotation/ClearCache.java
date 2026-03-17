package com.oj.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClearCache {
    //支持多个key
    String[] keyPatterns() default {};
    String prefix() default "";
    CacheType type() default CacheType.AUTO;
}

