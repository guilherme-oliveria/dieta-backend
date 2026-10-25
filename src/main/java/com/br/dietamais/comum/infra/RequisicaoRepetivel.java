package com.br.dietamais.comum.infra;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

final class RequisicaoRepetivel extends HttpServletRequestWrapper {
	private final byte[] corpo;

	RequisicaoRepetivel(HttpServletRequest requisicao, byte[] corpo) {
		super(requisicao);
		this.corpo = corpo;
	}

	@Override
	public ServletInputStream getInputStream() {
		var origem = new ByteArrayInputStream(corpo);
		return new ServletInputStream() {
			@Override public int read() { return origem.read(); }
			@Override public boolean isFinished() { return origem.available() == 0; }
			@Override public boolean isReady() { return true; }
			@Override public void setReadListener(ReadListener listener) { throw new IllegalStateException("API JSON síncrona"); }
		};
	}

	@Override
	public BufferedReader getReader() {
		return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
	}
}
