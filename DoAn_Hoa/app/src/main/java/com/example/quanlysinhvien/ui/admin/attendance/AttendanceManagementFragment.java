package com.example.quanlysinhvien.ui.admin.attendance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.AttendanceSession;
import com.example.quanlysinhvien.data.model.AttendanceStatus;
import com.example.quanlysinhvien.data.model.ClassModel;
import com.example.quanlysinhvien.data.repo.AttendanceRepository;
import com.example.quanlysinhvien.data.repo.AttendanceSessionRepository;
import com.example.quanlysinhvien.data.repo.ClassRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AttendanceManagementFragment extends Fragment {

    private long initialClassId = -1;
    private AttendanceRepository attendanceRepository;
    private AttendanceSessionRepository sessionRepository;
    private ClassRepository classRepository;
    private SessionManager sessionManager;

    private AutoCompleteTextView classAutocomplete, sessionAutocomplete;
    private RecyclerView recyclerView;
    private AttendanceManagementAdapter adapter;
    private ChipGroup chipGroup;

    private List<AttendanceStatus> fullList = new ArrayList<>();
    private List<AttendanceSession> sessionList = new ArrayList<>();
    private List<ClassModel> classList = new ArrayList<>();

    private ClassModel selectedClass;
    private AttendanceSession selectedSession;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attendanceRepository = new AttendanceRepository(requireContext());
        sessionRepository = new AttendanceSessionRepository(requireContext());
        classRepository = new ClassRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        if (getArguments() != null) {
            initialClassId = getArguments().getLong("class_id", -1L);
        }
    }

    private void updateAttendance(String newStatus, AttendanceStatus item) {
        long sessionId = selectedSession.getId();
        long adminId = sessionManager.getUserId();

        boolean success = attendanceRepository.updateAttendanceStatus(sessionId, item.getStudent().getId(), newStatus, adminId);
        if (success) {
            Toast.makeText(getContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
            loadAttendanceData(sessionId, selectedClass.getId());
        } else {
            Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_management_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        classAutocomplete = view.findViewById(R.id.class_autocomplete);
        sessionAutocomplete = view.findViewById(R.id.session_autocomplete);
        TextInputLayout classMenu = view.findViewById(R.id.class_menu);
        recyclerView = view.findViewById(R.id.rv_attendance_management);
        chipGroup = view.findViewById(R.id.chip_group_filter);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new AttendanceManagementAdapter(this::onStudentClick);
        recyclerView.setAdapter(adapter);

        setupClassDropdown();
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> filterList());

        if (initialClassId != -1) {
            classMenu.setEnabled(false);
        }
    }

    private void setupClassDropdown() {
        classList = classRepository.listClasses(null);
        ArrayAdapter<ClassModel> classAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, classList);
        classAutocomplete.setAdapter(classAdapter);

        classAutocomplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedClass = classList.get(position);
            sessionAutocomplete.setText("", false);
            selectedSession = null;
            setupSessionDropdown(selectedClass.getId());
        });

        if (initialClassId != -1) {
            for (int i = 0; i < classList.size(); i++) {
                if (classList.get(i).getId() == initialClassId) {
                    selectedClass = classList.get(i);
                    classAutocomplete.setText(selectedClass.toString(), false);
                    setupSessionDropdown(selectedClass.getId());
                    break;
                }
            }
        }
    }

    private void setupSessionDropdown(long selectedClassId) {
        sessionList = sessionRepository.getSessionsForClass(selectedClassId);
        ArrayAdapter<AttendanceSession> sessionAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sessionList);
        sessionAutocomplete.setAdapter(sessionAdapter);

        sessionAutocomplete.setOnItemClickListener((parent, view, position, id) -> {
            selectedSession = sessionList.get(position);
            loadAttendanceData(selectedSession.getId(), selectedClassId);
        });
    }

    private void loadAttendanceData(long sessionId, long currentClassId) {
        fullList = attendanceRepository.getAttendanceStatusForSession(sessionId, currentClassId);
        filterList();
    }

    private void filterList() {
        int checkedId = chipGroup.getCheckedChipId();
        List<AttendanceStatus> filteredList;
        if (checkedId == R.id.chip_not_present) {
            filteredList = fullList.stream()
                    .filter(s -> "ABSENT".equals(s.getStatus()))
                    .collect(Collectors.toList());
        } else { // chip_all or default
            filteredList = new ArrayList<>(fullList);
        }
        adapter.submitList(filteredList);
    }

    private void onStudentClick(AttendanceStatus item) {
        if (selectedSession == null || selectedClass == null) {
            Toast.makeText(getContext(), "Vui lòng chọn lớp và buổi học", Toast.LENGTH_SHORT).show();
            return;
        }

        UpdateAttendanceStatusDialogFragment dialog = UpdateAttendanceStatusDialogFragment.newInstance(item.getStudent().getName(), item.getStatus());
        dialog.setOnResultListener(newStatus -> {
            if (newStatus != null && !newStatus.isEmpty()) {
                updateAttendance(newStatus, item);
            }
        });
        dialog.show(getParentFragmentManager(), "UpdateAttendanceStatusDialog");
    }
}
