package com.example.quanlysinhvien.ui.admin.classlist;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.databinding.GridItemStudentInClassBinding;

public class StudentInClassAdapter extends ListAdapter<User, StudentInClassAdapter.ViewHolder> {

    public StudentInClassAdapter() {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                String oldMssv = oldItem.getMssv() != null ? oldItem.getMssv() : "";
                String newMssv = newItem.getMssv() != null ? newItem.getMssv() : "";
                return oldItem.getName().equals(newItem.getName()) && oldMssv.equals(newMssv);
            }
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GridItemStudentInClassBinding binding = GridItemStudentInClassBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = getItem(position);
        holder.bind(student);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final GridItemStudentInClassBinding binding;

        public ViewHolder(GridItemStudentInClassBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User student) {
            binding.tvStudentName.setText(student.getName());
            binding.tvStudentId.setText(student.getMssv());
        }
    }
}
