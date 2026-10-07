package com.smarthostel.security;

import com.smarthostel.model.Staff;
import com.smarthostel.model.Student;
import com.smarthostel.repository.StaffRepository;
import com.smarthostel.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a user from Oracle during login.
 * Staff log in with their e-mail; students log in with their registration number.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username.contains("@")) {
            Staff staff = staffRepository.findByEmailIgnoreCase(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
            return new AppUserDetails(staff.getId(), staff.getEmail(), staff.getPasswordHash(),
                    staff.getRole().toUpperCase(), staff.getName());
        }
        Student student = studentRepository.findByRegistrationNoIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return new AppUserDetails(student.getId(), student.getRegistrationNo(), student.getPasswordHash(),
                "STUDENT", student.getName());
    }
}
