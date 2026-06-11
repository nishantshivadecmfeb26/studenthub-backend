package com.studenthub.service;

import com.studenthub.dto.CourseDto;
import com.studenthub.entity.Course;
import com.studenthub.entity.Student;
import com.studenthub.entity.User;
import com.studenthub.entity.UserRole;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.repository.CourseRepository;
import com.studenthub.repository.EnrollmentRepository;
import com.studenthub.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserService userService;

    public Page<CourseDto> getCourses(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage;
        User institute = getCurrentInstitute();

        if (StringUtils.hasText(search)) {
            coursePage = courseRepository.findByInstituteAndCourseNameContainingIgnoreCaseOrInstituteAndInstructorNameContainingIgnoreCaseOrInstituteAndDescriptionContainingIgnoreCase(
                    institute, search, institute, search, institute, search, pageable);
        } else {
            coursePage = courseRepository.findByInstitute(institute, pageable);
        }

        return coursePage.map(this::convertToDto);
    }

    public CourseDto getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));
        ensureCourseBelongsToCurrentInstitute(course);
        return convertToDto(course);
    }

    @Transactional
    public CourseDto createCourse(CourseDto dto) {
        User institute = userService.getCurrentlyAuthenticatedUser();
        if (institute.getRole() != UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Only institutes can create courses");
        }

        Course course = Course.builder()
                .courseName(dto.getCourseName())
                .description(dto.getDescription())
                .duration(dto.getDuration())
                .instructorName(dto.getInstructorName())
                .content(dto.getContent())
                .institute(institute)
                .build();

        Course savedCourse = courseRepository.save(course);
        return convertToDto(savedCourse);
    }

    @Transactional
    public CourseDto updateCourse(Long courseId, CourseDto dto) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));
        ensureCourseBelongsToCurrentInstitute(course);

        course.setCourseName(dto.getCourseName());
        course.setDescription(dto.getDescription());
        course.setDuration(dto.getDuration());
        course.setInstructorName(dto.getInstructorName());
        course.setContent(dto.getContent());

        Course updatedCourse = courseRepository.save(course);
        return convertToDto(updatedCourse);
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + courseId));
        ensureCourseBelongsToCurrentInstitute(course);

        // 1. Delete associated enrollments first
        enrollmentRepository.deleteByCourseCourseId(courseId);
        
        // 2. Delete the Course
        courseRepository.deleteById(courseId);
    }

    private CourseDto convertToDto(Course course) {
        return CourseDto.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .duration(course.getDuration())
                .instructorName(course.getInstructorName())
                .content(course.getContent())
                .instituteId(course.getInstitute() != null ? course.getInstitute().getId() : null)
                .instituteName(course.getInstitute() != null ? course.getInstitute().getFullName() : null)
                .createdAt(course.getCreatedAt())
                .build();
    }

    private User getCurrentInstitute() {
        User user = userService.getCurrentlyAuthenticatedUser();
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return user;
        }

        Student student = studentRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        if (student.getInstitute() == null) {
            throw new BadRequestException("Student is not linked to an institute");
        }
        return student.getInstitute();
    }

    private void ensureCourseBelongsToCurrentInstitute(Course course) {
        User institute = getCurrentInstitute();
        if (course.getInstitute() == null || !course.getInstitute().getId().equals(institute.getId())) {
            throw new ResourceNotFoundException("Course not found with ID: " + course.getCourseId());
        }
    }
}
