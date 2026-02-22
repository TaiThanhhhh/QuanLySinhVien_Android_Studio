package com.example.quanlysinhvien.ui.student.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.StatusCount;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.AttendanceRepository;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.databinding.FragmentStudentDashboardBinding;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.List;

public class StudentDashboardFragment extends Fragment {

    private FragmentStudentDashboardBinding binding;
    private UserRepository userRepository;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentDashboardBinding.inflate(inflater, container, false);
        userRepository = new UserRepository(requireContext());
        attendanceRepository = new AttendanceRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserProfile();
        loadAttendanceStats();

        // Set up listeners for the cards
        binding.cardScanQr.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.action_student_dashboard_to_scan_qr));

        binding.cardViewHistory.setOnClickListener(
                v -> NavHostFragment.findNavController(this).navigate(R.id.action_student_dashboard_to_history));

        binding.cardFaceEnrollment.setOnClickListener(
                v -> NavHostFragment.findNavController(this)
                        .navigate(R.id.action_student_dashboard_to_face_enrollment));
    }

    private void loadUserProfile() {
        long userId = sessionManager.getUserId();
        if (userId != -1) {
            User user = userRepository.getUserById(userId);
            if (user != null) {
                binding.tvStudentName.setText(user.getName());
                binding.tvStudentId.setText(user.getMssv());
            }
        }
    }

    private void loadAttendanceStats() {
        long userId = sessionManager.getUserId();
        if (userId == -1)
            return;

        List<StatusCount> stats = attendanceRepository.getStudentAttendanceStats(userId);
        if (stats.isEmpty()) {
            binding.pieChartAttendance.setNoDataText("Chưa có dữ liệu điểm danh");
            binding.pieChartAttendance.invalidate();
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (StatusCount stat : stats) {
            String label = stat.getStatus();
            int color = Color.GRAY;

            // Translate status to Vietnamese and set colors
            switch (stat.getStatus()) {
                case "PRESENT":
                case "ON_TIME":
                    label = "Đúng giờ";
                    color = Color.parseColor("#4CAF50"); // Green
                    break;
                case "LATE":
                    label = "Trễ";
                    color = Color.parseColor("#FFC107"); // Amber
                    break;
                case "ABSENT":
                    label = "Vắng";
                    color = Color.parseColor("#F44336"); // Red
                    break;
                case "EXCUSED":
                    label = "Có phép";
                    color = Color.parseColor("#2196F3"); // Blue
                    break;
            }
            entries.add(new PieEntry(stat.getCount(), label));
            colors.add(color);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData data = new PieData(dataSet);
        binding.pieChartAttendance.setData(data);
        binding.pieChartAttendance.getDescription().setEnabled(false);
        binding.pieChartAttendance.getLegend().setEnabled(true);
        binding.pieChartAttendance.setCenterText("Tỷ lệ chuyên cần");
        binding.pieChartAttendance.setCenterTextSize(16f);
        binding.pieChartAttendance.setHoleRadius(60f);
        binding.pieChartAttendance.animateY(1000);
        binding.pieChartAttendance.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}