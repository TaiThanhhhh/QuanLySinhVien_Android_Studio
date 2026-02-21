package com.example.quanlysinhvien.ui.admin.attendance;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.ui.base.BaseDialogFragment;

import java.util.function.Consumer;

public class UpdateAttendanceStatusDialogFragment extends BaseDialogFragment {

    private static final String ARG_STUDENT_NAME = "student_name";
    private static final String ARG_CURRENT_STATUS = "current_status";

    private Consumer<String> resultConsumer;

    public static UpdateAttendanceStatusDialogFragment newInstance(String studentName, String currentStatus) {
        UpdateAttendanceStatusDialogFragment fragment = new UpdateAttendanceStatusDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STUDENT_NAME, studentName);
        args.putString(ARG_CURRENT_STATUS, currentStatus);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnResultListener(Consumer<String> consumer) {
        this.resultConsumer = consumer;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_confirmation;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            String studentName = args.getString(ARG_STUDENT_NAME, "");
            String currentStatus = args.getString(ARG_CURRENT_STATUS, "");

            setDialogIcon(R.drawable.ic_baseline_edit_24);
            setDialogTitle("Cập nhật cho " + studentName);
            setDialogMessage("Chọn trạng thái điểm danh mới:");

            ViewGroup customContainer = (ViewGroup) getCustomContentView();
            LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_attendance_content, customContainer, true);
            customContainer.setVisibility(View.VISIBLE);

            RadioGroup rgStatus = customContainer.findViewById(R.id.rg_status);

            switch (currentStatus) {
                case "ON_TIME": rgStatus.check(R.id.rb_on_time); break;
                case "LATE": rgStatus.check(R.id.rb_late); break;
                case "ABSENT": rgStatus.check(R.id.rb_absent); break;
                case "EXCUSED": rgStatus.check(R.id.rb_excused); break;
            }
        }

        return dialog;
    }

    @Override
    protected void onPositiveClicked() {
        if (resultConsumer != null) {
            View customContentView = getCustomContentView();
            if (customContentView != null) {
                RadioGroup rg = customContentView.findViewById(R.id.rg_status);
                int selectedId = rg.getCheckedRadioButtonId();
                String newStatus = "";
                if (selectedId == R.id.rb_on_time) newStatus = "ON_TIME";
                else if (selectedId == R.id.rb_late) newStatus = "LATE";
                else if (selectedId == R.id.rb_absent) newStatus = "ABSENT";
                else if (selectedId == R.id.rb_excused) newStatus = "EXCUSED";
                
                resultConsumer.accept(newStatus);
            }
        }
        dismiss();
    }
}
