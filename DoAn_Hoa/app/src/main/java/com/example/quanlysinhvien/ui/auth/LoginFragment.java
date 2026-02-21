package com.example.quanlysinhvien.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.MainActivity;
import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.AuthResult;
import com.example.quanlysinhvien.auth.AuthService;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.databinding.FragmentLoginBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;
    private AuthService authService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authService = new AuthService(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnLogin.setOnClickListener(v -> {
            if (validateInput()) {
                attemptLogin();
            }
        });
    }

    private boolean validateInput() {
        binding.tilUsername.setError(null);
        binding.tilPassword.setError(null);

        String username = binding.etMssv.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            binding.tilUsername.setError(getString(R.string.login_error_empty_username));
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.login_error_empty_password));
            return false;
        }

        return true;
    }

    private void attemptLogin() {
        setLoading(true);
        String mssv = binding.etMssv.getText().toString().trim();
        String pass = binding.etPassword.getText().toString();

        executor.execute(() -> {
            String androidId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            AuthResult res = authService.login(mssv, pass, androidId);

            handler.post(() -> {
                if (getContext() == null) return; // Fragment is not attached
                setLoading(false);

                switch (res.getStatus()) {
                    case SUCCESS:
                        User user = res.getUser();
                        SessionManager session = new SessionManager(requireContext());
                        session.saveSession(user.getId(), user.getRole());
                        goToMainActivity();
                        break;
                    case NEEDS_PASSWORD_RESET:
                        Bundle bundle = new Bundle();
                        bundle.putLong("userId", res.getUser().getId());
                        NavHostFragment.findNavController(this).navigate(R.id.action_login_to_changePassword, bundle);
                        break;
                    case DEVICE_MISMATCH:
                        Toast.makeText(getContext(), "Tài khoản đã được liên kết với thiết bị khác.", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        binding.tilPassword.setError(getString(R.string.login_error_invalid_credentials));
                        Toast.makeText(getContext(), getString(R.string.login_error_invalid_credentials), Toast.LENGTH_SHORT).show();
                        break;
                }
            });
        });
    }

    private void goToMainActivity() {
        if (getActivity() == null) return;
        Intent it = new Intent(getActivity(), MainActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(it);
        getActivity().finish();
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.loadingIndicator.setVisibility(View.VISIBLE);
            binding.btnLogin.setEnabled(false);
        } else {
            binding.loadingIndicator.setVisibility(View.GONE);
            binding.btnLogin.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
