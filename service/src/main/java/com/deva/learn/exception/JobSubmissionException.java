package com.deva.learn.exception;

import org.springframework.http.HttpStatus;

public class JobSubmissionException extends ApiException {
    public JobSubmissionException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE,
                "JOB_SUBMISSION_FAILED",
                "Could not submit job");
        initCause(cause);
    }
}
