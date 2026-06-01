package com.hotelos.roomservice.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestControllerAdvice @Slf4j
public class RsExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> validation(MethodArgumentNotValidException ex) {
        Map<String,String> e=new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(err->e.put(((FieldError)err).getField(),err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(e);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> bad(IllegalArgumentException e) { return ResponseEntity.badRequest().body(Map.of("error",e.getMessage())); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> conflict(IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error",e.getMessage())); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> gen(Exception e) { log.error("Unhandled",e); return ResponseEntity.internalServerError().body(Map.of("error","Internal error")); }
}
