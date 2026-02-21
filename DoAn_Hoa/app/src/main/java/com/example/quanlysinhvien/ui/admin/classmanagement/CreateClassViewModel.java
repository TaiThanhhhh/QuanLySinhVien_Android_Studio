package com.example.quanlysinhvien.ui.admin.classmanagement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlysinhvien.data.model.User;

import java.util.ArrayList;
import java.util.List;

public class CreateClassViewModel extends ViewModel {

    private final MutableLiveData<List<User>> _studentList = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<User>> studentList = _studentList;

    public LiveData<List<User>> getStudentList() {
        return _studentList;
    }

    public void setStudentList(List<User> students) {
        _studentList.setValue(new ArrayList<>(students));
    }

    public void addStudent(User student) {
        List<User> currentList = _studentList.getValue();
        if (currentList != null && student != null) {
            boolean exists = false;
            for (User u : currentList) {
                if (u != null && u.getId() == student.getId()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                List<User> newList = new ArrayList<>(currentList);
                newList.add(student);
                _studentList.setValue(newList);
            }
        }
    }

    public void removeStudent(User student) {
        List<User> currentList = _studentList.getValue();
        if (currentList != null && student != null) {
            List<User> newList = new ArrayList<>(currentList);
            newList.removeIf(u -> u != null && u.getId() == student.getId());
            _studentList.setValue(newList);
        }
    }

    public void clear() {
        _studentList.setValue(new ArrayList<>());
    }
}
