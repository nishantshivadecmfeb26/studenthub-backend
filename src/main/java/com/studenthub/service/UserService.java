package com.studenthub.service;

import com.studenthub.dto.AuthRequest;
import com.studenthub.dto.AuthResponse;
import com.studenthub.dto.InstituteDto;
import com.studenthub.dto.RegisterRequest;
import com.studenthub.entity.Student;
import com.studenthub.entity.User;
import com.studenthub.entity.UserRole;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.repository.StudentRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.repository.CourseRepository;
import com.studenthub.repository.EnrollmentRepository;
import com.studenthub.entity.Course;
import com.studenthub.entity.Enrollment;
import com.studenthub.dto.CourseDto;
import com.studenthub.dto.StudentDto;
import com.studenthub.dto.EnrollmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Map;
import java.util.HashMap;
import com.studenthub.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered!");
        }

        if ("INSTITUTE".equalsIgnoreCase(request.getAccountType())) {
            return registerInstitute(request);
        }

        if ("STUDENT".equalsIgnoreCase(request.getAccountType())) {
            registerStudent(request);
            return null;
        }

        throw new BadRequestException("Invalid account type");
    }

    @Transactional
    public User registerInstitute(RegisterRequest request) {
        String instituteName = StringUtils.hasText(request.getInstituteName())
                ? request.getInstituteName()
                : request.getFullName();

        User user = User.builder()
                .fullName(instituteName)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_ADMIN)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public Student registerStudent(RegisterRequest request) {
        if (request.getInstituteId() == null) {
            throw new BadRequestException("Please select an institute");
        }

        User institute = userRepository.findById(request.getInstituteId())
                .filter(user -> user.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        if (!StringUtils.hasText(request.getDepartment())) {
            throw new BadRequestException("Department is required");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ROLE_STUDENT)
                .build();

        user = userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .institute(institute)
                .phone(request.getPhone())
                .department(request.getDepartment())
                .build();

        return studentRepository.save(student);
    }

    public AuthResponse authenticateUser(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Long studentId = null;
        Long instituteId = null;
        String instituteName = null;

        if (user.getRole() == UserRole.ROLE_STUDENT) {
            Student student = studentRepository.findByUser(user)
                    .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
            studentId = student.getStudentId();
            if (student.getInstitute() != null) {
                instituteId = student.getInstitute().getId();
                instituteName = student.getInstitute().getFullName();
            }
        } else if (user.getRole() == UserRole.ROLE_ADMIN) {
            instituteId = user.getId();
            instituteName = user.getFullName();
        }

        return AuthResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .studentId(studentId)
                .instituteId(instituteId)
                .instituteName(instituteName)
                .build();
    }

    public List<InstituteDto> getInstitutes() {
        return userRepository.findByRoleOrderByFullNameAsc(UserRole.ROLE_ADMIN).stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase("admin@studenthub.com") && !user.getEmail().equalsIgnoreCase("superadmin@studenthub.com"))
                .map(user -> InstituteDto.builder()
                        .id(user.getId())
                        .name(user.getFullName())
                        .email(user.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    public User getCurrentlyAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadRequestException("No user is currently authenticated");
        }
        
        String email;
        if (authentication.getPrincipal() instanceof UserDetails) {
            email = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getPrincipal().toString();
        }
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public List<User> getAllInstitutes() {
        return userRepository.findByRoleOrderByFullNameAsc(UserRole.ROLE_ADMIN).stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase("admin@studenthub.com") && !user.getEmail().equalsIgnoreCase("superadmin@studenthub.com"))
                .collect(Collectors.toList());
    }

    @Transactional
    public User updateInstitute(Long id, String fullName, String email) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered!");
        }

        user.setFullName(fullName);
        user.setEmail(email);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteInstitute(Long instituteId) {
        User institute = userRepository.findById(instituteId)
                .filter(user -> user.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        // 1. Delete all enrollments of students belonging to this institute
        List<Student> students = studentRepository.findByInstitute(institute);
        for (Student student : students) {
            enrollmentRepository.deleteByStudentStudentId(student.getStudentId());
        }

        // 2. Delete all students of this institute
        studentRepository.deleteAll(students);

        // 3. Delete all enrollments of courses belonging to this institute
        List<Course> courses = courseRepository.findByInstitute(institute);
        for (Course course : courses) {
            enrollmentRepository.deleteByCourseCourseId(course.getCourseId());
            courseRepository.delete(course);
        }

        // 4. Delete the institute admin user itself
        userRepository.delete(institute);
    }

    public Map<String, Object> getSuperAdminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalInstitutes", getAllInstitutes().size());
        stats.put("totalStudents", studentRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("totalEnrollments", enrollmentRepository.count());
        return stats;
    }

    @Transactional
    public User updateSuperAdminProfile(Long id, String fullName, String email, String password) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == UserRole.ROLE_SUPER_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Super Admin not found"));

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered!");
        }

        user.setFullName(fullName);
        user.setEmail(email);
        if (StringUtils.hasText(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }
        return userRepository.save(user);
    }

    public Page<CourseDto> getInstituteCourses(Long instId, int page, int size, String search) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage;
        if (StringUtils.hasText(search)) {
            coursePage = courseRepository.findByInstituteAndCourseNameContainingIgnoreCaseOrInstituteAndInstructorNameContainingIgnoreCaseOrInstituteAndDescriptionContainingIgnoreCase(
                    institute, search, institute, search, institute, search, pageable);
        } else {
            coursePage = courseRepository.findByInstitute(institute, pageable);
        }
        return coursePage.map(course -> CourseDto.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .duration(course.getDuration())
                .instructorName(course.getInstructorName())
                .content(course.getContent())
                .instituteId(course.getInstitute().getId())
                .instituteName(course.getInstitute().getFullName())
                .createdAt(course.getCreatedAt())
                .build());
    }

    @Transactional
    public CourseDto createInstituteCourse(Long instId, CourseDto dto) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Course course = Course.builder()
                .courseName(dto.getCourseName())
                .description(dto.getDescription())
                .duration(dto.getDuration())
                .instructorName(dto.getInstructorName())
                .content(dto.getContent())
                .institute(institute)
                .build();

        Course savedCourse = courseRepository.save(course);
        return CourseDto.builder()
                .courseId(savedCourse.getCourseId())
                .courseName(savedCourse.getCourseName())
                .description(savedCourse.getDescription())
                .duration(savedCourse.getDuration())
                .instructorName(savedCourse.getInstructorName())
                .content(savedCourse.getContent())
                .instituteId(savedCourse.getInstitute().getId())
                .instituteName(savedCourse.getInstitute().getFullName())
                .createdAt(savedCourse.getCreatedAt())
                .build();
    }

    @Transactional
    public CourseDto updateInstituteCourse(Long instId, Long courseId, CourseDto dto) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Course course = courseRepository.findById(courseId)
                .filter(c -> c.getInstitute().getId().equals(institute.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        course.setCourseName(dto.getCourseName());
        course.setDescription(dto.getDescription());
        course.setDuration(dto.getDuration());
        course.setInstructorName(dto.getInstructorName());
        course.setContent(dto.getContent());

        Course updatedCourse = courseRepository.save(course);
        return CourseDto.builder()
                .courseId(updatedCourse.getCourseId())
                .courseName(updatedCourse.getCourseName())
                .description(updatedCourse.getDescription())
                .duration(updatedCourse.getDuration())
                .instructorName(updatedCourse.getInstructorName())
                .content(updatedCourse.getContent())
                .instituteId(updatedCourse.getInstitute().getId())
                .instituteName(updatedCourse.getInstitute().getFullName())
                .createdAt(updatedCourse.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteInstituteCourse(Long instId, Long courseId) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Course course = courseRepository.findById(courseId)
                .filter(c -> c.getInstitute().getId().equals(institute.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        enrollmentRepository.deleteByCourseCourseId(courseId);
        courseRepository.delete(course);
    }

    public List<StudentDto> getInstituteStudents(Long instId, String search) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        List<Student> students;
        if (StringUtils.hasText(search)) {
            students = studentRepository.findByInstituteAndUserFullNameContainingIgnoreCaseOrInstituteAndDepartmentContainingIgnoreCase(
                    institute, search, institute, search);
        } else {
            students = studentRepository.findByInstitute(institute);
        }
        return students.stream()
                .map(student -> StudentDto.builder()
                        .studentId(student.getStudentId())
                        .userId(student.getUser().getId())
                        .fullName(student.getUser().getFullName())
                        .email(student.getUser().getEmail())
                        .phone(student.getPhone())
                        .department(student.getDepartment())
                        .instituteId(student.getInstitute().getId())
                        .instituteName(student.getInstitute().getFullName())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteInstituteStudent(Long instId, Long studentId) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Student student = studentRepository.findById(studentId)
                .filter(s -> s.getInstitute().getId().equals(institute.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        enrollmentRepository.deleteByStudentStudentId(studentId);
        studentRepository.delete(student);
    }

    public List<EnrollmentDto> getInstituteEnrollments(Long instId) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        return enrollmentRepository.findByCourseInstituteId(instId).stream()
                .map(enroll -> EnrollmentDto.builder()
                        .enrollmentId(enroll.getEnrollmentId())
                        .studentId(enroll.getStudent().getStudentId())
                        .studentName(enroll.getStudent().getUser().getFullName())
                        .courseId(enroll.getCourse().getCourseId())
                        .courseName(enroll.getCourse().getCourseName())
                        .enrollmentDate(enroll.getEnrollmentDate())
                        .status(enroll.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateInstituteEnrollmentStatus(Long instId, Long enrollmentId, String status) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Enrollment enroll = enrollmentRepository.findById(enrollmentId)
                .filter(e -> e.getCourse().getInstitute().getId().equals(institute.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        enroll.setStatus(status);
        enrollmentRepository.save(enroll);
    }

    @Transactional
    public void cancelInstituteEnrollment(Long instId, Long enrollmentId) {
        User institute = userRepository.findById(instId)
                .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
                .orElseThrow(() -> new ResourceNotFoundException("Institute not found"));

        Enrollment enroll = enrollmentRepository.findById(enrollmentId)
                .filter(e -> e.getCourse().getInstitute().getId().equals(institute.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        enrollmentRepository.delete(enroll);
    }
}
