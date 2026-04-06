package com.example.quanlysinhvien.ui.admin.studentmanagement;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.ui.base.ConfirmationDialogFragment;

import java.util.List;

public class StudentManagementFragment extends Fragment {

    private UserRepository userRepository;
    private StudentManagementAdapter adapter;
    private EditText etSearch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_search_student);
        RecyclerView recyclerView = view.findViewById(R.id.rv_student_list);
        int spanCount = getResources().getInteger(R.integer.student_grid_span_count);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));

        adapter = new StudentManagementAdapter(this::onEditStudent, this::onDeleteStudent, this::onResetFaceStudent);
        recyclerView.setAdapter(adapter);

        loadStudents("");

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        view.findViewById(R.id.fab_add_student).setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_student_list_to_create);
        });
    }

    private void loadStudents(String filter) {
        List<User> students = userRepository.getAllStudents(filter);
        adapter.submitList(students);
    }

    private void reloadCurrentFilter() {
        loadStudents(etSearch.getText() == null ? "" : etSearch.getText().toString());
    }

    private void onEditStudent(User student) {
        Bundle bundle = new Bundle();
        bundle.putLong("student_id", student.getId());
        NavHostFragment.findNavController(this).navigate(R.id.action_student_list_to_create, bundle);
    }

    private void onDeleteStudent(User student) {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                getString(R.string.confirm_delete_student_title),
                getString(R.string.confirm_delete_student_message, student.getName()),
                R.drawable.ic_baseline_delete_24,
                getString(R.string.btn_delete),
                "Hủy");

        dialog.setOnResultListener(confirmed -> {
            if (confirmed) {
                userRepository.deleteUser(student.getId());
                reloadCurrentFilter();
            }
        });

        dialog.show(getParentFragmentManager(), "DeleteStudentConfirmation");
    }

    private void onResetFaceStudent(User student) {
        if (TextUtils.isEmpty(student.getFaceTemplate())) {
            Toast.makeText(getContext(), "Sinh viên chưa có dữ liệu khuôn mặt", Toast.LENGTH_SHORT).show();
            return;
        }

        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                "Reset khuôn mặt",
                "Xóa dữ liệu khuôn mặt của " + student.getName()
                        + "? Sau khi reset, sinh viên sẽ không thể điểm danh khuôn mặt cho đến khi admin cấp lại.",
                R.drawable.ic_baseline_refresh_24,
                "Reset",
                "Hủy");

        dialog.setOnResultListener(confirmed -> {
            if (confirmed) {
                if (userRepository.resetFaceTemplate(student.getId())) {
                    Toast.makeText(getContext(), "Reset khuôn mặt thành công", Toast.LENGTH_SHORT).show();
                    reloadCurrentFilter();
                } else {
                    Toast.makeText(getContext(), "Reset khuôn mặt thất bại", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show(getParentFragmentManager(), "ResetFaceConfirmation");
    }
}
