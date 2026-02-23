package com.example.quanlysinhvien.ui.admin.classlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.ClassModel;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.ClassRepository;
import com.example.quanlysinhvien.data.repo.EnrollmentRepository;
import com.example.quanlysinhvien.databinding.FragmentClassDetailBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassDetailFragment extends Fragment {

    private FragmentClassDetailBinding binding;
    private ClassRepository classRepository;
    private EnrollmentRepository enrollmentRepository;
    private StudentInClassAdapter adapter;
    private long classId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        classRepository = new ClassRepository(requireContext());
        enrollmentRepository = new EnrollmentRepository(requireContext());
        if (getArguments() != null) {
            classId = getArguments().getLong("class_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentClassDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        loadClassDetails();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        adapter = new StudentInClassAdapter(); // Listener removed
        int spanCount = getResources().getInteger(R.integer.student_grid_span_count);
        binding.rvEnrolledStudents.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        binding.rvEnrolledStudents.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnManageAttendance.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putLong("class_id", classId);
            NavHostFragment.findNavController(this).navigate(R.id.action_detail_to_manageAttendance, bundle);
        });

        binding.btnGenerateQr.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putLong("class_id", classId);
            NavHostFragment.findNavController(this).navigate(R.id.action_detail_to_generate_qr, bundle);
        });
    }

    private void loadClassDetails() {
        ClassModel classModel = classRepository.getClassById(classId);
        if (classModel == null)
            return;

        binding.tvClassNameDetail.setText(classModel.getTitle());
        binding.tvClassSubjectDetail.setText(classModel.getSubject());
        binding.tvTeacherDetail.setText(classModel.getTeacher());
        binding.tvSemesterDetail.setText(classModel.getSemester());
        binding.tvRoomDetail.setText(classModel.getRoom());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String startDate = (classModel.getStartDate() != null && classModel.getStartDate() > 0)
                ? sdf.format(new Date(classModel.getStartDate()))
                : "N/A";
        String endDate = (classModel.getEndDate() != null && classModel.getEndDate() > 0)
                ? sdf.format(new Date(classModel.getEndDate()))
                : "N/A";
        binding.tvStartEndDateDetail.setText(String.format("%s - %s", startDate, endDate));

        ClassModel.Status status = classModel.getCalculatedStatus();
        int statusColorRes, statusTextRes;
        switch (status) {
            case ONGOING:
                statusTextRes = R.string.class_status_ongoing;
                statusColorRes = R.color.status_ongoing;
                break;
            case UPCOMING:
                statusTextRes = R.string.class_status_upcoming;
                statusColorRes = R.color.status_upcoming;
                break;
            case FINISHED:
                statusTextRes = R.string.class_status_finished;
                statusColorRes = R.color.status_finished;
                break;
            default:
                statusTextRes = R.string.class_status_locked;
                statusColorRes = R.color.status_locked;
                break;
        }
        binding.tvClassStatusDetail.setText(getString(statusTextRes));
        binding.tvClassStatusDetail.setTextColor(ContextCompat.getColor(requireContext(), statusColorRes));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStudents();
    }

    private void loadStudents() {
        List<User> studentList = enrollmentRepository.getStudentsInClass(classId);
        binding.tvStudentCount.setText(String.format(Locale.getDefault(), "%d sinh viên", studentList.size()));
        adapter.submitList(studentList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
