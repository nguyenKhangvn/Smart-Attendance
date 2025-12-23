package com.dinhkhang.code.repository;

import com.dinhkhang.code.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByStudentCode(String studentCode);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByStudentCode(String studentCode);

    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true")
    List<User> findActiveUsersByRole(@Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND u.role = :role")
    List<User> searchUsersByKeywordAndRole(@Param("keyword") String keyword, @Param("role") User.Role role);
}
