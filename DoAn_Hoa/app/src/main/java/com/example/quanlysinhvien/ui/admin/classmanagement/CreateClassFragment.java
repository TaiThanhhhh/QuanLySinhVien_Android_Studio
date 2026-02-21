package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.ClassModel;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.ClassRepository;
import com.example.quanlysinhvien.data.repo.EnrollmentRepository;
import com.example.quanlysinhvien.databinding.FragmentCreateClassBinding;
import com.example.quanlysinhvien.ui.base.ConfirmationDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CreateClassFragment extends Fragment implements SelectedStudentAdapter.OnStudentRemovedListener {

    private CreateClassViewModel viewModel;
    private ClassRepository classRepository;
    private EnrollmentRepository enrollmentRepository;
    private SessionManager sessionManager;
    private long classId = -1;

    private FragmentCreateClassBinding binding;
    private SelectedStudentAdapter selectedStudentAdapter;

    public static final String REQUEST_KEY = "student_selection_key";
    public static final String BUNDLE_KEY = "selected_students";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        classRepository = new ClassRepository(requireContext());
        enrollmentRepository = new EnrollmentRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        if (getArguments() != null) {
            classId = getArguments().getLong("class_id", -1L);
        }

        getParentFragmentManager().setFragmentResultListener(REQUEST_KEY, this, (requestKey, bundle) -> {
            List<User> selectedStudents = bundle.getParcelableArrayList(BUNDLE_KEY);
            if (selectedStudents != null) {
                viewModel.setStudentList(selectedStudents);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateClassBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CreateClassViewModel.class);

        setupRecyclerView();
        setupClickListeners();

        if (classId != -1) {
            loadClassData();
        } else {
            setupCreateMode();
        }

        observeViewModel();
    }

    private void setupRecyclerView() {
        selectedStudentAdapter = new SelectedStudentAdapter(this);
        binding.rvSelectedStudents.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSelectedStudents.setAdapter(selectedStudentAdapter);
    }

    private void setupCreateMode() {
        viewModel.clear();
        binding.tvCreateClassTitle.setText(getString(R.string.create_class_title));
        updateToolbarTitle(getString(R.string.create_class_title));
        binding.btnCreateClass.setText(getString(R.string.btn_create_class));
    }

    private void observeViewModel() {
        viewModel.getStudentList().observe(getViewLifecycleOwner(), students -> {
            if (students == null) return;
            selectedStudentAdapter.submitList(students);
            int studentCount = students.size();
            binding.tvStudentCount.setText(String.format(Locale.getDefault(), getString(R.string.selected_student_count), studentCount));
            binding.btnCreateClass.setEnabled(studentCount >= 8);
        });
    }

    @Override
    public void onStudentRemoved(User student) {
        showRemoveStudentConfirmation(student);
    }

    private void showRemoveStudentConfirmation(User student) {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                "Xóa sinh viên?",
                "Bạn có chắc chắn muốn xóa " + student.getName() + " khỏi danh sách này không?",
                R.drawable.ic_baseline_delete_24,
                "Xóa",
                "Hủy"
        );

        dialog.setOnResultListener(confirmed -> {
            if (confirmed) {
                viewModel.removeStudent(student);
                Toast.makeText(getContext(), "Đã xóa sinh viên khỏi danh sách", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show(getParentFragmentManager(), "RemoveStudentConfirmation");
    }

    private void loadClassData() {
        ClassModel classModel = classRepository.getClassById(classId);
        if (classModel == null) return;

        String formattedTitle = String.format(Locale.getDefault(), getString(R.string.edit_class_title_format), classModel.getTitle(), classModel.getSemester());
        updateToolbarTitle(formattedTitle);
        binding.tvCreateClassTitle.setText(formattedTitle);
        binding.btnCreateClass.setText(getString(R.string.btn_update_class));

        binding.etClassName.setText(classModel.getTitle());
        binding.etSubject.setText(classModel.getSubject());
        binding.etTeacher.setText(classModel.getTeacher());
        binding.etSemester.setText(classModel.getSemester());
        binding.etRoom.setText(classModel.getRoom());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        if (classModel.getStartDate() != null && classModel.getStartDate() > 0) {
            binding.etStartDate.setText(sdf.format(new Date(classModel.getStartDate())));
        }
        if (classModel.getEndDate() != null && classModel.getEndDate() > 0) {
            binding.etEndDate.setText(sdf.format(new Date(classModel.getEndDate())));
        }

        List<User> students = enrollmentRepository.getStudentsInClass(classId);
        viewModel.setStudentList(students);
    }

    private void updateToolbarTitle(String title) {
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void setupClickListeners() {
        binding.etStartDate.setOnClickListener(v -> showDatePickerDialog(binding.etStartDate));
        binding.etEndDate.setOnClickListener(v -> showDatePickerDialog(binding.etEndDate));

        binding.btnAddStudent.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            List<User> currentStudents = viewModel.getStudentList().getValue();
            if(currentStudents == null) {
                currentStudents = new ArrayList<>();
            }
            bundle.putParcelableArrayList(BUNDLE_KEY, new ArrayList<>(currentStudents));
            NavHostFragment.findNavController(this).navigate(R.id.action_create_to_add_student, bundle);
        });

        binding.btnCreateClass.setOnClickListener(v -> saveClass());
    }

    private void saveClass() {
        String className = "";
        if (binding.etClassName.getText() != null) {
            className = binding.etClassName.getText().toString().trim();
        }

        if (TextUtils.isEmpty(className)) {
            binding.tilClassName.setError("Tên lớp không được để trống");
            return;
        }
        binding.tilClassName.setError(null); // Clear error

        List<User> students = viewModel.getStudentList().getValue();
        if (students == null || students.size() < 8) {
            Toast.makeText(getContext(), "Phải có ít nhất 8 sinh viên trong lớp", Toast.LENGTH_SHORT).show();
            return;
        }

        ClassModel classModel = (classId != -1) ? classRepository.getClassById(classId) : new ClassModel();
        if (classModel == null) {
            Toast.makeText(getContext(), "Lỗi: Không thể tải dữ liệu lớp học.", Toast.LENGTH_SHORT).show();
            return;
        }

        classModel.setTitle(className);
        if (binding.etSubject.getText() != null)
            classModel.setSubject(binding.etSubject.getText().toString().trim());
        if (binding.etTeacher.getText() != null)
            classModel.setTeacher(binding.etTeacher.getText().toString().trim());
        if (binding.etSemester.getText() != null)
            classModel.setSemester(binding.etSemester.getText().toString().trim());
        if (binding.etRoom.getText() != null)
            classModel.setRoom(binding.etRoom.getText().toString().trim());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            if (binding.etStartDate.getText() != null && !TextUtils.isEmpty(binding.etStartDate.getText().toString())) {
                Date startDate = sdf.parse(binding.etStartDate.getText().toString());
                if (startDate != null) classModel.setStartDate(startDate.getTime());
            }
            if (binding.etEndDate.getText() != null && !TextUtils.isEmpty(binding.etEndDate.getText().toString())) {
                Date endDate = sdf.parse(binding.etEndDate.getText().toString());
                if (endDate != null) classModel.setEndDate(endDate.getTime());
            }
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Ngày tháng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (classId != -1) {
            showUpdateConfirmationDialog(classModel, students);
        } else {
            performClassCreation(classModel, students);
        }
    }

    private void showUpdateConfirmationDialog(ClassModel classModel, List<User> students) {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                "Xác nhận cập nhật",
                "Bạn có chắc chắn muốn cập nhật thông tin lớp học này không? Hành động này sẽ ghi đè dữ liệu cũ.",
                R.drawable.ic_baseline_check_circle_24,
                "Cập nhật",
                "Hủy"
        );

        dialog.setOnResultListener(confirmed -> {
            if (confirmed) {
                performClassUpdate(classModel, students);
            }
        });

        dialog.show(getParentFragmentManager(), "UpdateClassConfirmation");
    }

    private void performClassUpdate(ClassModel classModel, List<User> students) {
        boolean success = classRepository.updateClass(classModel);
        if (success) {
            enrollmentRepository.updateEnrolledStudents(classId, students);
        }

        if (success) {
            Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_create_class, true).build();
            NavHostFragment.findNavController(this).navigate(R.id.nav_list_classes, null, navOptions);
        } else {
            Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private void performClassCreation(ClassModel classModel, List<User> students) {
        classModel.setClassCode("CLASS_" + System.currentTimeMillis());
        classModel.setTeacherId(sessionManager.getUserId());
        long newClassId = classRepository.createClass(classModel);
        boolean success = newClassId != -1;
        if (success) {
            for (User student : students) {
                enrollmentRepository.enrollStudent(newClassId, student.getId());
            }
        }

        if (success) {
            Toast.makeText(getContext(), "Tạo lớp thành công!", Toast.LENGTH_SHORT).show();
            clearForm();
            NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_create_class, true).build();
            NavHostFragment.findNavController(this).navigate(R.id.nav_list_classes, null, navOptions);
        } else {
            Toast.makeText(getContext(), "Tạo lớp thất bại", Toast.LENGTH_SHORT).show();
        }
    }


    private void clearForm() {
        binding.etClassName.setText("");
        binding.etSubject.setText("");
        binding.etTeacher.setText("");
        binding.etSemester.setText("");
        binding.etRoom.setText("");
        binding.etStartDate.setText("");
        binding.etEndDate.setText("");
        viewModel.clear();
    }

    private void showDatePickerDialog(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", dayOfMonth, month + 1, year);
                    editText.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }
}
