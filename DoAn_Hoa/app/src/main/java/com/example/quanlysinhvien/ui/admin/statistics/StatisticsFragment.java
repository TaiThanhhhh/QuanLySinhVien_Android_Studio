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
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;

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
    private android.widget.TextView tvStatSessions;
    private android.widget.TextView tvStatEnrolled;
    private android.widget.TextView tvStatRate;
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
            });

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pieChart = view.findViewById(R.id.pie_chart_attendance);
        lineChart = view.findViewById(R.id.line_chart_timeline);
        classSpinner = view.findViewById(R.id.actv_class_filter);
        tvStatSessions = view.findViewById(R.id.tv_stat_sessions);
        tvStatEnrolled = view.findViewById(R.id.tv_stat_enrolled);
        tvStatRate = view.findViewById(R.id.tv_stat_rate);
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

        ArrayAdapter<ClassModel> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line,
                classList);
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
        List<AttendanceSession> sessions = sessionRepository.getSessionsForClass(classId);
        int enrolledCount = enrollmentRepository.getStudentsInClass(classId).size();

        if (tvStatSessions != null)
            tvStatSessions.setText(String.valueOf(sessions.size()));
        if (tvStatEnrolled != null)
            tvStatEnrolled.setText(String.valueOf(enrolledCount));

        if (!sessions.isEmpty()) {
            // Aggregated Summary Data
            int totalPresentAll = 0;
            int totalLateAll = 0;
            int totalAbsentAll = 0;
            int totalExcusedAll = 0;

            ArrayList<Entry> lineEntries = new ArrayList<>();
            int sessionIndex = 0;

            // Sort sessions by time ascending for the line chart
            sessions.sort((s1, s2) -> Long.compare(s1.getStartTime(), s2.getStartTime()));

            for (AttendanceSession session : sessions) {
                List<StatusCount> counts = attendanceRepository.getAttendanceStatusCounts(session.getId());
                int sessionPresent = 0;
                int sessionLate = 0;
                int sessionExcused = 0;

                for (StatusCount sc : counts) {
                    if ("ON_TIME".equals(sc.getStatus())) {
                        sessionPresent += sc.getCount();
                    } else if ("LATE".equals(sc.getStatus())) {
                        sessionLate += sc.getCount();
                    } else if ("EXCUSED".equals(sc.getStatus())) {
                        sessionExcused += sc.getCount();
                    }
                }

                totalPresentAll += sessionPresent;
                totalLateAll += sessionLate;
                totalExcusedAll += sessionExcused;
                totalAbsentAll += Math.max(0, enrolledCount - (sessionPresent + sessionLate + sessionExcused));

                // Attendance rate for this specific session
                float sessionRate = enrolledCount > 0 ? (float) (sessionPresent + sessionLate) * 100f / enrolledCount
                        : 0f;
                lineEntries.add(new Entry(sessionIndex++, sessionRate));
            }

            // Stat Rate: Average across all sessions
            if (tvStatRate != null && enrolledCount > 0) {
                int totalPossible = sessions.size() * enrolledCount;
                int avgRate = (int) Math.round(((totalPresentAll + totalLateAll) * 100.0) / totalPossible);
                tvStatRate.setText(avgRate + "%");
            }

            // Pie Chart: Cumulative Distribution
            loadPieChartAggregated(totalPresentAll, totalLateAll, totalAbsentAll, totalExcusedAll);

            // Line Chart: Rate Trend
            loadLineChartTrend(lineEntries);

        } else {
            pieChart.clear();
            lineChart.clear();
            if (tvStatRate != null)
                tvStatRate.setText("—");
            Toast.makeText(getContext(), "Lớp học chưa có dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPieChartAggregated(int onTime, int late, int absent, int excused) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (onTime > 0)
            entries.add(new PieEntry(onTime, "Đúng giờ"));
        if (late > 0)
            entries.add(new PieEntry(late, "Đi muộn"));
        if (absent > 0)
            entries.add(new PieEntry(absent, "Vắng"));
        if (excused > 0)
            entries.add(new PieEntry(excused, "Vắng (có phép)"));

        PieDataSet dataSet = new PieDataSet(entries, "");
        List<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#4CAF50")); // Green for on time
        colors.add(Color.parseColor("#FFC107")); // Amber for late
        colors.add(Color.parseColor("#F44336")); // Red for absent
        colors.add(Color.parseColor("#2196F3")); // Blue for excused
        dataSet.setColors(colors);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);

        pieChart.setData(new PieData(dataSet));
        pieChart.invalidate();
    }

    private void loadLineChartTrend(ArrayList<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, "Tỉ lệ chuyên cần (%)");
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4.5f);
        dataSet.setDrawValues(true);
        dataSet.setColor(ColorTemplate.JOYFUL_COLORS[0]);
        dataSet.setCircleColor(ColorTemplate.JOYFUL_COLORS[0]);

        lineChart.setData(new LineData(dataSet));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getAxisLeft().setAxisMaximum(100f);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.invalidate();
    }

    private void checkPermissionAndExport() {
        if (selectedClass == null) {
            Toast.makeText(getContext(), "Vui lòng chọn một lớp học", Toast.LENGTH_SHORT).show();
            return;
        }

        // Android 10 (Q) and above uses Scoped Storage and MediaStore.Downloads
        // which does NOT require WRITE_EXTERNAL_STORAGE permission for adding new
        // files.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportPdf();
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                exportPdf();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    private void exportPdf() {
        List<AttendanceSession> sessions = sessionRepository.getSessionsForClass(selectedClass.getId());
        if (sessions == null || sessions.isEmpty()) {
            Toast.makeText(getContext(), "Lớp học này chưa có dữ liệu điểm danh", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "BaoCaoDiemDanh_" + selectedClass.getTitle().replace(" ", "_") + "_"
                + System.currentTimeMillis() + ".pdf";

        try {
            OutputStream fos;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        contentValues);
                fos = requireContext().getContentResolver().openOutputStream(uri);
            } else {
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .toString();
                fos = new FileOutputStream(filePath + "/" + fileName);
            }

            // Define Fonts - Using system font for Vietnamese support
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, BaseColor.BLACK);

            // Attempt to load system font for Unicode support (Vietnamese)
            try {
                String[] fontPaths = {
                        "/system/fonts/NotoSansVietnamese-Regular.ttf",
                        "/system/fonts/DroidSans.ttf",
                        "/system/fonts/Roboto-Regular.ttf"
                };
                BaseFont baseFont = null;
                for (String path : fontPaths) {
                    try {
                        File file = new File(path);
                        if (file.exists()) {
                            baseFont = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (baseFont != null) {
                    titleFont = new Font(baseFont, 18, Font.BOLD, BaseColor.BLACK);
                    headerFont = new Font(baseFont, 12, Font.BOLD, BaseColor.WHITE);
                    normalFont = new Font(baseFont, 11, Font.NORMAL, BaseColor.BLACK);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Document document = new Document();
            PdfWriter.getInstance(document, fos);
            document.open();

            // Report Title
            Paragraph title = new Paragraph("BÁO CÁO TỔNG HỢP ĐIỂM DANH", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Class Information
            document.add(new Paragraph("Lớp học: " + selectedClass.getTitle(), normalFont));
            document.add(new Paragraph("Môn học: " + selectedClass.getSubject(), normalFont));
            document.add(new Paragraph("Tổng số buổi: " + sessions.size(), normalFont));
            document.add(new Paragraph(
                    "Ngày xuất báo cáo: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()),
                    normalFont));
            document.add(new Paragraph(" "));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            for (AttendanceSession session : sessions) {
                Paragraph sessionHeader = new Paragraph("Buổi học: " + sdf.format(new Date(session.getStartTime())),
                        new Font(titleFont.getBaseFont(), 13, Font.BOLD, new BaseColor(0, 102, 204)));
                document.add(sessionHeader);
                document.add(new Paragraph(" "));

                List<AttendanceStatus> attendanceList = attendanceRepository
                        .getAttendanceStatusForSession(session.getId(), selectedClass.getId());

                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(10f);
                table.setWidths(new float[] { 1, 3, 5, 3 });

                // Styling headers
                PdfPCell cell;
                String[] headers = { "STT", "MSSV", "Họ và Tên", "Trạng thái" };
                for (String header : headers) {
                    cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setBackgroundColor(new BaseColor(0, 51, 102));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(8f);
                    table.addCell(cell);
                }

                int index = 1;
                for (AttendanceStatus item : attendanceList) {
                    table.addCell(createCell(String.valueOf(index++), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(item.getStudent().getMssv(), normalFont, Element.ALIGN_CENTER));
                    table.addCell(createCell(item.getStudent().getName(), normalFont, Element.ALIGN_LEFT));

                    // Localization for status
                    String status = item.getStatus();
                    String displayStatus = status;
                    if ("ON_TIME".equals(status))
                        displayStatus = "CÓ MẶT";
                    else if ("LATE".equals(status))
                        displayStatus = "ĐI MUỘN";
                    else if ("ABSENT".equals(status))
                        displayStatus = "VẮNG";
                    else if ("EXCUSED".equals(status))
                        displayStatus = "VẮNG (CÓ PHÉP)";

                    PdfPCell statusCell = createCell(displayStatus, normalFont, Element.ALIGN_CENTER);
                    if ("ABSENT".equals(status)) {
                        statusCell.setBackgroundColor(new BaseColor(255, 230, 230)); // Light red for absent
                    } else if ("EXCUSED".equals(status)) {
                        statusCell.setBackgroundColor(new BaseColor(230, 242, 255)); // Light blue for excused
                    }
                    table.addCell(statusCell);
                }
                document.add(table);
                document.add(new Paragraph(" ")); // Spacer
            }

            document.close();
            fos.close();
            Toast.makeText(getContext(), "Đã xuất báo cáo PDF thành công vào thư mục Downloads", Toast.LENGTH_LONG)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Lỗi khi xuất PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private PdfPCell createCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
