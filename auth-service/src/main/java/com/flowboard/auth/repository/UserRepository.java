package com.flowboard.auth.repository;

import com.flowboard.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    List<User> findAllByRole(String role);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<User> searchByFullName(@Param("q") String query);
}