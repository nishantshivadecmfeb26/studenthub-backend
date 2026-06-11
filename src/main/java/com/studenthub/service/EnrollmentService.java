package com.studenthub.service;

import com.studenthub.dto.EnrollmentDto;
import com.studenthub.entity.Course;
import com.studenthub.entity.Enrollment;
import com.studenthub.entity.Student;
import com.studenthub.entity.User;
import com.studenthub.entity.UserRole;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.repository.CourseRepository;
import com.studenthub.repository.EnrollmentRepository;
import com.studenthub.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserService userService;

    @Transactional
    public EnrollmentDto enrollStudent(Long studentId, Long courseId) {
        User currentUser = userService.getCurrentlyAuthenticatedUser();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));

        if (currentUser.getRole() != UserRole.ROLE_STUDENT || !student.getUser().getId().equals(currentUser.getId())) {
            throw new BadRequestException("Students can only enroll themselves");
        }

        if (student.getInstitute() == null || course.getInstitute() == null
                || !student.getInstitute().getId().equals(course.getInstitute().getId())) {
            throw new BadRequestException("You can only enroll in your institute's courses");
        }

        if (enrollmentRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId)) {
            throw new BadRequestException("Student is already enrolled in this course!");
        }

        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .course(course)
                .status("ENROLLED")
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        return convertToDto(savedEnrollment);
    }

    public List<EnrollmentDto> getEnrollmentsByStudent(Long studentId) {
        User currentUser = userService.getCurrentlyAuthenticatedUser();
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));

        if (currentUser.getRole() == UserRole.ROLE_STUDENT && !student.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Student not found with ID: " + studentId);
        }

        if (currentUser.getRole() == UserRole.ROLE_ADMIN
                && (student.getInstitute() == null || !student.getInstitute().getId().equals(currentUser.getId()))) {
            throw new ResourceNotFoundException("Student not found with ID: " + studentId);
        }

        return enrollmentRepository.findByStudentStudentId(studentId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<EnrollmentDto> getAllEnrollments() {
        User institute = userService.getCurrentlyAuthenticatedUser();
        if (institute.getRole() != UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Only institutes can view all enrollments");
        }

        return enrollmentRepository.findByCourseInstituteId(institute.getId()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public EnrollmentDto updateEnrollmentStatus(Long enrollmentId, String status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId));
        ensureEnrollmentVisibleToCurrentUser(enrollment);

        if (!status.equals("ENROLLED") && !status.equals("COMPLETED") && !status.equals("DROPPED")) {
            throw new BadRequestException("Invalid status value: " + status);
        }

        enrollment.setStatus(status);
        Enrollment updatedEnrollment = enrollmentRepository.save(enrollment);
        return convertToDto(updatedEnrollment);
    }

    @Transactional
    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with ID: " + enrollmentId));
        ensureEnrollmentVisibleToCurrentUser(enrollment);
        enrollmentRepository.delete(enrollment);
    }

    private EnrollmentDto convertToDto(Enrollment enrollment) {
        return EnrollmentDto.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .studentId(enrollment.getStudent().getStudentId())
                .studentName(enrollment.getStudent().getUser().getFullName())
                .courseId(enrollment.getCourse().getCourseId())
                .courseName(enrollment.getCourse().getCourseName())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .status(enrollment.getStatus())
                .build();
    }

    private void ensureEnrollmentVisibleToCurrentUser(Enrollment enrollment) {
        User currentUser = userService.getCurrentlyAuthenticatedUser();
        if (currentUser.getRole() == UserRole.ROLE_STUDENT
                && enrollment.getStudent().getUser().getId().equals(currentUser.getId())) {
            return;
        }

        if (currentUser.getRole() == UserRole.ROLE_ADMIN
                && enrollment.getCourse().getInstitute() != null
                && enrollment.getCourse().getInstitute().getId().equals(currentUser.getId())) {
            return;
        }

        throw new ResourceNotFoundException("Enrollment not found with ID: " + enrollment.getEnrollmentId());
    }
}
