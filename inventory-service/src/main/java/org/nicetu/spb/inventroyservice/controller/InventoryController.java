package org.nicetu.spb.inventroyservice.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicetu.spb.inventroyservice.model.dto.response.InventoryResponse;
import org.nicetu.spb.inventroyservice.security.JwtValidate;
import org.nicetu.spb.inventroyservice.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    @Autowired
    private final InventoryService inventoryService;

    @Autowired
    private final JwtValidate jwtValidate;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStockNoAccessToken(@RequestHeader(name = "Authorization") String authorizationHeader,
                                                          @RequestParam List<String> productName) {
        if (jwtValidate.validateTokenUserService(authorizationHeader)) {
            log.info("Received inventory check request for skuCode: {}", productName);
            return inventoryService.isInStock(productName);
        }
        return List.of(new InventoryResponse(null, false));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/validateToken")
    public String getOrderDetails(@RequestHeader(name = "Authorization") String authorizationHeader) {
        if (jwtValidate.validateTokenUserService(authorizationHeader)) {
            return inventoryService.getTokenUserService(authorizationHeader);
        } else {
            return "Unauthorized accessToken";
        }
    }

}
