package com.example.quanlysinhvien.ui.student.attendance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.repo.AttendanceRepository;
import com.example.quanlysinhvien.data.repo.EnrollmentRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

public class ScanQrFragment extends Fragment {

    private FusedLocationProviderClient fusedLocationClient;
    private EnrollmentRepository enrollmentRepository;
    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private String qrContents;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    verifyLocationAndProceed();
                } else {
                    Toast.makeText(getContext(), "Cần cấp quyền truy cập vị trí để điểm danh", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        enrollmentRepository = new EnrollmentRepository(requireContext());
        attendanceRepository = new AttendanceRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startQrScanner();
    }

    private void startQrScanner() {
        IntentIntegrator.forSupportFragment(this).initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(getContext(), "Đã hủy quét", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).popBackStack();
            } else {
                this.qrContents = result.getContents();
                checkPermissionAndVerifyLocation();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void checkPermissionAndVerifyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            verifyLocationAndProceed();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void verifyLocationAndProceed() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                try {
                    JSONObject json = new JSONObject(qrContents);
                    long sessionId = json.getLong("sessionId");
                    long classId = json.getLong("classId");
                    long startTime = json.getLong("startTime");
                    double classLat = json.getDouble("latitude");
                    double classLon = json.getDouble("longitude");
                    long userId = sessionManager.getUserId();

                    if (attendanceRepository.hasStudentAttended(sessionId, userId)) {
                        Toast.makeText(getContext(), "Bạn đã điểm danh cho buổi học này rồi", Toast.LENGTH_LONG).show();
                        NavHostFragment.findNavController(this).popBackStack();
                        return;
                    }

                    if (!enrollmentRepository.isStudentEnrolled(classId, userId)) {
                        Toast.makeText(getContext(), "Bạn không có trong danh sách lớp học này", Toast.LENGTH_LONG).show();
                        NavHostFragment.findNavController(this).popBackStack();
                        return;
                    }

                    float[] results = new float[1];
                    Location.distanceBetween(classLat, classLon, location.getLatitude(), location.getLongitude(), results);
                    float distanceInMeters = results[0];

                    if (distanceInMeters <= 50) {
                        showConfirmationDialog(sessionId, startTime, location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(getContext(), "Bạn đang ở ngoài phạm vi điểm danh. Vui lòng tiến lại gần hơn.", Toast.LENGTH_LONG).show();
                        NavHostFragment.findNavController(this).popBackStack();
                    }

                } catch (Exception e) {
                    Toast.makeText(getContext(), "Mã QR không hợp lệ", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                }
            } else {
                Toast.makeText(getContext(), "Không thể lấy được vị trí. Vui lòng bật GPS.", Toast.LENGTH_LONG).show();
                NavHostFragment.findNavController(this).popBackStack();
            }
        });
    }

    private void showConfirmationDialog(long sessionId, long startTime, double lat, double lon) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận Điểm danh")
                .setMessage("Bạn có chắc chắn muốn điểm danh cho buổi học này không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    Bundle bundle = new Bundle();
                    bundle.putLong("session_id", sessionId);
                    bundle.putLong("start_time", startTime);
                    bundle.putFloat("latitude", (float) lat);
                    bundle.putFloat("longitude", (float) lon);
                    NavHostFragment.findNavController(this).navigate(R.id.action_scan_to_face, bundle);
                })
                .setNegativeButton("Không", (dialog, which) -> {
                    Toast.makeText(getContext(), "Đã hủy điểm danh", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .show();
    }
}
