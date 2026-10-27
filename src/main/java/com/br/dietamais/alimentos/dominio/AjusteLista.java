package com.br.dietamais.alimentos.dominio;

import java.util.*;

public record AjusteLista(UUID vinculoId,List<UUID> base,List<UUID> adicionados,List<UUID> removidos,long versao) {}
