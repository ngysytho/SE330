package com.example.chatapp.service;

import com.example.chatapp.dto.AuthDtos.AuthResponse;
import com.example.chatapp.dto.AuthDtos.LoginRequest;
import com.example.chatapp.dto.AuthDtos.RegisterRequest;
import com.example.chatapp.enums.UserRole;
import com.example.chatapp.exception.BadRequestException;
import com.example.chatapp.exception.ForbiddenException;
import com.example.chatapp.model.Session;
import com.example.chatapp.model.User;
import com.example.chatapp.security.CustomUserPrincipal;
import com.example.chatapp.security.JwtService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthService {
    private final FirebaseService firebase;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String firebaseWebApiKey;

    public AuthService(
            FirebaseService firebase,
            JwtService jwtService,
            @Value("${firebase.web-api-key}") String firebaseWebApiKey
    ) {
        this.firebase = firebase;
        this.jwtService = jwtService;
        this.firebaseWebApiKey = firebaseWebApiKey;
    }

    public AuthResponse register(RegisterRequest request) {
        validateRegister(request);
        String gmail = normalizeGmail(request.gmail());

        try {
            UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(new UserRecord.CreateRequest()
                    .setEmail(gmail)
                    .setEmailVerified(false)
                    .setPassword(request.password())
                    .setDisplayName(request.name().trim())
                    .setDisabled(false));

            User user = createUserDocument(firebaseUser.getUid(), gmail, request.name().trim(), passwordEncoder.encode(request.password()));
            String token = jwtService.generateToken(user);
            createSession(user);
            return new AuthResponse(token, user);
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            String code = e.getAuthErrorCode() == null ? "" : e.getAuthErrorCode().name();
            if ("EMAIL_ALREADY_EXISTS".equals(code)) {
                throw new BadRequestException("Account is already registered");
            }
            throw new BadRequestException(e.getMessage());
        }
    }

    public AuthResponse login(LoginRequest request) {
        validateLogin(request);
        String gmail = normalizeGmail(request.gmail());
        User user = authenticate(gmail, request.password());

        long now = firebase.now();
        firebase.update(FirebaseService.USERS, user.getId(), Map.of("isOnline", true, "lastSeenAt", now, "updatedAt", now));
        user.setIsOnline(true);
        user.setLastSeenAt(now);
        user.setUpdatedAt(now);

        String token = jwtService.generateToken(user);
        createSession(user);
        return new AuthResponse(token, user);
    }

    public User me(CustomUserPrincipal principal) {
        return firebase.get(FirebaseService.USERS, principal.id(), User.class);
    }

    public void logout(String userId) {
        long now = firebase.now();
        firebase.update(FirebaseService.USERS, userId, Map.of("isOnline", false, "lastSeenAt", now, "updatedAt", now));
        firebase.save(FirebaseService.PRESENCE, userId, Map.of("userId", userId, "isOnline", false, "lastSeenAt", now));
    }

    public User findByFirebaseUid(String uid) {
        return firebase.run(firebase.collection(FirebaseService.USERS).whereEqualTo("uid", uid).limit(1).get())
                .stream()
                .map(snapshot -> snapshot.toObject(User.class))
                .findFirst()
                .orElseThrow(() -> new ForbiddenException("User profile not found"));
    }

    private User findOrCreateLoginProfile(String firebaseUid, String gmail) {
        return findUserByUid(firebaseUid)
                .orElseGet(() -> findUserByGmail(gmail)
                        .map(user -> linkLegacyProfile(user, firebaseUid))
                        .orElseGet(() -> createUserDocument(firebaseUid, gmail, displayName(firebaseUid, gmail), null)));
    }

    private User authenticate(String gmail, String password) {
        Optional<User> localUser = findUserByGmail(gmail);
        if (localUser.isPresent()) {
            User user = localUser.get();
            if (user.getPasswordHash() != null && !user.getPasswordHash().isBlank()) {
                if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                    throw new ForbiddenException("Invalid account or password");
                }
                return user;
            }

            Optional<String> firebaseUid = tryVerifyPasswordWithFirebase(gmail, password);
            if (firebaseUid.isPresent()) {
                return storePasswordHash(linkLegacyProfile(user, firebaseUid.get()), password);
            }

            return storePasswordHash(user, password);
        }

        String firebaseUid = verifyPasswordWithFirebase(gmail, password);
        return storePasswordHash(findOrCreateLoginProfile(firebaseUid, gmail), password);
    }

    private java.util.Optional<User> findUserByUid(String uid) {
        return firebase.run(firebase.collection(FirebaseService.USERS).whereEqualTo("uid", uid).limit(1).get())
                .stream()
                .map(snapshot -> snapshot.toObject(User.class))
                .filter(user -> user.getDeletedAt() == null)
                .findFirst();
    }

    private java.util.Optional<User> findUserByGmail(String gmail) {
        java.util.Optional<User> exactMatch = firebase.run(firebase.collection(FirebaseService.USERS).whereEqualTo("gmail", gmail).limit(1).get())
                .stream()
                .map(snapshot -> snapshot.toObject(User.class))
                .filter(user -> user.getDeletedAt() == null)
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }
        return firebase.all(FirebaseService.USERS, User.class).stream()
                .filter(user -> user.getDeletedAt() == null)
                .filter(user -> user.getGmail() != null && normalizeGmail(user.getGmail()).equals(gmail))
                .findFirst();
    }

    private User linkLegacyProfile(User user, String firebaseUid) {
        long now = firebase.now();
        firebase.update(FirebaseService.USERS, user.getId(), Map.of("uid", firebaseUid, "updatedAt", now));
        user.setUid(firebaseUid);
        user.setUpdatedAt(now);
        return user;
    }

    private String displayName(String firebaseUid, String gmail) {
        try {
            String name = FirebaseAuth.getInstance().getUser(firebaseUid).getDisplayName();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        } catch (com.google.firebase.auth.FirebaseAuthException ignored) {
        }
        return gmail.substring(0, gmail.indexOf("@") > 0 ? gmail.indexOf("@") : gmail.length());
    }

    private User createUserDocument(String firebaseUid, String gmail, String name, String passwordHash) {
        return findUserByUid(firebaseUid)
                .or(() -> findUserByGmail(gmail).map(user -> linkLegacyProfile(user, firebaseUid)))
                .map(user -> passwordHash == null ? user : storePasswordHash(user, passwordHash, true))
                .orElseGet(() -> {
                    long now = firebase.now();
                    String id = firebase.newId(FirebaseService.USERS);
                    User user = User.builder()
                            .id(id)
                            .uid(firebaseUid)
                            .passwordHash(passwordHash)
                            .name(name)
                            .gmail(gmail)
                            .role(UserRole.DEFAULT)
                            .isOnline(false)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                    firebase.save(FirebaseService.USERS, id, user);
                    return user;
                });
    }

    private User storePasswordHash(User user, String password) {
        return storePasswordHash(user, passwordEncoder.encode(password), true);
    }

    private User storePasswordHash(User user, String passwordHash, boolean alreadyEncoded) {
        String hash = alreadyEncoded ? passwordHash : passwordEncoder.encode(passwordHash);
        long now = firebase.now();
        firebase.update(FirebaseService.USERS, user.getId(), Map.of("passwordHash", hash, "updatedAt", now));
        user.setPasswordHash(hash);
        user.setUpdatedAt(now);
        return user;
    }

    @SuppressWarnings("unchecked")
    private String verifyPasswordWithFirebase(String gmail, String password) {
        return tryVerifyPasswordWithFirebase(gmail, password)
                .orElseThrow(() -> new ForbiddenException("Invalid account or password"));
    }

    private Optional<String> tryVerifyPasswordWithFirebase(String gmail, String password) {
        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseWebApiKey;
        Map<String, Object> body = new HashMap<>();
        body.put("email", gmail);
        body.put("password", password);
        body.put("returnSecureToken", true);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
            if (response == null || response.get("localId") == null) {
                return Optional.empty();
            }
            return Optional.of(response.get("localId").toString());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private void createSession(User user) {
        String sessionId = firebase.newId(FirebaseService.SESSIONS);
        long now = firebase.now();
        firebase.save(FirebaseService.SESSIONS, sessionId, Session.builder()
                .id(sessionId)
                .userId(user.getId())
                .uid(user.getUid())
                .loggedInAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void validateRegister(RegisterRequest request) {
        if (request.gmail() == null || request.gmail().isBlank()) {
            throw new BadRequestException("Account is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new BadRequestException("Name is required");
        }
    }

    private void validateLogin(LoginRequest request) {
        if (request.gmail() == null || request.gmail().isBlank()) {
            throw new BadRequestException("Account is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("Password is required");
        }
    }

    private String normalizeGmail(String gmail) {
        return gmail.trim().toLowerCase();
    }
}
