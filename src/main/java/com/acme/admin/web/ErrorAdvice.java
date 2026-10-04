package com.acme.admin.web;

import com.acme.admin.service.DomainException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class ErrorAdvice {
    @ExceptionHandler(DomainException.class) @ResponseStatus(HttpStatus.CONFLICT)
    String domain(DomainException ex, Model model) { model.addAttribute("message", ex.getMessage()); return "error"; }
    @ExceptionHandler({BindException.class, ConstraintViolationException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalid(Exception ex, Model model) {
        model.addAttribute("message", "Проверьте поля: логин — 3–64 символа a-z, 0-9, . _ -; имя — до 100 символов; пароль — 12–64 символа и не более 72 байт UTF-8. Выберите хотя бы одну роль или право."); return "error";
    }
    @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT)
    String conflict(Model model) { model.addAttribute("message", "Такое имя уже занято или связанные данные изменились. Обновите страницу."); return "error"; }
}
