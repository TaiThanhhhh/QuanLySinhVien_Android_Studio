package com.example.quanlysinhvien.ui.admin.attendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.repo.AttendanceSessionRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import java.util.Locale;

public class GenerateQrFragment extends Fragment {

    private long classId;
    private AttendanceSessionRepository attendanceSessionRepository;
    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView ivQrCode;
    private TextView tvCountdown;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    generateAndShowQrCode();
                } else {
                    Toast.makeText(getContext(), "Cần cấp quyền truy cập vị trí để tạo mã QR", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attendanceSessionRepository = new AttendanceSessionRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        if (getArguments() != null) {
            classId = getArguments().getLong("class_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generate_qr, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivQrCode = view.findViewById(R.id.iv_qr_code);
        tvCountdown = view.findViewById(R.id.tv_countdown);

        checkPermissionAndGenerateQr();
    }

    private void checkPermissionAndGenerateQr() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            generateAndShowQrCode();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void generateAndShowQrCode() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                long userId = sessionManager.getUserId();
                long sessionId = attendanceSessionRepository.createSession(classId, userId);

                if (sessionId == -1) {
                    Toast.makeText(getContext(), "Không thể tạo buổi điểm danh", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject qrContent = new JSONObject();
                    qrContent.put("sessionId", sessionId);
                    qrContent.put("classId", classId);
                    qrContent.put("startTime", System.currentTimeMillis());
                    qrContent.put("latitude", location.getLatitude());
                    qrContent.put("longitude", location.getLongitude());

                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                    Bitmap bitmap = barcodeEncoder.encodeBitmap(qrContent.toString(), BarcodeFormat.QR_CODE, 400, 400);
                    ivQrCode.setImageBitmap(bitmap);

                    startCountdown();
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Không thể lấy được vị trí. Vui lòng bật GPS.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startCountdown() {
        new CountDownTimer(45 * 60 * 1000, 1000) {
            public void onTick(long millisUntilFinished) {
                long minutes = (millisUntilFinished / 1000) / 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvCountdown.setText(String.format(Locale.getDefault(), "Mã QR sẽ hết hạn trong: %02d:%02d", minutes, seconds));
            }
            public void onFinish() {
                tvCountdown.setText("Mã QR đã hết hạn");
                if(isAdded()) ivQrCode.setImageAlpha(100);
            }
        }.start();
    }
}
