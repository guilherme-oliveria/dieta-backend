package com.br.dietamais.comum.api;

import java.util.Optional;

public interface AutenticadorConteudo { Optional<String> autenticarRequisicao(byte[] dados); }
