package com.example.quanlysinhvien.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quanlysinhvien.MainActivity;
import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.databinding.FragmentChangePasswordBinding;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChangePasswordFragment extends Fragment {

    private long userId;
    private UserRepository userRepository;
    private FragmentChangePasswordBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        if (getArguments() != null) {
            userId = getArguments().getLong("userId");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnChangePassword.setOnClickListener(v -> {
            if (validateInput()) {
                changePassword();
            }
        });
    }

    private boolean validateInput() {
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        String newPass = binding.etNewPassword.getText().toString().trim();
        String confirmPass = binding.etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPass)) {
            binding.tilNewPassword.setError(getString(R.string.error_password_empty));
            return false;
        }

        if (TextUtils.isEmpty(confirmPass)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_confirm_password_empty));
            return false;
        }

        if (!newPass.equals(confirmPass)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return false;
        }
        return true;
    }


    private void changePassword() {
        showLoading(true);
        String newPass = binding.etNewPassword.getText().toString().trim();

        executor.execute(() -> {
            boolean success = userRepository.changePassword(userId, newPass);
            String newToken = null;
            if (success) {
                // Invalidate old token by creating a new one
                newToken = UUID.randomUUID().toString();
                userRepository.updateAuthToken(userId, newToken);
            }
            User user = success ? userRepository.getUserById(userId) : null;

            final String finalToken = newToken;
            handler.post(() -> {
                showLoading(false);
                if (success && user != null) {
                    // Save new session and go to main
                    if (getContext() == null) return;
                    SessionManager session = new SessionManager(requireContext());
                    session.saveSession(user.getId(), user.getRole(), finalToken);

                    Toast.makeText(getContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();

                    Intent it = new Intent(getActivity(), MainActivity.class);
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(it);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                } else {
                     if (getContext() == null) return;
                    Toast.makeText(getContext(), getString(R.string.error_change_password), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnChangePassword.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnChangePassword.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }
}
