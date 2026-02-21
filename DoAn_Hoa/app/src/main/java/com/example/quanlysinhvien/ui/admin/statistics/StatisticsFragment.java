package com.example.quanlysinhvien.ui.admin.statistics;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.data.model.AttendanceSession;
import com.example.quanlysinhvien.data.model.AttendanceStatus;
import com.example.quanlysinhvien.data.model.ClassModel;
import com.example.quanlysinhvien.data.model.StatusCount;
import com.example.quanlysinhvien.data.model.TimestampCount;
import com.example.quanlysinhvien.data.repo.AttendanceRepository;
import com.example.quanlysinhvien.data.repo.AttendanceSessionRepository;
import com.example.quanlysinhvien.data.repo.ClassRepository;
import com.example.quanlysinhvien.data.repo.EnrollmentRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StatisticsFragment extends Fragment {
    private static final String TAG = "StatisticsFragment";

    private PieChart pieChart;
    private LineChart lineChart;
    private AutoCompleteTextView classSpinner;
    private AttendanceRepository attendanceRepository;
    private AttendanceSessionRepository sessionRepository;
    private ClassRepository classRepository;
    private EnrollmentRepository enrollmentRepository;
    private ClassModel selectedClass;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    exportPdf();
                } else {
                    Toast.makeText(getContext(), "Cần cấp quyền ghi file để xuất báo cáo", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attendanceRepository = new AttendanceRepository(requireContext());
        sessionRepository = new AttendanceSessionRepository(requireContext());
        classRepository = new ClassRepository(requireContext());
        enrollmentRepository = new EnrollmentRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pieChart = view.findViewById(R.id.pie_chart_attendance);
        lineChart = view.findViewById(R.id.line_chart_timeline);
        classSpinner = view.findViewById(R.id.actv_class_filter);
        Button btnExport = view.findViewById(R.id.btn_export_pdf);
        btnExport.setOnClickListener(v -> checkPermissionAndExport());

        setupCharts();
        setupClassSpinner();
    }

    private void setupClassSpinner() {
        List<ClassModel> classList = classRepository.listClasses(null);
        if (classList == null || classList.isEmpty()) {
            Toast.makeText(getContext(), "Chưa có lớp học nào", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<ClassModel> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, classList);
        classSpinner.setAdapter(adapter);

        classSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedClass = (ClassModel) parent.getItemAtPosition(position);
            loadChartData(selectedClass.getId());
        });

        if (!classList.isEmpty()) {
            selectedClass = classList.get(0);
            classSpinner.setText(selectedClass.getTitle(), false);
            loadChartData(selectedClass.getId());
        }
    }

    private void setupCharts() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.BLACK);

        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getAxisRight().setEnabled(false);
    }

    private void loadChartData(long classId) {
        AttendanceSession latestSession = sessionRepository.getLatestSessionForClass(classId);
        if (latestSession != null) {
            loadPieChartData(latestSession.getId(), classId);
            loadLineChartData(latestSession.getId());
        } else {
            pieChart.clear();
            lineChart.clear();
            Toast.makeText(getContext(), "Lớp học chưa có dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPieChartData(long sessionId, long classId) {
        List<StatusCount> statusCounts = attendanceRepository.getAttendanceStatusCounts(sessionId);
        int totalStudents = enrollmentRepository.getStudentsInClass(classId).size();
        int presentStudents = 0;

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (StatusCount sc : statusCounts) {
            String statusLabel = sc.getStatus().equals("ON_TIME") ? "Đúng giờ" : "Đi muộn";
            entries.add(new PieEntry(sc.getCount(), statusLabel));
            presentStudents += sc.getCount();
        }

        int absentCount = totalStudents - presentStudents;
        if (absentCount > 0) {
            entries.add(new PieEntry(absentCount, "Vắng"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate(); // refresh
    }

    private void loadLineChartData(long sessionId) {
        List<TimestampCount> timelineData = attendanceRepository.getAttendanceTimeline(sessionId);
        if (timelineData.isEmpty()) {
            lineChart.clear();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        long firstTimestamp = timelineData.get(0).getTimestamp();
        int cumulativeCount = 0;

        for (TimestampCount tc : timelineData) {
            cumulativeCount += tc.getCount();
            long timeDiffMinutes = TimeUnit.MILLISECONDS.toMinutes(tc.getTimestamp() - firstTimestamp);
            entries.add(new Entry(timeDiffMinutes, cumulativeCount));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Số sinh viên điểm danh");
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4.5f);
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // refresh
    }

    private void checkPermissionAndExport() {
        if (selectedClass == null) {
            Toast.makeText(getContext(), "Vui lòng chọn một lớp học", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            exportPdf();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void exportPdf() {
        AttendanceSession latestSession = sessionRepository.getLatestSessionForClass(selectedClass.getId());
        if (latestSession == null) {
            Toast.makeText(getContext(), "Lớp học này chưa có dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
            return;
        }

        List<AttendanceStatus> attendanceList = attendanceRepository.getAttendanceStatusForSession(latestSession.getId(), selectedClass.getId());

        String fileName = "BaoCaoDiemDanh_" + selectedClass.getTitle().replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";

        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                fos = requireContext().getContentResolver().openOutputStream(uri);
            } else {
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString();
                fos = new FileOutputStream(filePath + "/" + fileName);
            }

            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();

            document.add(new Paragraph("BÁO CÁO ĐIỂM DANH"));
            document.add(new Paragraph("Lớp: " + selectedClass.getTitle()));
            document.add(new Paragraph("Buổi học: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(latestSession.getStartTime()))));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.addCell("STT");
            table.addCell("MSSV");
            table.addCell("Họ và Tên");
            table.addCell("Trạng thái");

            int index = 1;
            for (AttendanceStatus item : attendanceList) {
                table.addCell(String.valueOf(index++));
                table.addCell(item.getStudent().getMssv());
                table.addCell(item.getStudent().getName());
                table.addCell(item.getStatus());
            }
            document.add(table);

            document.close();
            fos.close();
            Toast.makeText(getContext(), "Đã xuất file PDF thành công vào thư mục Downloads", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error exporting PDF", e);
            Toast.makeText(getContext(), "Lỗi khi xuất file PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
