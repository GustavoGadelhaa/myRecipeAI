package com.projeto.llm.receita.receita.IA.notificationAdm;

import com.projeto.llm.receita.receita.IA.webService.BrevoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component

public class NotificationAdm {
private final BrevoService brevoService;
    public void sendAlertToAdm(){
        brevoService.sendEmailToAdm("gustavogads4@gmail.com");
    }
}
