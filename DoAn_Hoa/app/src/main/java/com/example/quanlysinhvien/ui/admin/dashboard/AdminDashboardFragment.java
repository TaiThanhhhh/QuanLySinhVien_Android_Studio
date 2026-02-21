package com.example.quanlysinhvien.ui.admin.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.repo.ClassRepository;
import com.example.quanlysinhvien.data.repo.UserRepository;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalClasses;
    private TextView tvTotalStudents;
    private TextView tvTodayAttendance;
    private ClassRepository classRepository;
    private UserRepository userRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        classRepository = new ClassRepository(requireContext());
        userRepository = new UserRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        tvTotalClasses = view.findViewById(R.id.tv_total_classes);
        tvTotalStudents = view.findViewById(R.id.tv_total_students);
        tvTodayAttendance = view.findViewById(R.id.tv_today_attendance);

        // Quick Actions - Updated to navigate to nested graphs
        view.findViewById(R.id.card_action_create_class).setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.nav_class_management));

        view.findViewById(R.id.card_action_students).setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.nav_student_management));

        view.findViewById(R.id.card_action_reports).setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.nav_reports));

        view.findViewById(R.id.card_action_view_classes).setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.nav_class_management));

        loadDashboardStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardStats();
    }

    private void loadDashboardStats() {
        int classCount = classRepository.listClasses("").size();
        tvTotalClasses.setText(String.valueOf(classCount));

        int studentCount = userRepository.getAllStudents("").size();
        tvTotalStudents.setText(String.valueOf(studentCount));

        tvTodayAttendance.setText("-");
    }
}
