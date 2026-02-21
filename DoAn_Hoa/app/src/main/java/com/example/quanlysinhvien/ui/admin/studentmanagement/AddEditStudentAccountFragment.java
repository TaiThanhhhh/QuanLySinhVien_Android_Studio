package com.example.quanlysinhvien.ui.admin.studentmanagement;

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
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddEditStudentAccountFragment extends Fragment {

    private UserRepository userRepository;
    private long studentId = -1;
    private User currentUser = null;

    private TextInputEditText etName, etMssv, etPassword;
    private TextInputLayout layoutPassword;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        if (getArguments() != null) {
            studentId = getArguments().getLong("student_id", -1L);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_student_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.et_student_name_account);
        etMssv = view.findViewById(R.id.et_student_id_account);
        etPassword = view.findViewById(R.id.et_password_account);
        layoutPassword = view.findViewById(R.id.layout_password_account);

        if (studentId != -1) {
            // Edit Mode
            currentUser = userRepository.getUserById(studentId);
            if (currentUser != null) {
                etName.setText(currentUser.getName());
                etMssv.setText(currentUser.getMssv());
                etMssv.setEnabled(false);
                layoutPassword.setVisibility(View.GONE);
            }
        } else {
            // Add Mode
            // All fields are enabled and visible by default
        }

        Button btnSave = view.findViewById(R.id.btn_save_student_account);
        btnSave.setOnClickListener(v -> saveStudent());
    }

    private void saveStudent() {
        String name = etName.getText().toString().trim();
        String mssv = etMssv.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mssv)) {
            Toast.makeText(getContext(), "Tên và MSSV không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mssv.length() != 10) {
            Toast.makeText(getContext(), "MSSV phải có đúng 10 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        if (studentId == -1) {
            // Add new user
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), "Mật khẩu không được để trống", Toast.LENGTH_SHORT).show();
                return;
            }
            if (userRepository.getUserByMssv(mssv) != null) {
                Toast.makeText(getContext(), "MSSV này đã tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            User newUser = new User();
            newUser.setName(name);
            newUser.setMssv(mssv);
            newUser.setRole("STUDENT");
            long result = userRepository.createUser(newUser, password);
            if (result != -1) {
                Toast.makeText(getContext(), "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                Toast.makeText(getContext(), "Tạo tài khoản thất bại", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing user
            currentUser.setName(name);
            boolean result = userRepository.updateUser(currentUser);
            if (result) {
                Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
