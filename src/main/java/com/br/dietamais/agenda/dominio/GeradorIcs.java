package com.br.dietamais.agenda.dominio;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneOffsetTransitionRule;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Feed iCalendar (RFC 5545) com um evento diário por refeição. Título genérico: nunca leva alimento nem dado de saúde (RN-NOT-02). */
public final class GeradorIcs {
	public record Refeicao(UUID id,String nome,LocalTime horario) {}
	private static final DateTimeFormatter LOCAL=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
	private static final DateTimeFormatter UTC=DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(java.time.ZoneOffset.UTC);
	private static final Duration DURACAO=Duration.ofMinutes(30);
	private GeradorIcs(){}

	public static String gerar(List<Refeicao> refeicoes,Instant inicioVigencia,ZoneId fuso){
		var l=new ArrayList<String>();
		l.add("BEGIN:VCALENDAR");l.add("VERSION:2.0");l.add("PRODID:-//Dieta+//Refeicoes//PT-BR");l.add("CALSCALE:GREGORIAN");l.add("METHOD:PUBLISH");
		l.add("X-WR-CALNAME:Dieta+");l.add("X-WR-TIMEZONE:"+fuso.getId());l.add("REFRESH-INTERVAL;VALUE=DURATION:PT12H");l.add("X-PUBLISHED-TTL:PT12H");
		if(!refeicoes.isEmpty()){
			l.addAll(fusoHorario(fuso,inicioVigencia));
			var inicio=inicioVigencia.atZone(fuso).toLocalDate();
			for(var r:refeicoes){
				l.add("BEGIN:VEVENT");
				l.add("UID:refeicao-"+r.id()+"@dietamais.com.br");
				l.add("DTSTAMP:"+UTC.format(inicioVigencia));l.add("LAST-MODIFIED:"+UTC.format(inicioVigencia));
				l.add("DTSTART;TZID="+fuso.getId()+":"+LOCAL.format(LocalDateTime.of(inicio,r.horario())));
				l.add("DTEND;TZID="+fuso.getId()+":"+LOCAL.format(LocalDateTime.of(inicio,r.horario()).plus(DURACAO)));
				l.add("RRULE:FREQ=DAILY");
				l.add("SUMMARY:"+escapar("Dieta+: "+r.nome()));
				l.add("TRANSP:TRANSPARENT");
				l.add("END:VEVENT");
			}
		}
		l.add("END:VCALENDAR");
		var sb=new StringBuilder();
		for(var linha:l)dobrar(sb,linha);
		return sb.toString();
	}
	/** VTIMEZONE a partir das regras do java.time: offset fixo, ou as regras anuais de horário de verão. */
	static List<String> fusoHorario(ZoneId fuso,Instant ref){
		var regras=fuso.getRules();var l=new ArrayList<String>();
		l.add("BEGIN:VTIMEZONE");l.add("TZID:"+fuso.getId());
		var transicoes=regras.getTransitionRules();
		var base=LocalDateTime.of(1970,1,1,0,0);
		if(transicoes.isEmpty()){
			var o=regras.getOffset(ref);
			l.add("BEGIN:STANDARD");l.add("DTSTART:"+LOCAL.format(base));l.add("TZOFFSETFROM:"+deslocamento(o.getTotalSeconds()));l.add("TZOFFSETTO:"+deslocamento(o.getTotalSeconds()));l.add("END:STANDARD");
		}else{
			for(var t:transicoes){
				boolean verao=t.getOffsetAfter().getTotalSeconds()>t.getStandardOffset().getTotalSeconds();
				l.add(verao?"BEGIN:DAYLIGHT":"BEGIN:STANDARD");
				l.add("DTSTART:"+LOCAL.format(base.withMonth(t.getMonth().getValue()).withDayOfMonth(1).with(t.getLocalTime())));
				l.add("RRULE:FREQ=YEARLY;BYMONTH="+t.getMonth().getValue()+";"+regraDia(t));
				l.add("TZOFFSETFROM:"+deslocamento(t.getOffsetBefore().getTotalSeconds()));l.add("TZOFFSETTO:"+deslocamento(t.getOffsetAfter().getTotalSeconds()));
				l.add(verao?"END:DAYLIGHT":"END:STANDARD");
			}
		}
		l.add("END:VTIMEZONE");return l;
	}
	private static String regraDia(ZoneOffsetTransitionRule t){
		var dia=t.getDayOfWeek()==null?null:diaSigla(t.getDayOfWeek().getValue());
		int ind=t.getDayOfMonthIndicator();
		if(dia==null)return "BYMONTHDAY="+(ind<0?-1:ind);
		if(ind<0)return "BYDAY=-1"+dia;
		var dias=new StringBuilder();for(int d=ind;d<ind+7;d++){if(d>ind)dias.append(',');dias.append(d);}
		return "BYDAY="+dia+";BYMONTHDAY="+dias;
	}
	private static String diaSigla(int dow){return new String[]{"MO","TU","WE","TH","FR","SA","SU"}[dow-1];}
	private static String deslocamento(int segundos){
		char sinal=segundos<0?'-':'+';int a=Math.abs(segundos);return String.format("%c%02d%02d",sinal,a/3600,a%3600/60);
	}
	static String escapar(String t){return t.replace("\\","\\\\").replace(";","\\;").replace(",","\\,").replace("\r","").replace("\n","\\n");}
	/** Dobra em 75 octetos (RFC 5545 3.1) sem partir caractere UTF-8. */
	static void dobrar(StringBuilder sb,String linha){
		int atual=0;
		for(int i=0;i<linha.length();){
			int cp=linha.codePointAt(i);var s=new String(Character.toChars(cp));int bytes=s.getBytes(StandardCharsets.UTF_8).length;
			if(atual+bytes>75){sb.append("\r\n ");atual=1;}
			sb.append(s);atual+=bytes;i+=Character.charCount(cp);
		}
		sb.append("\r\n");
	}
	public static LocalDate dia(Instant i,ZoneId fuso){return i.atZone(fuso).toLocalDate();}
}
