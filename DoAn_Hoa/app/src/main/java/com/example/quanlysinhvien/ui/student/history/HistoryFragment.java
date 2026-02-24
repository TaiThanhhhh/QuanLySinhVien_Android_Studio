package com.example.quanlysinhvien.ui.student.history;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.AttendanceRecord;
import com.example.quanlysinhvien.data.repo.AttendanceRepository;
import com.example.quanlysinhvien.databinding.FragmentHistoryBinding;
import com.example.quanlysinhvien.databinding.ItemAttendanceHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private HistoryAdapter adapter;
    private FragmentHistoryBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attendanceRepository = new AttendanceRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(new ArrayList<>(), record -> {
            // Handle item click if needed, maybe navigate to a detail screen
        });
        binding.rvHistory.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        long studentId = sessionManager.getUserId();
        List<AttendanceRecord> updatedHistory = attendanceRepository.getAttendanceHistoryForStudent(studentId);
        adapter.updateData(updatedHistory);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<AttendanceRecord> records;
        private final OnItemClickListener listener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss", Locale.getDefault());

        public interface OnItemClickListener {
            void onItemClick(AttendanceRecord record);
        }

        public HistoryAdapter(List<AttendanceRecord> records, OnItemClickListener listener) {
            this.records = records;
            this.listener = listener;
        }

        public void updateData(List<AttendanceRecord> newRecords) {
            this.records = newRecords;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAttendanceHistoryBinding itemBinding = ItemAttendanceHistoryBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = records.get(position);
            holder.bind(record, dateFormat, listener);
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ItemAttendanceHistoryBinding binding;

            ViewHolder(ItemAttendanceHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(AttendanceRecord record, SimpleDateFormat dateFormat, OnItemClickListener listener) {
                Context context = itemView.getContext();

                binding.tvClassName.setText(record.getClassTitle());
                binding.tvClassSubject.setText(record.getSubject());
                binding.tvLecturerName
                        .setText(record.getTeacherName() != null ? record.getTeacherName() : "Giảng viên: N/A");
                binding.tvAttendanceDate.setText(dateFormat.format(new Date(record.getSessionTime())));

                int statusTextRes;
                int statusColorRes;

                switch (record.getStatus()) {
                    case "ON_TIME":
                        statusTextRes = R.string.status_on_time;
                        statusColorRes = R.color.status_ongoing; // Green
                        break;
                    case "LATE":
                        statusTextRes = R.string.status_late;
                        statusColorRes = R.color.status_upcoming; // Yellow
                        break;
                    case "ABSENT":
                        statusTextRes = R.string.status_absent;
                        statusColorRes = R.color.status_locked; // Red
                        break;
                    case "EXCUSED":
                        statusTextRes = R.string.status_excused;
                        statusColorRes = R.color.primary; // Blue
                        break;
                    default:
                        statusTextRes = R.string.status_absent;
                        statusColorRes = R.color.status_locked;
                        break;
                }

                binding.chipAttendanceStatus.setText(context.getString(statusTextRes));
                binding.chipAttendanceStatus.setChipBackgroundColor(
                        ColorStateList.valueOf(ContextCompat.getColor(context, statusColorRes)));

                itemView.setOnClickListener(v -> listener.onItemClick(record));
            }
        }
    }
}
