package com.studenthub.controller;

import com.studenthub.dto.AuthRequest;
import com.studenthub.dto.AuthResponse;
import com.studenthub.dto.InstituteDto;
import com.studenthub.dto.RegisterRequest;
import com.studenthub.entity.Student;
import com.studenthub.entity.User;
import com.studenthub.entity.UserRole;
import com.studenthub.service.StudentService;
import com.studenthub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest registerRequest) {
        userService.registerUser(registerRequest);
        return new ResponseEntity<>("Account registered successfully!", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse response = userService.authenticateUser(authRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        User user = userService.getCurrentlyAuthenticatedUser();
        if (user.getRole() == UserRole.ROLE_STUDENT) {
            return ResponseEntity.ok(studentService.getStudentByEmail(user.getEmail()));
        }
        
        // Return user details for
        return ResponseEntity.ok(user);
    }

    @GetMapping("/institutes")
    public ResponseEntity<List<InstituteDto>> getInstitutes() {
        return ResponseEntity.ok(userService.getInstitutes());
    }
}
