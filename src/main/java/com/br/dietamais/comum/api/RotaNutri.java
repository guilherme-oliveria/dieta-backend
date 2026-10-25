package com.br.dietamais.comum.api;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RotaNutri { boolean exclusiva() default true; }
