package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
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
        ItemStudentSelectedGridBinding binding = ItemStudentSelectedGridBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
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

            // Unify style with "Selected" state in the picker
            int primaryColor = ContextCompat.getColor(itemView.getContext(), R.color.primary);
            int whiteColor = ContextCompat.getColor(itemView.getContext(), android.R.color.white);

            // Always show as "Selected" state (Primary background, white text/icons)
            binding.getRoot().setCardBackgroundColor(primaryColor);
            binding.tvStudentName.setTextColor(whiteColor);
            binding.tvStudentId.setTextColor(whiteColor);

            // Icon tints
            binding.ivStudentAvatar.setImageTintList(ColorStateList.valueOf(whiteColor));
            binding.btnRemoveStudent.setImageTintList(ColorStateList.valueOf(whiteColor));
        }
    }

    public interface OnStudentRemovedListener {
        void onStudentRemoved(User student);
    }
}
