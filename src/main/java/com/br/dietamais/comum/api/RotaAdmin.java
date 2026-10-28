package com.br.dietamais.comum.api;

import java.lang.annotation.*;

/** Rota do admin da plataforma: só o perfil ADMIN entra. Nenhuma rota de admin devolve dado clínico (RN-LGPD-05). */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RotaAdmin { }
