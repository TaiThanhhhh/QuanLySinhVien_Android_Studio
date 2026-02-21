package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.databinding.FragmentAddEditStudentBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddEditStudentFragment extends Fragment {

    private static final String TAG = "AddEditStudentFragment";
    private FragmentAddEditStudentBinding binding;
    private UserRepository userRepository;
    private StudentSelectionAdapter adapter;
    private List<User> allStudents;
    private ArrayList<User> initiallySelectedStudents = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        if (getArguments() != null) {
            ArrayList<User> selected = getArguments().getParcelableArrayList(CreateClassFragment.BUNDLE_KEY);
            if (selected != null) {
                initiallySelectedStudents.addAll(selected);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditStudentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearch();

        binding.fabAddStudents.setOnClickListener(v -> handleAddStudents());
    }

    private void setupRecyclerView() {
        adapter = new StudentSelectionAdapter(initiallySelectedStudents);
        binding.rvStudentsToAdd.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvStudentsToAdd.setAdapter(adapter);

        allStudents = userRepository.getAllStudents(null);
        adapter.submitList(allStudents);
    }

    private void setupSearch() {
        binding.etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterStudents(String query) {
        if (allStudents == null) return;
        List<User> filteredList = allStudents.stream()
                .filter(student -> student.getName().toLowerCase().contains(query.toLowerCase()) ||
                                   (student.getMssv() != null && student.getMssv().contains(query)))
                .collect(Collectors.toList());
        adapter.submitList(filteredList);
    }

    private void handleAddStudents() {
        List<User> selectedStudents = adapter.getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn ít nhất một sinh viên", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle result = new Bundle();
        result.putParcelableArrayList(CreateClassFragment.BUNDLE_KEY, new ArrayList<>(selectedStudents));
        getParentFragmentManager().setFragmentResult(CreateClassFragment.REQUEST_KEY, result);
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
