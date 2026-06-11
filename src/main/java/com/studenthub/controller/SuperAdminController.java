package com.studenthub.controller;

import com.studenthub.dto.RegisterRequest;
import com.studenthub.dto.CourseDto;
import com.studenthub.dto.StudentDto;
import com.studenthub.dto.EnrollmentDto;
import com.studenthub.entity.User;
import com.studenthub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/institutes")
    public ResponseEntity<List<User>> getAllInstitutes() {
        return ResponseEntity.ok(userService.getAllInstitutes());
    }

    @PostMapping("/institutes")
    public ResponseEntity<User> createInstitute(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.registerInstitute(request));
    }

    @PutMapping("/institutes/{id}")
    public ResponseEntity<User> updateInstitute(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(userService.updateInstitute(id, payload.get("fullName"), payload.get("email")));
    }

    @DeleteMapping("/institutes/{id}")
    public ResponseEntity<Void> deleteInstitute(@PathVariable Long id) {
        userService.deleteInstitute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(userService.getSuperAdminStats());
    }

    // Scoped Course Operations for Super Admin
    @GetMapping("/institutes/{instId}/courses")
    public ResponseEntity<Page<CourseDto>> getInstituteCourses(
            @PathVariable Long instId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getInstituteCourses(instId, page, size, search));
    }

    @PostMapping("/institutes/{instId}/courses")
    public ResponseEntity<CourseDto> createInstituteCourse(
            @PathVariable Long instId,
            @RequestBody CourseDto dto) {
        return ResponseEntity.ok(userService.createInstituteCourse(instId, dto));
    }

    @PutMapping("/institutes/{instId}/courses/{courseId}")
    public ResponseEntity<CourseDto> updateInstituteCourse(
            @PathVariable Long instId,
            @PathVariable Long courseId,
            @RequestBody CourseDto dto) {
        return ResponseEntity.ok(userService.updateInstituteCourse(instId, courseId, dto));
    }

    @DeleteMapping("/institutes/{instId}/courses/{courseId}")
    public ResponseEntity<Void> deleteInstituteCourse(
            @PathVariable Long instId,
            @PathVariable Long courseId) {
        userService.deleteInstituteCourse(instId, courseId);
        return ResponseEntity.noContent().build();
    }

    // Scoped Student Operations for Super Admin
    @GetMapping("/institutes/{instId}/students")
    public ResponseEntity<List<StudentDto>> getInstituteStudents(
            @PathVariable Long instId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(userService.getInstituteStudents(instId, search));
    }

    @DeleteMapping("/institutes/{instId}/students/{studentId}")
    public ResponseEntity<Void> deleteInstituteStudent(
            @PathVariable Long instId,
            @PathVariable Long studentId) {
        userService.deleteInstituteStudent(instId, studentId);
        return ResponseEntity.noContent().build();
    }

    // Scoped Enrollment Operations for Super Admin
    @GetMapping("/institutes/{instId}/enrollments")
    public ResponseEntity<List<EnrollmentDto>> getInstituteEnrollments(
            @PathVariable Long instId) {
        return ResponseEntity.ok(userService.getInstituteEnrollments(instId));
    }

    @PutMapping("/institutes/{instId}/enrollments/{enrollmentId}/status")
    public ResponseEntity<Void> updateInstituteEnrollmentStatus(
            @PathVariable Long instId,
            @PathVariable Long enrollmentId,
            @RequestParam String status) {
        userService.updateInstituteEnrollmentStatus(instId, enrollmentId, status);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/institutes/{instId}/enrollments/{enrollmentId}")
    public ResponseEntity<Void> cancelInstituteEnrollment(
            @PathVariable Long instId,
            @PathVariable Long enrollmentId) {
        userService.cancelInstituteEnrollment(instId, enrollmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@RequestBody Map<String, String> payload) {
        User current = userService.getCurrentlyAuthenticatedUser();
        return ResponseEntity.ok(userService.updateSuperAdminProfile(
                current.getId(),
                payload.get("fullName"),
                payload.get("email"),
                payload.get("password")
        ));
    }
}

