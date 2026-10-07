package com.smarthostel.controller;

import com.smarthostel.dto.*;
import com.smarthostel.security.AppUserDetails;
import com.smarthostel.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ---------- the logged-in student ----------

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentProfileResponse myProfile(@AuthenticationPrincipal AppUserDetails user) {
        return studentService.getProfile(user.getId());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public StudentResponse updateMyProfile(@AuthenticationPrincipal AppUserDetails user,
                                           @Valid @RequestBody ProfileUpdateRequest request) {
        return studentService.updateOwnProfile(user.getId(), request);
    }

    // ---------- admin / warden ----------

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<StudentResponse> search(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) String gender,
                                        @RequestParam(required = false) Long hostelId) {
        return studentService.search(q, year, gender, hostelId);
    }

    @GetMapping("/waiting")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public List<StudentResponse> waitingForRoom() {
        return studentService.waitingForRoom();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public StudentResponse getOne(@PathVariable Long id) {
        return studentService.getStudent(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return studentService.createStudent(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','WARDEN')")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.updateStudent(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
    }
}
