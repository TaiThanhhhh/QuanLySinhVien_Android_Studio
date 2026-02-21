package com.example.quanlysinhvien.ui.admin.studentmanagement;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.google.android.material.textfield.TextInputEditText;

public class CreateEditStudentFragment extends Fragment {

    private UserRepository userRepository;
    private long studentId = -1;
    private User existingStudent;

    private TextInputEditText etName, etMssv;
    private TextView tvTitle;
    private Button btnSave, btnResetDevice, btnResetPassword;
    private LinearLayout adminActionsLayout;

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
        return inflater.inflate(R.layout.fragment_create_edit_student, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);

        if (studentId != -1) {
            tvTitle.setText(R.string.title_edit_student);
            btnSave.setText(R.string.btn_update);
            adminActionsLayout.setVisibility(View.VISIBLE);
            loadStudentData();
        } else {
            tvTitle.setText(R.string.title_create_student);
            btnSave.setText(R.string.btn_save);
            adminActionsLayout.setVisibility(View.GONE);
        }

        setupClickListeners();
    }

    private void bindViews(View view) {
        tvTitle = view.findViewById(R.id.tv_create_edit_student_title);
        etName = view.findViewById(R.id.et_student_name);
        etMssv = view.findViewById(R.id.et_student_mssv);
        btnSave = view.findViewById(R.id.btn_save_student);
        adminActionsLayout = view.findViewById(R.id.layout_admin_actions);
        btnResetDevice = view.findViewById(R.id.btn_reset_device);
        btnResetPassword = view.findViewById(R.id.btn_reset_password);
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(v -> saveStudent());
        btnResetDevice.setOnClickListener(v -> confirmResetDevice());
        btnResetPassword.setOnClickListener(v -> confirmResetPassword());
    }

    private void loadStudentData() {
        existingStudent = userRepository.getUserById(studentId);
        if (existingStudent != null) {
            etName.setText(existingStudent.getName());
            etMssv.setText(existingStudent.getMssv());
            etMssv.setEnabled(false);
        }
    }

    private void confirmResetDevice() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Reset Thiết bị")
                .setMessage("Bạn có chắc chắn muốn hủy liên kết thiết bị cho sinh viên " + existingStudent.getName() + "? Sinh viên sẽ có thể đăng nhập trên một thiết bị mới.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    if (userRepository.resetDeviceBinding(studentId)) {
                        Toast.makeText(getContext(), "Reset thiết bị thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Reset thiết bị thất bại", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmResetPassword() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Reset Mật khẩu")
                .setMessage("Bạn có chắc chắn muốn đặt lại mật khẩu cho sinh viên " + existingStudent.getName() + "? Mật khẩu sẽ được đặt về mặc định và sinh viên sẽ phải đổi lại ở lần đăng nhập tới.")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    if (userRepository.resetPassword(studentId)) {
                        Toast.makeText(getContext(), "Reset mật khẩu thành công", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Reset mật khẩu thất bại", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveStudent() {
        String name = etName.getText().toString().trim();
        String mssv = etMssv.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(mssv)) {
            Toast.makeText(getContext(), R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mssv.length() != 10) {
            etMssv.setError(getString(R.string.error_invalid_mssv_length));
            return;
        }

        if (studentId != -1) { // Update mode
            existingStudent.setName(name);
            if (userRepository.updateUser(existingStudent)) {
                Toast.makeText(getContext(), R.string.success_update_student, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                Toast.makeText(getContext(), R.string.error_update_student, Toast.LENGTH_SHORT).show();
            }
        } else { // Create mode
            if (userRepository.getUserByMssv(mssv) != null) {
                etMssv.setError(getString(R.string.error_mssv_exists));
                return;
            }
            
            User newUser = new User();
            newUser.setName(name);
            newUser.setMssv(mssv);
            newUser.setRole("STUDENT");
            newUser.setPasswordNeedsReset(true);

            if (userRepository.createUser(newUser, "123456") != -1) {
                Toast.makeText(getContext(), R.string.success_create_student, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                Toast.makeText(getContext(), R.string.error_create_student, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
