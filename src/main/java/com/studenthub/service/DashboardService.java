package com.studenthub.service;

import com.studenthub.dto.DashboardStatsDto;
import com.studenthub.entity.User;
import com.studenthub.entity.UserRole;
import com.studenthub.exception.BadRequestException;
import com.studenthub.repository.CourseRepository;
import com.studenthub.repository.EnrollmentRepository;
import com.studenthub.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserService userService;

    public DashboardStatsDto getAdminStats() {
        User institute = userService.getCurrentlyAuthenticatedUser();
        if (institute.getRole() != UserRole.ROLE_ADMIN) {
            throw new BadRequestException("Only institutes can view dashboard stats");
        }

        long totalStudents = studentRepository.countByInstitute(institute);
        long totalCourses = courseRepository.countByInstitute(institute);
        long totalEnrollments = enrollmentRepository.countByCourseInstituteId(institute.getId());
        long activeEnrollments = enrollmentRepository.countByCourseInstituteIdAndStatus(institute.getId(), "ENROLLED");
        long completedEnrollments = enrollmentRepository.countByCourseInstituteIdAndStatus(institute.getId(), "COMPLETED");

        return DashboardStatsDto.builder()
                .totalStudents(totalStudents)
                .totalCourses(totalCourses)
                .totalEnrollments(totalEnrollments)
                .activeEnrollments(activeEnrollments)
                .completedEnrollments(completedEnrollments)
                .build();
    }
}
