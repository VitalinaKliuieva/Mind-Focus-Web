package com.mindfocus.web;

import com.mindfocus.domain.User;
import com.mindfocus.repo.UserRepository;
import com.mindfocus.security.CurrentUser;
import com.mindfocus.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 100) String password,
                                  @NotBlank @Size(max = 60) String displayName) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserDto(Long id, String email, String displayName) {}

    public record AuthResponse(String token, UserDto user) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already registered");
        }
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(r.displayName().trim());
        u.setPasswordHash(encoder.encode(r.password()));
        return respond(users.save(u));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password");
        }
        return respond(u);
    }

    @GetMapping("/me")
    public UserDto me() {
        User u = users.findById(CurrentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in"));
        return dto(u);
    }

    private AuthResponse respond(User u) {
        return new AuthResponse(jwt.issue(u.getId(), u.getEmail()), dto(u));
    }

    private UserDto dto(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getDisplayName());
    }
}
