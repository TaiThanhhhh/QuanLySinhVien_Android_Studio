package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.databinding.GridItemStudentSelectionBinding;
import java.util.ArrayList;
import java.util.List;

public class StudentSelectionAdapter extends ListAdapter<User, StudentSelectionAdapter.ViewHolder> {
    private final List<User> selectedStudents;

    public StudentSelectionAdapter(List<User> initiallySelected) {
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
        this.selectedStudents = new ArrayList<>(initiallySelected);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GridItemStudentSelectionBinding binding = GridItemStudentSelectionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public List<User> getSelectedStudents() {
        return selectedStudents;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final GridItemStudentSelectionBinding binding;

        ViewHolder(GridItemStudentSelectionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    User student = getItem(position);

                    // Toggle selection status in our list of selected students
                    if (selectedStudents.stream().anyMatch(s -> s.getId() == student.getId())) {
                        selectedStudents.removeIf(s -> s.getId() == student.getId());
                    } else {
                        selectedStudents.add(student);
                    }
                    // Notify the adapter that this item has changed, so it can be re-bound
                    notifyItemChanged(position);
                }
            });
        }

        void bind(User student) {
            binding.tvStudentNameSelection.setText(student.getName());
            binding.tvStudentIdSelection.setText(student.getMssv());

            // Set the checkbox state from our source of truth, the selectedStudents list
            binding.checkboxStudentSelect.setChecked(selectedStudents.stream().anyMatch(s -> s.getId() == student.getId()));
        }
    }
}
