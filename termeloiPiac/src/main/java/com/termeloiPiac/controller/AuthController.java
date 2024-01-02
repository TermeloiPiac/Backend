package com.termeloiPiac.controller;

import com.termeloiPiac.dao.RoleDAO;
import com.termeloiPiac.dao.UserDAO;
import com.termeloiPiac.dto.request.LoginRequestDTO;
import com.termeloiPiac.dto.request.RegisterRequestDTO;
import com.termeloiPiac.dto.response.LoginResponseDTO;
import com.termeloiPiac.dto.response.RegisterResponseDTO;
import com.termeloiPiac.entity.Role;
import com.termeloiPiac.entity.User;
import com.termeloiPiac.security.jwt.JwtUtils;
import com.termeloiPiac.service.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true",
        allowedHeaders = {"Origin", "X-Api-Key", "X-Requested-With", "Content-Type", "Accept", "Authorization"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping("/termeloiPiac/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDAO userDAO;
    private final RoleDAO roleDAO;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserDAO userDAO, RoleDAO roleDAO,
                          PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userDAO = userDAO;
        this.roleDAO = roleDAO;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/session")
    public ResponseEntity<?> getSession(@CookieValue(name = "_sessionUser") Cookie cookie) {

        HttpHeaders httpHeaders = new HttpHeaders();
        UserDetailsImpl userDetails = null;

        LOGGER.info("#cc {}", cookie);
        if(cookie != null) {
            ResponseCookie resCookie = ResponseCookie.from("_sessionUser", cookie.getValue())
                    .maxAge(86400)
                    .domain("localhost")
                    .path("/")
                    .secure(true)
                    .httpOnly(true)
                    .build();

            httpHeaders.add(HttpHeaders.SET_COOKIE, resCookie.toString());

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            userDetails = (UserDetailsImpl) authentication.getPrincipal();
        }
        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(userDetails != null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequestDTO.getEmail(), loginRequestDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority())
                .collect(Collectors.toList());

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.SET_COOKIE, jwtUtils.generateSessionUserCookie(jwt).toString());
        return ResponseEntity.ok()
                .headers(httpHeaders)
                .body(new LoginResponseDTO(userDetails.getId(), userDetails.getFirstname(),
                        userDetails.getLastname(), userDetails.getUsername(), userDetails.getEmail(), userDetails.getPhoneNumber(), userDetails.getCreateDate(), roles));
    }
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        if (userDAO.existsByEmail(registerRequestDTO.getEmail())) {
            return ResponseEntity.badRequest().body(new RegisterResponseDTO("Error: Email is already in use!"));
        }

        // Create new user's account
        User user = new User(registerRequestDTO.getFirstName(), registerRequestDTO.getLastName(),
                registerRequestDTO.getEmail(), encoder.encode(registerRequestDTO.getPassword()), registerRequestDTO.getPhoneNumber(), Calendar.getInstance());

        Set<String> strRoles = registerRequestDTO.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleDAO.findByName(Role.ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleDAO.findByName(Role.ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "mod":
                        Role modRole = roleDAO.findByName(Role.ERole.ROLE_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);

                        break;
                    default:
                        Role userRole = roleDAO.findByName(Role.ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userDAO.save(user);

        return ResponseEntity.ok(new RegisterResponseDTO("User registered successfully!"));
    }
}