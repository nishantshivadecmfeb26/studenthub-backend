package com.studenthub.repository;

import com.studenthub.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentStudentId(Long studentId);
    List<Enrollment> findByCourseInstituteId(Long instituteId);
    Optional<Enrollment> findByStudentStudentIdAndCourseCourseId(Long studentId, Long courseId);
    boolean existsByStudentStudentIdAndCourseCourseId(Long studentId, Long courseId);
    long countByStatus(String status);
    long countByCourseInstituteId(Long instituteId);
    long countByCourseInstituteIdAndStatus(Long instituteId, String status);
    void deleteByStudentStudentId(Long studentId);
    void deleteByCourseCourseId(Long courseId);
}
