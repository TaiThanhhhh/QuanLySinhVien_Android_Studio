package com.example.quanlysinhvien.ui.admin.studentmanagement;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.databinding.GridItemStudentManagementBinding;

import java.util.function.Consumer;

public class StudentManagementAdapter extends ListAdapter<User, StudentManagementAdapter.StudentViewHolder> {

    private final Consumer<User> onEdit;
    private final Consumer<User> onDelete;

    public StudentManagementAdapter(Consumer<User> onEdit, Consumer<User> onDelete) {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getName().equals(newItem.getName()) &&
                        oldItem.getMssv().equals(newItem.getMssv()) &&
                        oldItem.isPasswordNeedsReset() == newItem.isPasswordNeedsReset();
            }
        });
        this.onEdit = onEdit;
        this.onDelete = onDelete;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GridItemStudentManagementBinding binding = GridItemStudentManagementBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new StudentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        User student = getItem(position);
        holder.bind(student, onEdit, onDelete);
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final GridItemStudentManagementBinding binding;

        public StudentViewHolder(GridItemStudentManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(User student, Consumer<User> onEdit, Consumer<User> onDelete) {
            binding.tvStudentName.setText(student.getName());
            binding.tvStudentId.setText(student.getMssv());
            if (student.isPasswordNeedsReset()) {
                binding.tvPasswordStatus.setText("Chưa đổi mật khẩu");
                binding.tvPasswordStatus.setTextColor(Color.RED);
            } else {
                binding.tvPasswordStatus.setText("Đã đổi mật khẩu");
                binding.tvPasswordStatus.setTextColor(Color.GREEN);
            }
            binding.btnEditStudent.setOnClickListener(v -> onEdit.accept(student));
            binding.btnDeleteStudent.setOnClickListener(v -> onDelete.accept(student));
        }
    }
}
