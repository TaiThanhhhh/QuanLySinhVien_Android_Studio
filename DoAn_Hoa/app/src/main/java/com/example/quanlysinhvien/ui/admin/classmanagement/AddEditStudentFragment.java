package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.databinding.FragmentAddEditStudentBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AddEditStudentFragment extends Fragment {

    private FragmentAddEditStudentBinding binding;
    private UserRepository userRepository;
    private StudentSelectionAdapter adapter;
    private List<User> allStudents = new ArrayList<>();
    private CreateClassViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        // Share ViewModel with CreateClassFragment via requireActivity()
        viewModel = new ViewModelProvider(requireActivity()).get(CreateClassViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditStudentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Always read the CURRENT selection from ViewModel (not stale Bundle args)
        List<User> currentlySelected = viewModel.getStudentList().getValue();
        if (currentlySelected == null)
            currentlySelected = new ArrayList<>();

        setupRecyclerView(new ArrayList<>(currentlySelected));
        setupSearch();

        binding.fabAddStudents.setOnClickListener(v -> handleConfirm());

        binding.btnToggleSelectAll.setOnClickListener(v -> {
            if (adapter == null)
                return;
            if (adapter.isAllSelected()) {
                adapter.deselectAll();
            } else {
                adapter.selectAll();
            }
            updateToggleSelectAllButton();
        });

        // Initialize button state
        updateToggleSelectAllButton();
    }

    private void updateToggleSelectAllButton() {
        if (adapter == null)
            return;
        boolean allSelected = adapter.isAllSelected();
        if (allSelected) {
            binding.btnToggleSelectAll.setText("Bỏ chọn hết");
            binding.btnToggleSelectAll.setIconResource(R.drawable.ic_baseline_delete_24);
        } else {
            binding.btnToggleSelectAll.setText("Chọn tất cả");
            binding.btnToggleSelectAll.setIconResource(R.drawable.ic_baseline_check_24);
        }
    }

    private void setupRecyclerView(List<User> initiallySelected) {
        // Reverted to Grid layout as requested by the user
        binding.rvStudentsToAdd.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));

        // Merge: all students from DB, but mark already-selected ones as checked
        allStudents = userRepository.getAllStudents(null);
        if (allStudents == null)
            allStudents = new ArrayList<>();

        adapter = new StudentSelectionAdapter(initiallySelected);
        adapter.setOnSelectionChangedListener(this::updateToggleSelectAllButton);
        binding.rvStudentsToAdd.setAdapter(adapter);
        adapter.submitList(new ArrayList<>(allStudents));
        adapter.setFullList(allStudents); // Allow lookup of filtered-out selected items
    }

    private void setupSearch() {
        binding.etSearchStudent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void filterStudents(String query) {
        if (allStudents == null)
            return;
        List<User> filtered;
        if (query == null || query.trim().isEmpty()) {
            filtered = new ArrayList<>(allStudents);
        } else {
            String q = query.toLowerCase().trim();
            filtered = allStudents.stream()
                    .filter(s -> s.getName().toLowerCase().contains(q) ||
                            (s.getMssv() != null && s.getMssv().contains(q)))
                    .collect(Collectors.toList());
        }
        adapter.submitList(filtered);
    }

    private void handleConfirm() {
        List<User> selectedStudents = adapter.getSelectedStudents();
        if (selectedStudents.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng chọn ít nhất một sinh viên", Toast.LENGTH_SHORT).show();
            return;
        }
        // Update ViewModel directly — no FragmentResult needed
        viewModel.setStudentList(selectedStudents);
        Toast.makeText(getContext(), "Đã thêm sinh viên thành công", Toast.LENGTH_SHORT).show();
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
