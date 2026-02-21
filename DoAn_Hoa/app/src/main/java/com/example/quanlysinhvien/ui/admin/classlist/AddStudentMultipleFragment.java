package com.example.quanlysinhvien.ui.admin.classlist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.EnrollmentRepository;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.ui.admin.classmanagement.StudentSelectionAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddStudentMultipleFragment extends Fragment {

    private UserRepository userRepository;
    private EnrollmentRepository enrollmentRepository;
    private StudentSelectionAdapter adapter;
    private long classId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        enrollmentRepository = new EnrollmentRepository(requireContext());
        if (getArguments() != null) {
            classId = getArguments().getLong("class_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_student_multiple, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvAllStudents = view.findViewById(R.id.rv_all_students);
        rvAllStudents.setLayoutManager(new GridLayoutManager(getContext(), 2));

        List<User> allStudents = userRepository.getAllStudents(null);
        List<User> enrolledStudents = enrollmentRepository.getStudentsInClass(classId);
        List<Long> enrolledStudentIds = enrolledStudents.stream().map(User::getId).collect(Collectors.toList());

        List<User> studentsNotInClass = allStudents.stream()
                .filter(student -> !enrolledStudentIds.contains(student.getId()))
                .collect(Collectors.toList());

        // LOGGING: Check the size of the list to be displayed
        Log.d("AddStudentFragment", "Total students in DB: " + allStudents.size());
        Log.d("AddStudentFragment", "Students already in class: " + enrolledStudents.size());
        Log.d("AddStudentFragment", "Number of students to show: " + studentsNotInClass.size());

        adapter = new StudentSelectionAdapter(new ArrayList<>());
        adapter.submitList(studentsNotInClass);
        rvAllStudents.setAdapter(adapter);

        Button btnAddSelectedStudents = view.findViewById(R.id.btn_add_selected_students);
        btnAddSelectedStudents.setOnClickListener(v -> {
            for (User student : adapter.getSelectedStudents()) {
                enrollmentRepository.enrollStudent(classId, student.getId());
            }
            Toast.makeText(getContext(), "Đã thêm sinh viên vào lớp", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        });
    }
}
