package com.example.quanlysinhvien.auth;

import android.content.Context;

import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;

import java.util.UUID;

public class AuthService {
    private final UserRepository userRepo;

    public AuthService(Context ctx) {
        this.userRepo = new UserRepository(ctx);
    }

    public AuthResult login(String mssv, String password, String androidId) {
        User u = userRepo.getUserByMssv(mssv);
        if (u == null)
            return new AuthResult(AuthResult.Status.INVALID_CREDENTIALS, null);
        if (!userRepo.verifyPassword(u, password))
            return new AuthResult(AuthResult.Status.INVALID_CREDENTIALS, null);

        // device binding (Students only)
        if ("STUDENT".equals(u.getRole())) {
            String bound = u.getDeviceId();
            if (bound == null) {
                boolean boundOk = userRepo.updateDeviceBinding(u.getId(), androidId);
                if (!boundOk) {
                    // could not bind (race) - treat as mismatch
                    return new AuthResult(AuthResult.Status.DEVICE_MISMATCH, null);
                }
            } else if (!bound.equals(androidId)) {
                return new AuthResult(AuthResult.Status.DEVICE_MISMATCH, u);
            }
        }

        if (u.isPasswordNeedsReset())
            return new AuthResult(AuthResult.Status.NEEDS_PASSWORD_RESET, u);

        String token = UUID.randomUUID().toString();
        if (!userRepo.updateAuthToken(u.getId(), token)) {
            // Handle token update failure, maybe return an error
            return new AuthResult(AuthResult.Status.INVALID_CREDENTIALS, null); // Or a new status for this case
        }

        return new AuthResult(AuthResult.Status.SUCCESS, u, token);
    }
}
