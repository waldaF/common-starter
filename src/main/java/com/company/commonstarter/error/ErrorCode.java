package com.company.commonstarter.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus getStatus();
}