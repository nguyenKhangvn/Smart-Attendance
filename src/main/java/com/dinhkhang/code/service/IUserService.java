package com.dinhkhang.code.service;

import com.dinhkhang.code.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

public interface IUserService extends UserDetailsService {
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;

    User createUser(User user);

    User updateUser(Long id, User updatedUser);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

    List<User> getAllUsers();

    List<User> getUsersByRole(User.Role role);

    Page<User> getUsersByRole(User.Role role, Pageable pageable);

    List<User> searchStudents(String keyword);

    void deleteUser(Long id);

    void deactivateUser(Long id);

    void activateUser(Long id);
}
