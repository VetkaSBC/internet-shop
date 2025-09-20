package org.nicetu.spb.userservice.model.dto.response;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InformationMessage {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private Collection<? extends GrantedAuthority> roles;
}
