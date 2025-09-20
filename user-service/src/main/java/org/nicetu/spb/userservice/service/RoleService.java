package org.nicetu.spb.userservice.service;

import org.nicetu.spb.userservice.model.entity.Role;
import org.nicetu.spb.userservice.model.entity.RoleName;

import java.util.List;
import java.util.Optional;

public interface RoleService {
    Optional<Role> findByName(RoleName name);
    boolean assignRole(Long id, String roleName);
    boolean revokeRole(Long id, String roleName);
    List<String> getUserRoles(Long id);
}
