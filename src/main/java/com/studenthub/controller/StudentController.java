package com.studenthub.controller;

import com.studenthub.dto.StudentDto;
import com.studenthub.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/{id}")
    public ResponseEntity<StudentDto> getStudentProfile(@PathVariable Long id) {
        StudentDto profile = studentService.getStudentById(id);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDto> updateStudentProfile(
            @PathVariable Long id,
            @Valid @RequestBody StudentDto studentDto) {
        StudentDto updated = studentService.updateStudentProfile(id, studentDto);
        return ResponseEntity.ok(updated);
    }
}
