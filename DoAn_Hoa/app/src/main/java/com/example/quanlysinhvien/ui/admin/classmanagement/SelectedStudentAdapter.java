package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.databinding.ItemStudentSelectedGridBinding;

public class SelectedStudentAdapter extends ListAdapter<User, SelectedStudentAdapter.ViewHolder> {
    private final OnStudentRemovedListener listener;

    public SelectedStudentAdapter(OnStudentRemovedListener listener) {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getName().equals(newItem.getName()) && oldItem.getMssv().equals(newItem.getMssv());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentSelectedGridBinding binding = ItemStudentSelectedGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentSelectedGridBinding binding;

        ViewHolder(ItemStudentSelectedGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.btnRemoveStudent.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onStudentRemoved(getItem(position));
                }
            });
        }

        void bind(User student) {
            binding.tvStudentName.setText(student.getName());
            binding.tvStudentId.setText(student.getMssv());
        }
    }

    public interface OnStudentRemovedListener {
        void onStudentRemoved(User student);
    }
}
