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
import com.example.quanlysinhvien.databinding.GridItemStudentSelectionBinding;

import java.util.ArrayList;
import java.util.List;

public class StudentSelectionAdapter extends ListAdapter<User, StudentSelectionAdapter.ViewHolder> {

    // Single source of truth: which students are selected (by ID)
    private final List<Long> selectedIds;
    // Full unfiltered list to resolve User objects from IDs even when items are
    // filtered out
    private List<User> fullStudentList = new ArrayList<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public StudentSelectionAdapter(List<User> initiallySelected) {
        super(new DiffUtil.ItemCallback<User>() {
            @Override
            public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
                return oldItem.getId() == newItem.getId() &&
                        oldItem.getName().equals(newItem.getName());
            }
        });
        this.selectedIds = new ArrayList<>();
        if (initiallySelected != null) {
            for (User u : initiallySelected)
                selectedIds.add(u.getId());
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GridItemStudentSelectionBinding binding = GridItemStudentSelectionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /** Call this after submitList() with the FULL (unfiltered) student list */
    public void setFullList(List<User> fullList) {
        this.fullStudentList = new ArrayList<>(fullList);
    }

    /** Returns a new list of all currently selected User objects */
    public List<User> getSelectedStudents() {
        List<User> result = new ArrayList<>();
        for (User u : fullStudentList) {
            if (selectedIds.contains(u.getId())) {
                result.add(u);
            }
        }
        return result;
    }

    public void selectAll() {
        selectedIds.clear();
        for (User u : fullStudentList) {
            selectedIds.add(u.getId());
        }
        notifyDataSetChanged();
        if (listener != null)
            listener.onSelectionChanged();
    }

    public void deselectAll() {
        selectedIds.clear();
        notifyDataSetChanged();
        if (listener != null)
            listener.onSelectionChanged();
    }

    public boolean isAllSelected() {
        return !fullStudentList.isEmpty() && selectedIds.size() == fullStudentList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final GridItemStudentSelectionBinding binding;

        ViewHolder(GridItemStudentSelectionBinding b) {
            super(b.getRoot());
            this.binding = b;

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION)
                    return;
                User student = getItem(pos);
                long id = student.getId();
                if (selectedIds.contains(id)) {
                    selectedIds.remove((Long) id); // Fix: remove by Object, not by index
                } else {
                    selectedIds.add(id);
                }
                notifyItemChanged(pos);
                if (listener != null)
                    listener.onSelectionChanged();
            });
        }

        void bind(User student) {
            boolean isSelected = selectedIds.contains(student.getId());

            binding.tvStudentNameSelection.setText(student.getName());
            binding.tvStudentIdSelection.setText(student.getMssv() != null ? student.getMssv() : "");

            // Indicator visibility
            binding.ivCheckSelection.setVisibility(isSelected ? android.view.View.VISIBLE : android.view.View.GONE);

            // Highlight card background when selected - Use primary color as requested by
            // user
            int bgColor = isSelected
                    ? ContextCompat.getColor(itemView.getContext(), R.color.primary)
                    : ContextCompat.getColor(itemView.getContext(), R.color.surface);
            binding.getRoot().setCardBackgroundColor(bgColor);

            // Text color logic for contrast when selected
            int textColor = isSelected
                    ? ContextCompat.getColor(itemView.getContext(), android.R.color.white)
                    : ContextCompat.getColor(itemView.getContext(), android.R.color.black);
            binding.tvStudentNameSelection.setTextColor(textColor);
            binding.tvStudentIdSelection.setTextColor(textColor);

            // Icon tint logic
            int iconColor = isSelected
                    ? ContextCompat.getColor(itemView.getContext(), android.R.color.white)
                    : ContextCompat.getColor(itemView.getContext(), R.color.primary);
            binding.ivStudentAvatarSelection.setImageTintList(ColorStateList.valueOf(iconColor));

            // Stroke to make it pop
            binding.getRoot().setStrokeWidth(isSelected ? 3 : 0);
            binding.getRoot().setStrokeColor(
                    isSelected ? ContextCompat.getColor(itemView.getContext(), R.color.secondary) : 0);
        }
    }
}
