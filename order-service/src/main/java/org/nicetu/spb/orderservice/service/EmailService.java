package org.nicetu.spb.orderservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.nicetu.spb.orderservice.model.dto.order.OrderDto;
import org.nicetu.spb.orderservice.model.dto.order.OrderItemDto;
import org.nicetu.spb.orderservice.security.JwtProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final JwtProvider jwtProvider;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public Mono<Void> sendOrderConfirmationEmail(OrderDto orderDto, String jwtToken) {
        String customerEmail = jwtProvider.getEmailFromToken(jwtToken);

        if (customerEmail == null) {
            log.error("Cannot extract email from JWT token for order: {}", orderDto.getOrderId());
            return Mono.empty();
        }

        log.info("Sending order confirmation email to: {} for order: {}", customerEmail, orderDto.getOrderId());

        return sendEmail(orderDto, customerEmail)
                .doOnSuccess(v -> log.info("Order confirmation email sent to: {} for order: {}", customerEmail, orderDto.getOrderId()))
                .doOnError(e -> log.error("Failed to send order confirmation email to: {} for order: {}", customerEmail, orderDto.getOrderId(), e));
    }

    private Mono<Void> sendEmail(OrderDto orderDto, String customerEmail) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

                helper.setTo(customerEmail);
                helper.setSubject("Информация о заказе #" + orderDto.getOrderId());
                helper.setText(buildEmailTextContent(orderDto, customerEmail));

                mailSender.send(message);
                log.info("Email successfully sent to: {}", customerEmail);

            } catch (MessagingException e) {
                log.error("Error sending email to: {}", customerEmail, e);
                throw new RuntimeException("Failed to send email", e);
            }
        });
    }

    private String buildEmailTextContent(OrderDto orderDto, String customerEmail) {
        StringBuilder sb = new StringBuilder();

        sb.append("Информация о вашем заказе:\n\n");

        sb.append("Номер заказа: ").append(orderDto.getOrderId()).append("\n");
        sb.append("Дата заказа: ").append(orderDto.getOrderDate().format(DATE_FORMATTER)).append("\n");

        if (orderDto.getOrderDesc() != null && !orderDto.getOrderDesc().isEmpty()) {
            sb.append("Описание: ").append(orderDto.getOrderDesc()).append("\n");
        }

        sb.append("\n");
        sb.append("Состав заказа:\n");
        sb.append("----------------------------------------\n");

        if (orderDto.getOrderItemDtos() != null && !orderDto.getOrderItemDtos().isEmpty()) {
            for (OrderItemDto item : orderDto.getOrderItemDtos()) {
                sb.append("Товар ID: ").append(item.getProductId()).append("\n");
                sb.append("Количество: ").append(item.getQuantity()).append("\n");
                sb.append("Цена за единицу: ").append(String.format("%.2f ₽", item.getPrice())).append("\n");
                sb.append("Стоимость: ").append(String.format("%.2f ₽", item.getTotalPrice())).append("\n");
                sb.append("----------------------------------------\n");
            }
        } else {
            sb.append("Нет товаров в заказе\n");
            sb.append("----------------------------------------\n");
        }

        sb.append("\n");
        sb.append("Итоговая стоимость: ").append(String.format("%.2f ₽", orderDto.getOrderFee())).append("\n\n");

        sb.append("С уважением,\n");
        sb.append("Команда магазина\n\n");
        sb.append("Если у вас есть вопросы, свяжитесь с нашей службой поддержки.");

        return sb.toString();
    }
}