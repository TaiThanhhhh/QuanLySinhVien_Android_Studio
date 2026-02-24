package com.example.quanlysinhvien.ui.admin.attendance;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.AttendanceStatus;
import com.example.quanlysinhvien.databinding.GridItemAttendanceManagementBinding;

import java.util.function.Consumer;

public class AttendanceManagementAdapter extends ListAdapter<AttendanceStatus, AttendanceManagementAdapter.ViewHolder> {

    private final Consumer<AttendanceStatus> onClickListener;

    public AttendanceManagementAdapter(Consumer<AttendanceStatus> onClickListener) {
        super(new DiffUtil.ItemCallback<AttendanceStatus>() {
            @Override
            public boolean areItemsTheSame(@NonNull AttendanceStatus oldItem, @NonNull AttendanceStatus newItem) {
                return oldItem.getStudent().getId() == newItem.getStudent().getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull AttendanceStatus oldItem, @NonNull AttendanceStatus newItem) {
                return oldItem.getStatus().equals(newItem.getStatus());
            }
        });
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GridItemAttendanceManagementBinding binding = GridItemAttendanceManagementBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceStatus item = getItem(position);
        holder.bind(item, onClickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final GridItemAttendanceManagementBinding binding;

        public ViewHolder(GridItemAttendanceManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final AttendanceStatus item, final Consumer<AttendanceStatus> onClickListener) {
            Context context = itemView.getContext();
            binding.tvStudentNameManage.setText(item.getStudent().getName());
            binding.tvStudentIdManage.setText(item.getStudent().getMssv());

            int statusColorRes;
            int statusTextRes;

            switch (item.getStatus()) {
                case "ON_TIME":
                    statusTextRes = R.string.status_on_time;
                    statusColorRes = R.color.status_ongoing;
                    break;
                case "LATE":
                    statusTextRes = R.string.status_late;
                    statusColorRes = R.color.status_upcoming; // Using upcoming for orange
                    break;
                case "EXCUSED":
                    statusTextRes = R.string.status_excused;
                    statusColorRes = R.color.primary; // Or any other distinct color
                    break;
                default: // ABSENT
                    statusTextRes = R.string.status_absent;
                    statusColorRes = R.color.status_locked; // Using locked for red
                    break;
            }
            binding.tvStatusManage.setText(context.getString(statusTextRes));
            binding.tvStatusManage.setTextColor(ContextCompat.getColor(context, statusColorRes));

            itemView.setOnClickListener(v -> onClickListener.accept(item));
        }
    }
}
