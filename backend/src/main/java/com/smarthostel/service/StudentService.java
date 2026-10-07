package com.smarthostel.service;

import com.smarthostel.dto.*;
import com.smarthostel.exception.BusinessRuleException;
import com.smarthostel.exception.ResourceNotFoundException;
import com.smarthostel.model.RoomAllocation;
import com.smarthostel.model.Student;
import com.smarthostel.repository.RoomAllocationRepository;
import com.smarthostel.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.smarthostel.service.AllocationService.blankToNull;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final RoomAllocationRepository allocationRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StudentResponse> search(String q, Integer year, String gender, Long hostelId) {
        List<Student> students = studentRepository.search(blankToNull(q), year, blankToNull(gender));
        Map<Long, RoomAllocation> rooms = activeRoomsOf(students);
        return students.stream()
                .map(s -> StudentResponse.from(s, rooms.get(s.getId())))
                .filter(s -> hostelId == null || hostelId.equals(s.hostelId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> waitingForRoom() {
        return studentRepository.findWaitingForRoom().stream().map(s -> StudentResponse.from(s, null)).toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(Long id) {
        Student s = findStudent(id);
        return StudentResponse.from(s, allocationRepository.findActiveByStudentId(id).orElse(null));
    }

    /** The student's own "My Room" page. */
    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long studentId) {
        Student s = findStudent(studentId);
        RoomAllocation active = allocationRepository.findActiveByStudentId(studentId).orElse(null);
        List<String> roommates = active == null ? List.of() :
                allocationRepository.findActiveByRoomId(active.getRoom().getId()).stream()
                        .filter(a -> !a.getStudent().getId().equals(studentId))
                        .map(a -> a.getStudent().getName() + " (bed " + a.getBedNumber() + ")")
                        .toList();
        List<AllocationResponse> history = allocationRepository.findHistoryByStudentId(studentId).stream()
                .map(AllocationResponse::from).toList();
        return new StudentProfileResponse(
                StudentResponse.from(s, active),
                active == null ? null : RoomResponse.from(active.getRoom()),
                active == null ? null : HostelResponse.from(active.getRoom().getHostel()),
                roommates, history);
    }

    @Transactional
    public StudentResponse updateOwnProfile(Long studentId, ProfileUpdateRequest request) {
        Student s = findStudent(studentId);
        s.setEmail(request.email().trim().toLowerCase());
        s.setPhone(request.phone());
        studentRepository.saveAndFlush(s);
        return getStudent(studentId);
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        if (request.password() == null || request.password().length() < 8) {
            throw new BusinessRuleException("An initial password of at least 8 characters is required");
        }
        Student s = new Student();
        copy(request, s);
        s.setPasswordHash(passwordEncoder.encode(request.password()));
        return StudentResponse.from(studentRepository.saveAndFlush(s), null);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student s = findStudent(id);
        copy(request, s);
        if (request.password() != null && !request.password().isBlank()) {
            s.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        studentRepository.saveAndFlush(s);
        return getStudent(id);
    }

    /** Fails with HTTP 409 (foreign key) if the student still has allocations, complaints, etc. */
    @Transactional
    public void deleteStudent(Long id) {
        studentRepository.delete(findStudent(id));
        studentRepository.flush();
    }

    Student findStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Student", id));
    }

    private Map<Long, RoomAllocation> activeRoomsOf(List<Student> students) {
        if (students.isEmpty()) return Map.of();
        List<Long> ids = students.stream().map(Student::getId).toList();
        return allocationRepository.findActiveByStudentIds(ids).stream()
                .collect(Collectors.toMap(a -> a.getStudent().getId(), Function.identity()));
    }

    private void copy(StudentRequest r, Student s) {
        s.setRegistrationNo(r.registrationNo().toUpperCase());
        s.setName(r.name().trim());
        s.setEmail(r.email().trim().toLowerCase());
        s.setPhone(r.phone());
        s.setGender(r.gender());
        s.setYear(r.year());
        s.setBranch(r.branch());
        s.setCgpa(r.cgpa());
    }
}
