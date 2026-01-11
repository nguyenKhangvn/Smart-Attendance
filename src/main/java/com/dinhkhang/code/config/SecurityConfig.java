package com.dinhkhang.code.config;

import com.dinhkhang.code.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Autowired
        private IUserService userService;

        @Autowired
        private CustomAuthenticationSuccessHandler successHandler;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
                authProvider.setUserDetailsService(userService);
                authProvider.setPasswordEncoder(passwordEncoder());
                return authProvider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                // Public resources
                                                .requestMatchers("/resources/**", "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/", "/login", "/register").permitAll()

                                                // API endpoints (for mobile/ajax)
                                                .requestMatchers("/api/attendance/checkin").hasRole("STUDENT")
                                                .requestMatchers("/api/qr/**").hasRole("TEACHER")

                                                // Teacher pages
                                                .requestMatchers("/teacher/**").hasRole("TEACHER")

                                                // Student pages
                                                .requestMatchers("/student/**").hasRole("STUDENT")

                                                // Admin pages
                                                .requestMatchers("/admin/**").hasRole("ADMIN")

                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .successHandler(successHandler)
                                                .failureUrl("/login?error=true")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                .accessDeniedPage("/access-denied"))
                                .csrf(csrf -> csrf.disable()); // Disable CSRF for all endpoints

                return http.build();
        }
}
