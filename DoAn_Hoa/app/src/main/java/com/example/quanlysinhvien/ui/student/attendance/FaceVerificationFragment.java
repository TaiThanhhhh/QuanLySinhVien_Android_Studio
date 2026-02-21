package com.example.quanlysinhvien.ui.student.attendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.quanlysinhvien.R;
import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.Attendance;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.AttendanceRepository;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.ui.student.common.FaceUtils;
import com.example.quanlysinhvien.ui.student.common.FaceEmbedder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@ExperimentalGetImage public class FaceVerificationFragment extends Fragment {
    private static final String TAG = "FaceVerification";
    private static final double SIMILARITY_THRESHOLD = 0.8; // For landmarks
    private static final double EMBEDDING_COSINE_THRESHOLD = 0.75; // tuned for ArcFace-like models
    private static final String MODEL_ASSET = "mobile_face_embedding.tflite";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long LATE_THRESHOLD_MINUTES = 10;

    private long sessionId, startTime;
    private double latitude, longitude;

    private AttendanceRepository attendanceRepository;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private String enrolledFaceTemplate;

    private PreviewView previewView;
    private TextView tvStatus;
    private LinearLayout buttonsLayout;
    private android.widget.Button btnRetry;
    private android.widget.Button btnCancel;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    private FaceEmbedder embedder;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicInteger retryCount = new AtomicInteger(0);

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), R.string.permission_needed_camera, Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attendanceRepository = new AttendanceRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        userRepository = new UserRepository(requireContext());
        if (getArguments() != null) {
            sessionId = getArguments().getLong("session_id");
            startTime = getArguments().getLong("start_time");
            latitude = getArguments().getFloat("latitude"); // Use getFloat now
            longitude = getArguments().getFloat("longitude");
        }
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_face_verification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        previewView = view.findViewById(R.id.camera_preview);
        tvStatus = view.findViewById(R.id.tv_verification_status);
        buttonsLayout = view.findViewById(R.id.layout_buttons_verification);
        btnRetry = view.findViewById(R.id.btn_retry_verification);
        btnCancel = view.findViewById(R.id.btn_cancel_verification);

        // Wire buttons
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> {
                tvStatus.setText(R.string.opening_camera);
                buttonsLayout.setVisibility(android.view.View.GONE);
                retryVerification();
            });
        }
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelVerification());
        }

        checkEnrollmentAndStart();
    }

    private void checkEnrollmentAndStart() {
        long studentId = sessionManager.getUserId();
        User student = userRepository.getUserById(studentId);
        if (student != null && student.getFaceTemplate() != null && !student.getFaceTemplate().isEmpty()) {
            enrolledFaceTemplate = student.getFaceTemplate();
            checkCameraPermission();
        } else {
            Toast.makeText(getContext(), R.string.no_face_enrolled, Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack();
        }
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @ExperimentalGetImage
    private void startCamera() {
        // Lazy initialize embedder here to avoid loading TF runtime during fragment creation
        if (embedder == null && FaceUtils.assetExists(requireContext(), MODEL_ASSET)) {
            try {
                embedder = new FaceEmbedder(requireContext(), MODEL_ASSET, 112, 512);
            } catch (Throwable t) {
                embedder = null; // fallback to landmarks-only
            }
        }
        buttonsLayout.setVisibility(View.GONE);
        tvStatus.setText(R.string.opening_camera);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (previewView == null) {
                    Log.e(TAG, "PreviewView is null — cannot start camera");
                    requireActivity().runOnUiThread(() -> {
                        tvStatus.setText(R.string.status_camera_error);
                        buttonsLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "Preview view chưa sẵn sàng", Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                Preview preview = new Preview.Builder().build();
                
                // Select camera based on availability
                CameraSelector cameraSelector;
                boolean hasFrontCamera = false;
                try {
                    hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                } catch (Exception e) {
                    Log.e(TAG, "Error checking for front camera", e);
                }

                if (hasFrontCamera) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    boolean hasBackCamera = false;
                    try {
                        hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                    } catch (Exception e) {
                         Log.e(TAG, "Error checking for back camera", e);
                    }

                    if (hasBackCamera) {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                    } else {
                        requireActivity().runOnUiThread(() -> {
                            tvStatus.setText(R.string.status_camera_error);
                            buttonsLayout.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "Không tìm thấy camera nào trên thiết bị.", Toast.LENGTH_LONG).show();
                        });
                        return;
                    }
                }

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .build();
                FaceDetector detector = FaceDetection.getClient(options);

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (isProcessing.get() || !isAdded()) { image.close(); return; }
                    isProcessing.set(true);

                    if (image.getImage() == null) {
                        Log.w(TAG, "ImageProxy.getImage() returned null");
                        isProcessing.set(false);
                        image.close();
                        return;
                    }

                    final int rotationDegrees = image.getImageInfo().getRotationDegrees();
                    Log.d(TAG, "RotationDegrees=" + rotationDegrees);

                    @SuppressWarnings("UnsafeOptInUsageError")
                    InputImage inputImage = InputImage.fromMediaImage(image.getImage(), rotationDegrees);

                    detector.process(inputImage)
                            .addOnSuccessListener(faces -> {
                                if (!faces.isEmpty()) {
                                    Face face = faces.get(0);
                                    String currentLandmarksJson = FaceUtils.landmarksToJson(face.getAllLandmarks());

                                    // First try embedding-based if we have enrolled embedding
                                    try {
                                        float[] enrolledEmbedding = FaceUtils.extractEmbeddingFromTemplate(enrolledFaceTemplate);
                                        if (enrolledEmbedding != null && embedder != null && embedder.isReady()) {
                                            Bitmap previewBitmap = previewView.getBitmap();
                                            if (previewBitmap != null) {
                                                android.graphics.Rect bbox = face.getBoundingBox();
                                                Bitmap faceCrop = FaceUtils.alignCrop(previewBitmap, faces.get(0).getAllLandmarks(), bbox, true, 25);
                                                if (faceCrop != null) {
                                                    float[] probeEmb = embedder.embed(faceCrop);
                                                    double cosine = FaceEmbedder.cosineSimilarity(enrolledEmbedding, probeEmb);
                                                    Log.d(TAG, "Embedding cosine similarity=" + cosine);
                                                    if (cosine >= EMBEDDING_COSINE_THRESHOLD) {
                                                        if (cameraProvider != null) cameraProvider.unbindAll();
                                                        recordAttendanceAsSuccess();
                                                    } else {
                                                        // fallback to landmarks-based
                                                        String currentFaceTemplate = currentLandmarksJson;
                                                        if (currentFaceTemplate != null && compareFaces(enrolledFaceTemplate, currentFaceTemplate)) {
                                                            if (cameraProvider != null) cameraProvider.unbindAll();
                                                            recordAttendanceAsSuccess();
                                                        } else {
                                                            handleVerificationFailure();
                                                        }
                                                    }
                                                } else {
                                                    handleVerificationFailure();
                                                }
                                            } else {
                                                handleVerificationFailure();
                                            }
                                        } else {
                                            // No enrolled embedding, fallback to landmarks compare
                                            String currentFaceTemplate = currentLandmarksJson;
                                            if (currentFaceTemplate != null && compareFaces(enrolledFaceTemplate, currentFaceTemplate)) {
                                                if (cameraProvider != null) cameraProvider.unbindAll();
                                                recordAttendanceAsSuccess();
                                            } else {
                                                handleVerificationFailure();
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error comparing faces/embeddings", e);
                                        handleVerificationFailure();
                                    }

                                } else {
                                    handleVerificationFailure();
                                }
                                isProcessing.set(false);
                                image.close();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Face detection failed", e);
                                handleVerificationFailure();
                                isProcessing.set(false);
                                image.close();
                            });
                });
                
                final CameraSelector finalSelector = cameraSelector;
                // Wait for the view to be properly laid out before binding
                previewView.post(() -> {
                    preview.setSurfaceProvider(previewView.getSurfaceProvider());
                    try {
                        cameraProvider.unbindAll();
                        cameraProvider.bindToLifecycle(getViewLifecycleOwner(), finalSelector, preview, imageAnalysis);
                        requireActivity().runOnUiThread(() -> tvStatus.setText(R.string.status_finding_face));
                        Log.i(TAG, "Camera bound successfully with selector=" + finalSelector);
                    } catch (Exception be) {
                        Log.e(TAG, "Camera bind failed for selected camera", be);
                        requireActivity().runOnUiThread(() -> {
                            tvStatus.setText(R.string.status_camera_error);
                            buttonsLayout.setVisibility(android.view.View.VISIBLE);
                            Toast.makeText(getContext(), "Không thể mở camera: " + be.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
                requireActivity().runOnUiThread(() -> {
                    tvStatus.setText(R.string.status_camera_error);
                    buttonsLayout.setVisibility(android.view.View.VISIBLE);
                    Toast.makeText(getContext(), "Lỗi khi khởi tạo camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void handleVerificationFailure() {
        if (retryCount.incrementAndGet() >= MAX_RETRY_ATTEMPTS) {
            if (cameraProvider != null) cameraProvider.unbindAll();
            requireActivity().runOnUiThread(() -> {
                tvStatus.setText(R.string.status_verification_failed);
                buttonsLayout.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(), "Không thể nhận dạng khuôn mặt. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void retryVerification() {
        retryCount.set(0);
        startCamera();
    }

    private void cancelVerification() {
        Toast.makeText(getContext(), R.string.attendance_canceled, Toast.LENGTH_SHORT).show();
        NavHostFragment.findNavController(this).popBackStack(R.id.nav_student_home, false);
    }

    private boolean compareFaces(String enrolledTemplate, String currentTemplate) throws Exception {
        List<Pair<Integer, PointF>> enrolledLandmarks = FaceUtils.jsonToTypedLandmarks(enrolledTemplate);
        List<Pair<Integer, PointF>> currentLandmarks = FaceUtils.jsonToTypedLandmarks(currentTemplate);

        if (enrolledLandmarks.size() < 5 || currentLandmarks.size() < 5) {
            Log.d(TAG, "Not enough landmarks: enrolled=" + enrolledLandmarks.size() + ", current=" + currentLandmarks.size());
            return false;
        }

        // Map type -> point for stable matching
        Map<Integer, PointF> enrolledMap = new HashMap<>();
        for (Pair<Integer, PointF> p : enrolledLandmarks) enrolledMap.put(p.first, p.second);
        Map<Integer, PointF> currentMap = new HashMap<>();
        for (Pair<Integer, PointF> p : currentLandmarks) currentMap.put(p.first, p.second);

        List<PointF> matchedEnrolled = new ArrayList<>();
        List<PointF> matchedCurrent = new ArrayList<>();
        for (Integer type : enrolledMap.keySet()) {
            if (currentMap.containsKey(type)) {
                matchedEnrolled.add(enrolledMap.get(type));
                matchedCurrent.add(currentMap.get(type));
            }
        }

        if (matchedEnrolled.size() < 5) {
            Log.d(TAG, "Not enough matched landmark types: " + matchedEnrolled.size());
            return false;
        }

        // Convert to double arrays for numeric ops
        int n = matchedEnrolled.size();
        double[][] A = new double[n][2];
        double[][] B = new double[n][2];
        for (int i = 0; i < n; i++) {
            A[i][0] = matchedEnrolled.get(i).x;
            A[i][1] = matchedEnrolled.get(i).y;
            B[i][0] = matchedCurrent.get(i).x;
            B[i][1] = matchedCurrent.get(i).y;
        }

        // Compute similarity transform (scale, rotation, translation) from B -> A using Procrustes (Umeyama)
        // Step 1: centroids
        double[] centroidA = {0.0, 0.0}, centroidB = {0.0, 0.0};
        for (int i = 0; i < n; i++) {
            centroidA[0] += A[i][0]; centroidA[1] += A[i][1];
            centroidB[0] += B[i][0]; centroidB[1] += B[i][1];
        }
        centroidA[0] /= n; centroidA[1] /= n;
        centroidB[0] /= n; centroidB[1] /= n;

        // center the points
        double[][] AA = new double[n][2];
        double[][] BB = new double[n][2];
        for (int i = 0; i < n; i++) {
            AA[i][0] = A[i][0] - centroidA[0]; AA[i][1] = A[i][1] - centroidA[1];
            BB[i][0] = B[i][0] - centroidB[0]; BB[i][1] = B[i][1] - centroidB[1];
        }

        // compute variance of BB for scale
        double varB = 0.0;
        for (int i = 0; i < n; i++) varB += BB[i][0]*BB[i][0] + BB[i][1]*BB[i][1];

        // compute covariance matrix H = BB^T * AA
        double[][] H = new double[2][2];
        for (int i = 0; i < n; i++) {
            H[0][0] += BB[i][0] * AA[i][0];
            H[0][1] += BB[i][0] * AA[i][1];
            H[1][0] += BB[i][1] * AA[i][0];
            H[1][1] += BB[i][1] * AA[i][1];
        }

        // SVD of 2x2 H: compute rotation using analytic SVD
        double det = H[0][0]*H[1][1] - H[0][1]*H[1][0];
        double trace = H[0][0] + H[1][1];
        // compute rotation angle via atan2
        double angle = Math.atan2(H[0][1] - H[1][0], H[0][0] + H[1][1]);
        double cos = Math.cos(angle/1.0);
        double sin = Math.sin(angle/1.0);

        // compute scale = trace(H^T R) / varB approx = trace / varB
        double scale = (trace) / (varB == 0 ? 1 : varB);

        // apply transform to BB and compute RMS error to AA
        double sumSq = 0.0;
        for (int i = 0; i < n; i++) {
            double x = BB[i][0], y = BB[i][1];
            double xr = scale * (cos * x - sin * y);
            double yr = scale * (sin * x + cos * y);
            double dx = AA[i][0] - xr;
            double dy = AA[i][1] - yr;
            sumSq += dx*dx + dy*dy;
        }
        double rmse = Math.sqrt(sumSq / n);

        // Normalize by average face size (distance between eyes if present or bbox size approximated)
        double norm = 0.0;
        // try use eye distance if available
        PointF leftEyeEn = null, rightEyeEn = null;
        for (Pair<Integer, PointF> p : enrolledLandmarks) {
            if (p.first == com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE) leftEyeEn = p.second;
            if (p.first == com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE) rightEyeEn = p.second;
        }
        if (leftEyeEn != null && rightEyeEn != null) {
            norm = Math.hypot(leftEyeEn.x - rightEyeEn.x, leftEyeEn.y - rightEyeEn.y);
        } else {
            // fallback to average distance to centroid
            double avgDist = 0.0;
            for (PointF p : matchedEnrolled) avgDist += Math.hypot(p.x - (float)centroidA[0], p.y - (float)centroidA[1]);
            norm = avgDist / n;
        }
        if (norm == 0) norm = 1.0;
        double normalizedRmse = rmse / norm;

        Log.d(TAG, "Procrustes RMSE=" + rmse + " normalized=" + normalizedRmse + " matchedCount=" + n);

        // Tuned threshold: matches if normalized RMSE < 0.12
        return normalizedRmse < 0.12;
    }

    private void recordAttendanceAsSuccess() {
        if (!isAdded()) return;

        long currentTime = System.currentTimeMillis();
        long diffMinutes = (currentTime - startTime) / (60 * 1000);

        Attendance attendance = new Attendance();
        attendance.setSessionId(sessionId);
        attendance.setStudentId(sessionManager.getUserId());
        attendance.setTimestamp(currentTime);
        attendance.setLatitude(latitude);
        attendance.setLongitude(longitude);
        String status = diffMinutes <= LATE_THRESHOLD_MINUTES ? "ON_TIME" : "LATE";
        attendance.setStatus(status);

        long result = attendanceRepository.recordAttendance(attendance);

        requireActivity().runOnUiThread(() -> {
            if(result != -1) {
                Toast.makeText(getContext(), getString(R.string.attendance_success, status), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), R.string.attendance_failed, Toast.LENGTH_LONG).show();
            }
            NavHostFragment.findNavController(this).popBackStack(R.id.nav_student_home, false);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if(cameraProvider != null) cameraProvider.unbindAll();
    }
}
