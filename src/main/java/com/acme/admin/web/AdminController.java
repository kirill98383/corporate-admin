package com.acme.admin.web;

import com.acme.admin.domain.Permission;
import com.acme.admin.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final UserService users;
    private final RoleService roles;
    private final AuditService audit;
    public AdminController(UserService users, RoleService roles, AuditService audit) { this.users=users; this.roles=roles; this.audit=audit; }
    @GetMapping("/") String dashboard() { return "dashboard"; }
    @GetMapping("/login") String login() { return "login"; }
    @GetMapping("/users") String users(Model model) { model.addAttribute("users", users.list()); return "users"; }
    @GetMapping("/users/new") String newUser(Model model) { model.addAttribute("roles", roles.list()); return "user-form"; }
    @GetMapping("/users/{id}/edit") String editUser(@PathVariable long id, Model model) {
        model.addAttribute("account", users.get(id)); model.addAttribute("roles", roles.list()); return "user-form";
    }
    @PostMapping("/users") String createUser(@Valid @ModelAttribute Commands.CreateUser command, RedirectAttributes flash) {
        users.create(command); flash.addFlashAttribute("success", "Пользователь создан"); return "redirect:/";
    }
    @PostMapping("/users/{id}") String updateUser(@PathVariable long id, @Valid @ModelAttribute Commands.UpdateUser command, RedirectAttributes flash) {
        users.update(id, command); flash.addFlashAttribute("success", "Пользователь обновлён"); return "redirect:/";
    }
    @PostMapping("/users/{id}/password") String password(@PathVariable long id, @Valid @ModelAttribute Commands.ResetPassword command, RedirectAttributes flash) {
        users.resetPassword(id, command); flash.addFlashAttribute("success", "Пароль изменён. Старые сессии будут завершены при следующем запросе."); return "redirect:/";
    }
    @GetMapping("/roles") String roles(Model model) { model.addAttribute("roles", roles.list()); return "roles"; }
    @GetMapping("/roles/new") String newRole(Model model) { model.addAttribute("permissions", Permission.values()); return "role-form"; }
    @GetMapping("/roles/{id}/edit") String editRole(@PathVariable long id, Model model) {
        model.addAttribute("role", roles.get(id)); model.addAttribute("permissions", Permission.values()); return "role-form";
    }
    @PostMapping("/roles") String createRole(@Valid @ModelAttribute Commands.SaveRole command, RedirectAttributes flash) {
        roles.create(command); flash.addFlashAttribute("success", "Роль создана"); return "redirect:/";
    }
    @PostMapping("/roles/{id}") String updateRole(@PathVariable long id, @Valid @ModelAttribute Commands.SaveRole command, RedirectAttributes flash) {
        roles.update(id, command); flash.addFlashAttribute("success", "Роль обновлена"); return "redirect:/";
    }
    @GetMapping("/audit") String audit(Model model) { model.addAttribute("events", audit.latest()); return "audit"; }
}
