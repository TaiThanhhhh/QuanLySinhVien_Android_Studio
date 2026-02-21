package com.example.quanlysinhvien.ui.student.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.google.android.material.textfield.TextInputEditText;

public class StudentChangePasswordFragment extends Fragment {

    private UserRepository userRepository;
    private SessionManager sessionManager;

    private TextInputEditText etOldPassword, etNewPassword, etConfirmPassword;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etOldPassword = view.findViewById(R.id.et_old_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        Button btnSave = view.findViewById(R.id.btn_save_password);

        btnSave.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPass = etOldPassword.getText().toString();
        String newPass = etNewPassword.getText().toString();
        String confirmPass = etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            return;
        }

        if (newPass.length() < 6) {
            etNewPassword.setError("Mật khẩu mới phải có ít nhất 6 ký tự");
            return;
        }

        long userId = sessionManager.getUserId();
        User currentUser = userRepository.getUserById(userId);

        if (currentUser == null) {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!userRepository.verifyPassword(currentUser, oldPass)) {
            etOldPassword.setError("Mật khẩu cũ không đúng");
            return;
        }

        if (userRepository.changePassword(userId, newPass)) {
            Toast.makeText(getContext(), "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        } else {
            Toast.makeText(getContext(), "Đổi mật khẩu thất bại", Toast.LENGTH_SHORT).show();
        }
    }
}
