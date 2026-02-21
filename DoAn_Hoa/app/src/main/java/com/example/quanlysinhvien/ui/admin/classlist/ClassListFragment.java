package com.example.quanlysinhvien.ui.admin.classlist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.ClassModel;
import com.example.quanlysinhvien.data.repo.ClassRepository;
import com.example.quanlysinhvien.databinding.FragmentClassListBinding;
import com.example.quanlysinhvien.databinding.ItemClassBinding;
import com.example.quanlysinhvien.ui.base.ConfirmationDialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClassListFragment extends Fragment {

    private ClassRepository classRepository;
    private ClassAdapter adapter;
    private FragmentClassListBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        classRepository = new ClassRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentClassListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.rvClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassAdapter(new ArrayList<>());
        binding.rvClasses.setAdapter(adapter);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadClasses(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.fabAddClass.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_list_to_create);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Editable searchText = binding.etSearch.getText();
        loadClasses(searchText != null ? searchText.toString() : "");
    }

    private void loadClasses(String filter) {
        List<ClassModel> classList = classRepository.listClasses(filter);
        adapter.updateData(classList);
    }

    private void showDeleteConfirmationDialog(ClassModel classModel) {
        ConfirmationDialogFragment dialog = ConfirmationDialogFragment.newInstance(
                "Xác nhận xóa",
                "Bạn có chắc chắn muốn xóa lớp học '" + classModel.getTitle() + "' không?",
                R.drawable.ic_baseline_delete_24,
                "Xóa",
                "Hủy"
        );

        dialog.setOnResultListener(confirmed -> {
            if (confirmed) {
                classRepository.deleteClass(classModel.getId());
                Editable searchText = binding.etSearch.getText();
                loadClasses(searchText != null ? searchText.toString() : "");
                Toast.makeText(getContext(), "Xóa lớp học thành công", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show(getParentFragmentManager(), "DeleteClassConfirmation");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {
        private List<ClassModel> items;

        ClassAdapter(List<ClassModel> items) {
            this.items = items;
        }

        public void updateData(List<ClassModel> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemClassBinding itemBinding = ItemClassBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ClassViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
            holder.bind(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ClassViewHolder extends RecyclerView.ViewHolder {
            private final ItemClassBinding itemBinding;

            ClassViewHolder(ItemClassBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;

                itemView.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        long classId = items.get(position).getId();
                        Bundle bundle = new Bundle();
                        bundle.putLong("class_id", classId);
                        NavHostFragment.findNavController(ClassListFragment.this).navigate(R.id.action_list_to_detail, bundle);
                    }
                });

                itemBinding.btnEditClass.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        long classId = items.get(position).getId();
                        Bundle bundle = new Bundle();
                        bundle.putLong("class_id", classId);
                        NavHostFragment.findNavController(ClassListFragment.this).navigate(R.id.action_list_to_create, bundle);
                    }
                });

                itemBinding.btnDeleteClass.setOnClickListener(v -> {
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        showDeleteConfirmationDialog(items.get(position));
                    }
                });
            }

            void bind(ClassModel classModel) {
                Context context = itemView.getContext();
                itemBinding.tvClassName.setText(classModel.getTitle());
                itemBinding.tvClassSubject.setText(classModel.getSubject());
                itemBinding.tvLecturerName.setText(classModel.getTeacher());
                itemBinding.tvSemester.setText(classModel.getSemester());
                itemBinding.tvRoom.setText(classModel.getRoom());

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                String startDate = (classModel.getStartDate() != null && classModel.getStartDate() > 0) ? sdf.format(new Date(classModel.getStartDate())) : "N/A";
                String endDate = (classModel.getEndDate() != null && classModel.getEndDate() > 0) ? sdf.format(new Date(classModel.getEndDate())) : "N/A";
                itemBinding.tvStartEndDate.setText(String.format("%s - %s", startDate, endDate));

                ClassModel.Status status = classModel.getCalculatedStatus();
                int statusColorRes;
                int statusTextRes;

                switch (status) {
                    case ONGOING:
                        statusTextRes = R.string.class_status_ongoing;
                        statusColorRes = R.color.status_ongoing;
                        break;
                    case UPCOMING:
                        statusTextRes = R.string.class_status_upcoming;
                        statusColorRes = R.color.status_upcoming;
                        break;
                    case FINISHED:
                        statusTextRes = R.string.class_status_finished;
                        statusColorRes = R.color.status_finished;
                        break;
                    default: // LOCKED
                        statusTextRes = R.string.class_status_locked;
                        statusColorRes = R.color.status_locked;
                        break;
                }
                itemBinding.chipClassStatus.setText(context.getString(statusTextRes));
                itemBinding.chipClassStatus.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, statusColorRes)));
            }
        }
    }
}
