package com.example.quanlysinhvien.ui.student.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.AttendanceRecord;

public class AttendanceDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvClassTitle = view.findViewById(R.id.tv_class_title_detail);
        TextView tvTimestamp = view.findViewById(R.id.tv_timestamp_detail);
        TextView tvStatus = view.findViewById(R.id.tv_status_detail);

        if (getArguments() != null) {
            String classTitle = getArguments().getString("class_title");
            long timestamp = getArguments().getLong("timestamp");
            String status = getArguments().getString("status");

            tvClassTitle.setText(classTitle);
            tvTimestamp.setText(String.valueOf(timestamp));
            tvStatus.setText(status);
        }
    }
}
