package com.studenthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDto {
    private Long studentId;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String department;
    private Long instituteId;
    private String instituteName;
}
