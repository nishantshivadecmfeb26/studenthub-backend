package com.studenthub.repository;

import com.studenthub.entity.Student;
import com.studenthub.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUser(User user);
    Optional<Student> findByUserEmail(String email);
    List<Student> findByInstitute(User institute);
    List<Student> findByInstituteAndUserFullNameContainingIgnoreCaseOrInstituteAndDepartmentContainingIgnoreCase(
            User nameInstitute, String fullName, User departmentInstitute, String department);
    long countByInstitute(User institute);
    List<Student> findByUserFullNameContainingIgnoreCaseOrDepartmentContainingIgnoreCase(String fullName, String department);
}
