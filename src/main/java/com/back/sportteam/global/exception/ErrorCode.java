package com.back.sportteam.global.exception;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

public interface ErrorCode extends Serializable {
    String getCode();
    String getMessage();
    HttpStatus getStatus();
}
