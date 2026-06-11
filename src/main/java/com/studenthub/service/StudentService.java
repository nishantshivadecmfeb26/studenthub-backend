package com.studenthub.service;

import com.studenthub.dto.StudentDto;
import com.studenthub.entity.Student;
import com.studenthub.entity.User;
import com.studenthub.entity.UserRole;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.repository.EnrollmentRepository;
import com.studenthub.repository.StudentRepository;
import com.studenthub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserService userService;

    public List<StudentDto> getAllStudents() {
        User institute = getCurrentInstituteUser();
        return studentRepository.findByInstitute(institute).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public StudentDto getStudentById(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        
        User currentUser = userService.getCurrentlyAuthenticatedUser();
        if (currentUser.getRole() == UserRole.ROLE_ADMIN) {
            ensureSameInstitute(student);
        } else if (!student.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Student not found with ID: " + studentId);
        }
        
        return convertToDto(student);
    }

    public StudentDto getStudentByEmail(String email) {
        Student student = studentRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with email: " + email));
        return convertToDto(student);
    }

    public List<StudentDto> searchStudents(String query) {
        User institute = getCurrentInstituteUser();
        return studentRepository.findByInstituteAndUserFullNameContainingIgnoreCaseOrInstituteAndDepartmentContainingIgnoreCase(
                        institute, query, institute, query)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public StudentDto updateStudentProfile(Long studentId, StudentDto dto) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        User currentUser = userService.getCurrentlyAuthenticatedUser();
        if (currentUser.getRole() == UserRole.ROLE_ADMIN) {
            ensureSameInstitute(student);
        } else if (!student.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Student not found with ID: " + studentId);
        }

        User user = student.getUser();
        user.setFullName(dto.getFullName());
        userRepository.save(user);

        student.setPhone(dto.getPhone());
        student.setDepartment(dto.getDepartment());

        Student updatedStudent = studentRepository.save(student);
        return convertToDto(updatedStudent);
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + studentId));
        ensureSameInstitute(student);
        
        // 1. Delete associated enrollments first
        enrollmentRepository.deleteByStudentStudentId(studentId);
        
        // 2. Delete Student directly (which cascades to User since Student owns the OneToOne association with CascadeType.ALL)
        studentRepository.delete(student);
    }

    private StudentDto convertToDto(Student student) {
        return StudentDto.builder()
                .studentId(student.getStudentId())
                .userId(student.getUser().getId())
                .fullName(student.getUser().getFullName())
                .email(student.getUser().getEmail())
                .phone(student.getPhone())
                .department(student.getDepartment())
                .instituteId(student.getInstitute() != null ? student.getInstitute().getId() : null)
                .instituteName(student.getInstitute() != null ? student.getInstitute().getFullName() : null)
                .build();
    }

    private User getCurrentInstituteUser() {
        User user = userService.getCurrentlyAuthenticatedUser();
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Only institutes can access student management");
        }
        return user;
    }

    private void ensureSameInstitute(Student student) {
        User institute = getCurrentInstituteUser();
        if (student.getInstitute() == null || !student.getInstitute().getId().equals(institute.getId())) {
            throw new ResourceNotFoundException("Student not found with ID: " + student.getStudentId());
        }
    }
}
