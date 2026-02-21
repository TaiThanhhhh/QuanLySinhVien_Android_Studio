package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.databinding.ItemStudentSelectableGridBinding;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StudentSelectionGridAdapter extends RecyclerView.Adapter<StudentSelectionGridAdapter.ViewHolder> {

    private List<User> students;
    private final Set<User> selectedStudents = new HashSet<>();
    private final OnStudentSelectedListener listener;

    public interface OnStudentSelectedListener {
        void onStudentSelected(User student, boolean isSelected);
    }

    public StudentSelectionGridAdapter(List<User> students, OnStudentSelectedListener listener) {
        this.students = students;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStudentSelectableGridBinding binding = ItemStudentSelectableGridBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User student = students.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public void updateList(List<User> newList) {
        students = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemStudentSelectableGridBinding binding;

        public ViewHolder(ItemStudentSelectableGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User student) {
            binding.tvStudentName.setText(student.getName());
            binding.tvStudentId.setText(student.getMssv());

            final MaterialCardView cardView = (MaterialCardView) itemView;
            cardView.setChecked(selectedStudents.contains(student));

            itemView.setOnClickListener(v -> {
                boolean isSelected = !cardView.isChecked();
                cardView.setChecked(isSelected);
                if (isSelected) {
                    selectedStudents.add(student);
                } else {
                    selectedStudents.remove(student);
                }
                listener.onStudentSelected(student, isSelected);
            });
        }
    }
}
