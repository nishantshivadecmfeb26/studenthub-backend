package com.studenthub.repository;

import com.studenthub.entity.Course;
import com.studenthub.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    java.util.List<Course> findByInstitute(User institute);
    Page<Course> findByInstitute(User institute, Pageable pageable);
    Page<Course> findByInstituteAndCourseNameContainingIgnoreCaseOrInstituteAndInstructorNameContainingIgnoreCaseOrInstituteAndDescriptionContainingIgnoreCase(
            User courseInstitute, String courseName, User instructorInstitute, String instructorName, User descriptionInstitute, String description, Pageable pageable);

    Page<Course> findByCourseNameContainingIgnoreCaseOrInstructorNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String courseName, String instructorName, String description, Pageable pageable);
    long countByInstitute(User institute);
}
