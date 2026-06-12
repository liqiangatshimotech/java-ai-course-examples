package com.example.codingagent.web;

import jakarta.validation.constraints.NotBlank;

public record ApproveCodingTaskRequest(
        @NotBlank(message = "approver 不能为空")
        String approver,

        String comment
) {
}
