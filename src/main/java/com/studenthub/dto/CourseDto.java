package com.studenthub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    private Long courseId;

    @NotBlank(message = "Course name is required")
    private String courseName;

    private String description;

    @NotBlank(message = "Duration is required")
    private String duration;

    @NotBlank(message = "Instructor name is required")
    private String instructorName;

    private String content;

    private Long instituteId;
    private String instituteName;

    private LocalDateTime createdAt;
}
