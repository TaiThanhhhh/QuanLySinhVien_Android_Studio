package com.example.quanlysinhvien.ui.admin.studentmanagement;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
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
    private final Consumer<User> onResetFace;

    public StudentManagementAdapter(Consumer<User> onEdit, Consumer<User> onDelete, Consumer<User> onResetFace) {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getName().equals(newItem.getName())
                        && oldItem.getMssv().equals(newItem.getMssv())
                        && oldItem.isPasswordNeedsReset() == newItem.isPasswordNeedsReset()
                        && TextUtils.equals(oldItem.getFaceTemplate(), newItem.getFaceTemplate());
            }
        });
        this.onEdit = onEdit;
        this.onDelete = onDelete;
        this.onResetFace = onResetFace;
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
        holder.bind(student, onEdit, onDelete, onResetFace);
    }

    static class StudentViewHolder extends RecyclerView.ViewHolder {
        private final GridItemStudentManagementBinding binding;

        StudentViewHolder(GridItemStudentManagementBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User student, Consumer<User> onEdit, Consumer<User> onDelete, Consumer<User> onResetFace) {
            binding.tvStudentName.setText(student.getName());
            binding.tvStudentId.setText(student.getMssv());

            if (student.isPasswordNeedsReset()) {
                binding.tvStudentPassword.setText("Chưa đổi mật khẩu");
                binding.tvStudentPassword.setTextColor(Color.RED);
            } else {
                binding.tvStudentPassword.setText("Đã đổi mật khẩu");
                binding.tvStudentPassword.setTextColor(Color.parseColor("#4CAF50"));
            }
            binding.tvStudentPassword.setVisibility(View.VISIBLE);

            boolean hasFaceTemplate = !TextUtils.isEmpty(student.getFaceTemplate());
            binding.btnResetFace.setEnabled(hasFaceTemplate);
            binding.btnResetFace.setAlpha(hasFaceTemplate ? 1f : 0.4f);

            binding.btnEditStudent.setOnClickListener(v -> onEdit.accept(student));
            binding.btnDeleteStudent.setOnClickListener(v -> onDelete.accept(student));
            binding.btnResetFace.setOnClickListener(v -> {
                if (hasFaceTemplate) {
                    onResetFace.accept(student);
                }
            });
        }
    }
}
