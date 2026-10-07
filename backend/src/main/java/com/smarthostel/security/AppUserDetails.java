package com.smarthostel.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in user, as Spring Security sees it.
 * A user is either a STUDENT (id = student_id) or a staff member
 * with role ADMIN, WARDEN or MAINTENANCE (id = staff_id).
 */
@Getter
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;   // registration number (student) or e-mail (staff)
    private final String password;   // BCrypt hash; null when built from a JWT
    private final String role;       // STUDENT, ADMIN, WARDEN, MAINTENANCE
    private final String name;

    public AppUserDetails(Long id, String username, String password, String role, String name) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.name = name;
    }

    public boolean isStudent() {
        return "STUDENT".equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Spring's hasRole('ADMIN') checks for the authority "ROLE_ADMIN"
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
