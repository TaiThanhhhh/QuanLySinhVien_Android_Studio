package com.example.quanlysinhvien.ui.student.dashboard;

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
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.databinding.FragmentStudentDashboardBinding;

public class StudentDashboardFragment extends Fragment {

    private FragmentStudentDashboardBinding binding;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStudentDashboardBinding.inflate(inflater, container, false);
        userRepository = new UserRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserProfile();

        // Set up listeners for the cards
        binding.cardScanQr.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_student_dashboard_to_scan_qr)
        );

        binding.cardViewHistory.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_student_dashboard_to_history)
        );
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}