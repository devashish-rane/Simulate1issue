package com.deva.learn.exception;

import java.util.Collection;
import java.util.Map;

import org.springframework.http.HttpStatus;

public class UnsupportedPlanException extends ApiException {
    public UnsupportedPlanException(String plan, Collection<String> allowedPlans) {
        super(
                HttpStatus.BAD_REQUEST,
                "UNSUPPORTED_PLAN",
                "Unsupported plan: " + plan,
                Map.of(
                        "plan", plan,
                        "allowedPlans", allowedPlans));
    }
}
