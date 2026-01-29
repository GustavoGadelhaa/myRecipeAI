package com.projeto.llm.receita.receita.IA.notificationAdm;

import com.projeto.llm.receita.receita.IA.webService.BrevoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationAdm {
private final BrevoService brevoService;
    public void sendAlertToAdm(){
        log.error("Alerta enviado pro ADM as {}", ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        brevoService.sendEmailToAdm("gustavogads4@gmail.com");
    }
}
