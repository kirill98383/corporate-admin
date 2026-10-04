package com.acme.admin;

import com.acme.admin.domain.*;
import com.acme.admin.repository.*;
import com.acme.admin.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:tests;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "app.bootstrap.password=Test-Admin-Pass-123"})
@AutoConfigureMockMvc
class AdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserService users;
    @Autowired RoleService roles;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    long adminRole;
    long auditorRole;
    @BeforeEach void reset() {
        jdbc.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM app_users WHERE username <> 'admin')");
        jdbc.update("DELETE FROM app_users WHERE username <> 'admin'");
        jdbc.update("DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE name NOT IN ('ADMIN','AUDITOR'))");
        jdbc.update("DELETE FROM roles WHERE name NOT IN ('ADMIN','AUDITOR')");
        jdbc.update("DELETE FROM audit_events WHERE actor <> 'SYSTEM'");
        adminRole=roleRepository.findAll().stream().filter(r->r.name().equals("ADMIN")).findFirst().orElseThrow().id();
        auditorRole=roleRepository.findAll().stream().filter(r->r.name().equals("AUDITOR")).findFirst().orElseThrow().id();
        jdbc.update("DELETE FROM role_permissions WHERE role_id=?", auditorRole);
        jdbc.update("INSERT INTO role_permissions(role_id, permission) VALUES (?, 'AUDIT_READ')", auditorRole);
    }
    @Test void anonymousRedirectedAndLoginWorks() throws Exception {
        mvc.perform(get("/users")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("С возвращением")));
        mvc.perform(formLogin().user("admin").password("Test-Admin-Pass-123")).andExpect(authenticated());
        mvc.perform(formLogin().user("admin").password("bad")).andExpect(unauthenticated());
    }
    @Test @WithUserDetails("admin") void allAdminViewsRender() throws Exception {
        long id=userRepository.findByUsername("admin").orElseThrow().id();
        for(String route:List.of("/", "/users", "/users/new", "/users/"+id+"/edit", "/roles", "/roles/new", "/roles/"+auditorRole+"/edit", "/audit"))
            mvc.perform(get(route)).andExpect(status().isOk());
    }
    @Test @WithUserDetails("admin") void csrfRequiredForChanges() throws Exception {
        mvc.perform(post("/roles").param("name","SUPPORT").param("permissions","USER_READ")).andExpect(status().isForbidden());
        assertThat(roleRepository.findAll()).noneMatch(r->r.name().equals("SUPPORT"));
    }
    @Test @WithUserDetails("admin") void createUserHashesPasswordAndAuditsAtomically() throws Exception {
        mvc.perform(post("/users").with(csrf()).param("username","alice").param("displayName","Алиса").param("password","Alice-password-123").param("roleIds",String.valueOf(auditorRole)))
            .andExpect(status().is3xxRedirection());
        var alice=userRepository.findByUsername("alice").orElseThrow();
        assertThat(encoder.matches("Alice-password-123",alice.passwordHash())).isTrue();
        assertThat(alice.passwordHash()).doesNotContain("Alice-password");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='USER_CREATED' AND target='alice'",Long.class)).isEqualTo(1);
    }
    @Test @WithUserDetails("admin") void lastAdminCannotBeDisabledOrDemoted() {
        var admin=userRepository.findByUsername("admin").orElseThrow();
        assertThatThrownBy(()->users.update(admin.id(),new Commands.UpdateUser("Admin",false,Set.of(adminRole)))).isInstanceOf(DomainException.class);
        assertThatThrownBy(()->users.update(admin.id(),new Commands.UpdateUser("Admin",true,Set.of(auditorRole)))).isInstanceOf(DomainException.class);
        assertThat(userRepository.findById(admin.id()).orElseThrow().enabled()).isTrue();
        assertThat(userRepository.activeAdministrators()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='USER_UPDATED'",Long.class)).isZero();
    }
    @Test @WithUserDetails("admin") void systemRoleImmutableAndCustomRoleEditable() {
        assertThatThrownBy(()->roles.update(adminRole,new Commands.SaveRole("OTHER",Set.of(Permission.USER_READ)))).isInstanceOf(DomainException.class);
        roles.create(new Commands.SaveRole("SUPPORT",Set.of(Permission.USER_READ)));
        var role=roleRepository.findAll().stream().filter(r->r.name().equals("SUPPORT")).findFirst().orElseThrow();
        roles.update(role.id(),new Commands.SaveRole("SUPPORT",Set.of(Permission.AUDIT_READ)));
        assertThat(roleRepository.findById(role.id()).orElseThrow().permissions()).containsExactly(Permission.AUDIT_READ);
    }
    @Test @WithUserDetails("admin") void rejectsInvalidAndDuplicateInput() throws Exception {
        assertThatThrownBy(()->users.create(new Commands.CreateUser("valid","Name","Valid-password-123",Set.of(999L)))).isInstanceOf(DomainException.class);
        assertThatThrownBy(()->users.create(new Commands.CreateUser("valid","Name","я".repeat(40),Set.of(auditorRole)))).isInstanceOf(DomainException.class);
        assertThat(userRepository.findByUsername("valid")).isEmpty();
        mvc.perform(post("/users").with(csrf()).param("username","bad!").param("displayName","Name").param("password","short").param("roleIds",String.valueOf(auditorRole))).andExpect(status().isBadRequest());
        mvc.perform(post("/roles").with(csrf()).param("name","AUDITOR").param("permissions","USER_READ")).andExpect(status().isConflict());
    }
    private MockHttpSession loginAuditor() throws Exception {
        userRepository.create("auditor","Аудитор",encoder.encode("Auditor-password-123"),Set.of(auditorRole));
        return (MockHttpSession)mvc.perform(formLogin().user("auditor").password("Auditor-password-123")).andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
    @Test void auditorCannotReadUsersOrMutateEvenWithCsrf() throws Exception {
        var session=loginAuditor();
        mvc.perform(get("/audit").session(session)).andExpect(status().isOk());
        mvc.perform(get("/users").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/roles").session(session).with(csrf()).param("name","ESCALATED").param("permissions","USER_WRITE")).andExpect(status().isForbidden());
    }
    @Test void revokedPermissionTakesEffectInExistingSession() throws Exception {
        var session=loginAuditor();
        mvc.perform(get("/audit").session(session)).andExpect(status().isOk());
        jdbc.update("DELETE FROM role_permissions WHERE role_id=?",auditorRole);
        mvc.perform(get("/audit").session(session)).andExpect(status().isForbidden());
    }
    @Test void disabledAccountLosesExistingSession() throws Exception {
        var session=loginAuditor();
        jdbc.update("UPDATE app_users SET enabled=FALSE WHERE username='auditor'");
        mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/login?expired"));
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void passwordResetRevokesExistingSession() throws Exception {
        var session=loginAuditor();
        userRepository.changePassword(userRepository.findByUsername("auditor").orElseThrow().id(),encoder.encode("Changed-password-123"));
        mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/login?expired"));
        mvc.perform(formLogin().user("auditor").password("Auditor-password-123")).andExpect(unauthenticated());
        mvc.perform(formLogin().user("auditor").password("Changed-password-123")).andExpect(authenticated());
    }
    @Test @WithUserDetails("admin") void roleAssignmentAndBlockingPersist() {
        users.create(new Commands.CreateUser("editor","Editor","Editor-password-123",Set.of(auditorRole)));
        var editor=userRepository.findByUsername("editor").orElseThrow();
        users.update(editor.id(),new Commands.UpdateUser("Editor 2",false,Set.of(adminRole,auditorRole)));
        var updated=userRepository.findById(editor.id()).orElseThrow();
        assertThat(updated.enabled()).isFalse();
        assertThat(updated.roleIds()).containsExactlyInAnyOrder(adminRole,auditorRole);
        assertThat(updated.displayName()).isEqualTo("Editor 2");
    }
}
