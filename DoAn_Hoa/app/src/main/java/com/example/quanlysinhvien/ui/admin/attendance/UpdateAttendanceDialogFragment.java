package com.example.quanlysinhvien.ui.admin.attendance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.quanlysinhvien.R;
import com.google.android.material.button.MaterialButton;

public class UpdateAttendanceDialogFragment extends DialogFragment {

    public static final String TAG = "UpdateAttendanceDialog";
    public static final String REQUEST_KEY = "attendance_update_request";
    public static final String KEY_STATUS = "new_status";
    private static final String ARG_STUDENT_NAME = "student_name";
    private static final String ARG_CURRENT_STATUS = "current_status";

    public static UpdateAttendanceDialogFragment newInstance(String studentName, String currentStatus) {
        UpdateAttendanceDialogFragment fragment = new UpdateAttendanceDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_NAME, studentName);
        args.putString(ARG_CURRENT_STATUS, currentStatus);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate the custom layout
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_update_attendance, null);

        // Get arguments
        String studentName = getArguments().getString(ARG_STUDENT_NAME);
        String currentStatus = getArguments().getString(ARG_CURRENT_STATUS);

        // Setup views
        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        RadioGroup rgStatus = view.findViewById(R.id.rg_status);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);

        tvTitle.setText(String.format("Cập nhật cho %s", studentName));

        // Pre-select the current status
        switch (currentStatus) {
            case "ON_TIME":
                rgStatus.check(R.id.rb_on_time);
                break;
            case "LATE":
                rgStatus.check(R.id.rb_late);
                break;
            case "ABSENT":
                rgStatus.check(R.id.rb_absent);
                break;
            case "EXCUSED":
                rgStatus.check(R.id.rb_excused);
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view);
        AlertDialog dialog = builder.create();

        // Set button listeners
        btnConfirm.setOnClickListener(v -> {
            int selectedId = rgStatus.getCheckedRadioButtonId();
            RadioButton selectedRadioButton = view.findViewById(selectedId);
            String newStatus = getStatusFromRadioButton(selectedRadioButton);

            // Send the result back to the parent fragment
            Bundle result = new Bundle();
            result.putString(KEY_STATUS, newStatus);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Disable the default background to show the MaterialCardView corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        return dialog;
    }

    private String getStatusFromRadioButton(RadioButton radioButton) {
        if (radioButton.getId() == R.id.rb_on_time) {
            return "ON_TIME";
        } else if (radioButton.getId() == R.id.rb_late) {
            return "LATE";
        } else if (radioButton.getId() == R.id.rb_absent) {
            return "ABSENT";
        } else if (radioButton.getId() == R.id.rb_excused) {
            return "EXCUSED";
        } else {
            return ""; // Should not happen
        }
    }
}
