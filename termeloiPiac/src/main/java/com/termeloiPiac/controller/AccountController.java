package com.termeloiPiac.controller;

import com.termeloiPiac.dao.UserDAO;
import com.termeloiPiac.dto.response.LoginResponseDTO;
import com.termeloiPiac.security.jwt.JwtUtils;
import com.termeloiPiac.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:4200", maxAge = 3600, allowCredentials = "true",
        allowedHeaders = {"Origin", "X-Api-Key", "X-Requested-With", "Content-Type", "Accept", "Authorization"},
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.PUT, RequestMethod.DELETE})
@RestController
@RequestMapping(value = "/termeloiPiac/api/account")
public class AccountController {
    private final UserDAO userDAO;
    private final JwtUtils jwtUtils;

    @Autowired
    public AccountController(UserDAO userDAO, JwtUtils jwtUtils) {
        this.userDAO = userDAO;
        this.jwtUtils = jwtUtils;
    }


    @GetMapping("/getUserData")
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public ResponseEntity<?> getUserData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return ResponseEntity.ok()
                .body(new LoginResponseDTO(userDetails.getId(), userDetails.getFirstname(),
                        userDetails.getLastname(), userDetails.getUsername(), userDetails.getEmail(), userDetails.getPhoneNumber()));
    }

    /*@RequestMapping(value = "/getUserProfileData", method = RequestMethod.GET)
    @PreAuthorize("hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')")
    public @ResponseBody UserProfileDataResponse userDataCall() {
        return userProfileDataService.process();
    }*/
}