package org.nicetu.spb.userservice.model.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Login {

    private String email;

    private String password;
}
