package org.nicetu.spb.userservice.service;


import org.nicetu.spb.userservice.constant.AppConstant;
import org.nicetu.spb.userservice.model.dto.request.EmailDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmailService {
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public EmailService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public String sendEmail(EmailDetails emailDetails) {
        return webClientBuilder.baseUrl(AppConstant.DiscoveredDomainApi.API_GATEWAY_HOST).build()
                .post()
                .uri("/api/email/sendMail")
                .bodyValue(emailDetails)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}