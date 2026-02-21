package com.example.quanlysinhvien.ui.admin.classmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.User;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder> {
    private List<User> studentList;

    public StudentAdapter(List<User> studentList) {
        this.studentList = studentList;
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_in_class, parent, false);
        return new StudentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder holder, int position) {
        User student = studentList.get(position);
        holder.bind(student);
    }

    @Override
    public int getItemCount() {
        return studentList.size();
    }

    public void updateData(List<User> newStudentList) {
        this.studentList = newStudentList;
        notifyDataSetChanged();
    }

    class StudentViewHolder extends RecyclerView.ViewHolder {
        private final TextView studentName, studentId;

        StudentViewHolder(View itemView) {
            super(itemView);
            studentName = itemView.findViewById(R.id.tv_student_name);
            studentId = itemView.findViewById(R.id.tv_student_id);
        }

        void bind(User student) {
            studentName.setText(student.getName());
            studentId.setText(student.getMssv());
        }
    }
}
