package org.nicetu.spb.userservice.repository;

import org.nicetu.spb.userservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String name);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findById(@Param("id") Long id);

    @Query("SELECT CASE WHEN COUNT(u) > 0 " +
            "THEN true " +
            "ELSE false " +
            "END FROM User u WHERE u.email = :email")
    Boolean existsByEmail(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 " +
            "THEN true " +
            "ELSE false " +
            "END FROM User u WHERE u.phoneNumber = :phone")
    Boolean existsByPhoneNumber(@Param("phone") String phone);
}
