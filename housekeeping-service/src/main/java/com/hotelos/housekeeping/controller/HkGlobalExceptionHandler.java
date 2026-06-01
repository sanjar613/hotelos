package com.hotelos.housekeeping.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice @Slf4j
public class HkGlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> bad(IllegalArgumentException e) {
        log.warn(e.getMessage()); return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> conflict(IllegalStateException e) {
        log.warn(e.getMessage()); return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> generic(Exception e) {
        log.error("Unhandled",e); return ResponseEntity.internalServerError().body(Map.of("error","Internal error"));
    }
}
