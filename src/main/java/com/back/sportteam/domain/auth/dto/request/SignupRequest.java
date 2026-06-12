package com.back.sportteam.domain.auth.dto.request;

import com.back.sportteam.auth.domain.AuthProvider;
import com.back.sportteam.user.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수입니다.")
        String email,

        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하입니다.")
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(max = 100, message = "닉네임은 100자 이하입니다.")
        String nickname,

        @NotNull(message = "회원 역할은 필수입니다.")
        UserRole role,

        AuthProvider provider,
        String providerId
) {}