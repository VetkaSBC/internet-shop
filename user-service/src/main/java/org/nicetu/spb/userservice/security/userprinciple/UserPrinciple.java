package org.nicetu.spb.userservice.security.userprinciple;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import org.nicetu.spb.userservice.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@With
@Builder
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserPrinciple implements UserDetails {

    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    @JsonIgnore
    private String password;
    private String phoneNumber;
    private Collection<? extends GrantedAuthority> roles;

    public static UserPrinciple build(User user) {
        List<GrantedAuthority> authorityList = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return UserPrinciple.builder()
                .id(user.getId())
                .firstname(user.getFirstName())
                .lastname(user.getLastName())
                .email(user.getEmail())
                .password(user.getPassword())
                .phoneNumber(user.getPhoneNumber())
                .roles(authorityList)
                .build();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    public Long getId() {
        return id;
    }

    public String getFirstname(){
        return firstname;
    }

    public String getLastname(){
        return lastname;
    }


    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
