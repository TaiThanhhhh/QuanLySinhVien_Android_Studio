package com.example.quanlysinhvien.ui.student.enrollment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.ui.student.common.FaceUtils;
import com.example.quanlysinhvien.ui.student.common.FaceEmbedder;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import com.example.quanlysinhvien.databinding.FragmentFaceEnrollmentBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Handler;
import android.os.Looper;

public class FaceEnrollmentFragment extends Fragment {
    private static final String TAG = "FaceEnrollment";
    private static final String MODEL_ASSET = "mobile_face_embedding.tflite";
    private static final int ENROLL_SAMPLES = 5;
    private static final long SAMPLE_INTERVAL_MS = 700; // time between captures
    private FragmentFaceEnrollmentBinding binding;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    private ExecutorService cameraExecutor;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private FaceEmbedder embedder;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Cần cấp quyền camera để đăng ký khuôn mặt", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userRepository = new UserRepository(requireContext());
        sessionManager = new SessionManager(requireContext());
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentFaceEnrollmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkCameraPermission();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        // Lazy initialize embedder here to avoid loading TF runtime during fragment
        // creation
        if (embedder == null && FaceUtils.assetExists(requireContext(), MODEL_ASSET)) {
            try {
                embedder = new FaceEmbedder(requireContext(), MODEL_ASSET, 112, 512);
            } catch (Throwable t) {
                // If interpreter not available or init fails, keep embedder null and fallback
                // to landmarks-only
                embedder = null;
            }
        }
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                .getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreviewEnrollment.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
                FaceDetector detector = FaceDetection.getClient(options);

                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (isProcessing.get() || !isAdded()) {
                        image.close();
                        return;
                    }
                    isProcessing.set(true);

                    if (image.getImage() == null) {
                        Log.w(TAG, "ImageProxy.getImage() returned null");
                        isProcessing.set(false);
                        image.close();
                        return;
                    }

                    @SuppressWarnings("UnsafeOptInUsageError")
                    InputImage inputImage = InputImage.fromMediaImage(image.getImage(),
                            image.getImageInfo().getRotationDegrees());

                    detector.process(inputImage)
                            .addOnSuccessListener(faces -> {
                                if (!faces.isEmpty()) {
                                    Face face = faces.get(0);
                                    // Simple check for a decent picture for enrollment
                                    if (isGoodFaceForEnrollment(face)) {
                                        String landmarksJson = FaceUtils.landmarksToJson(face.getAllLandmarks());
                                        final String[] faceTemplateRef = new String[1];
                                        faceTemplateRef[0] = null;

                                        // If embedder available, perform multi-sample enrollment
                                        if (embedder != null && embedder.isReady()) {
                                            // collect ENROLL_SAMPLES embeddings
                                            List<float[]> collected = new java.util.ArrayList<>();
                                            int[] captureCount = { 0 };
                                            binding.tvEnrollmentStatus
                                                    .setText(getString(R.string.enrollment_collecting, captureCount[0],
                                                            ENROLL_SAMPLES));

                                            Runnable captureTask = new Runnable() {
                                                @Override
                                                public void run() {
                                                    Bitmap previewBitmap = binding.cameraPreviewEnrollment.getBitmap();
                                                    if (previewBitmap != null) {
                                                        android.graphics.Rect bbox = face.getBoundingBox();
                                                        Bitmap faceCrop = FaceUtils.alignCrop(previewBitmap,
                                                                face.getAllLandmarks(), bbox, true, 25);
                                                        if (faceCrop != null) {
                                                            float[] emb = embedder.embed(faceCrop);
                                                            if (emb != null)
                                                                collected.add(emb);
                                                        }
                                                    }
                                                    captureCount[0]++;
                                                    mainHandler.post(() -> binding.tvEnrollmentStatus
                                                            .setText(getString(R.string.enrollment_collecting,
                                                                    captureCount[0], ENROLL_SAMPLES)));
                                                    if (captureCount[0] < ENROLL_SAMPLES) {
                                                        mainHandler.postDelayed(this, SAMPLE_INTERVAL_MS);
                                                    } else {
                                                        // average embeddings
                                                        if (!collected.isEmpty()) {
                                                            int dim = collected.get(0).length;
                                                            float[] avg = new float[dim];
                                                            for (float[] e : collected) {
                                                                for (int i = 0; i < dim; i++)
                                                                    avg[i] += e[i];
                                                            }
                                                            for (int i = 0; i < dim; i++)
                                                                avg[i] /= collected.size();
                                                            // normalize
                                                            double sum = 0;
                                                            for (float v : avg)
                                                                sum += v * v;
                                                            double norm = Math.sqrt(sum);
                                                            if (norm > 0)
                                                                for (int i = 0; i < dim; i++)
                                                                    avg[i] /= norm;
                                                            faceTemplateRef[0] = FaceUtils
                                                                    .packTemplateWithEmbedding(landmarksJson, avg);
                                                        }

                                                        if (faceTemplateRef[0] == null)
                                                            faceTemplateRef[0] = landmarksJson;

                                                        if (faceTemplateRef[0] != null) {
                                                            cameraProvider.unbindAll();
                                                            userRepository.updateFaceTemplate(
                                                                    sessionManager.getUserId(), faceTemplateRef[0]);
                                                            requireActivity().runOnUiThread(() -> {
                                                                Toast.makeText(getContext(),
                                                                        "Đăng ký khuôn mặt thành công!",
                                                                        Toast.LENGTH_LONG).show();
                                                                NavHostFragment
                                                                        .findNavController(FaceEnrollmentFragment.this)
                                                                        .popBackStack();
                                                            });
                                                        }

                                                        isProcessing.set(false);
                                                        image.close();
                                                    }
                                                }
                                            };

                                            // start first capture immediately
                                            mainHandler.post(captureTask);
                                            return; // we will finish after captures
                                        }

                                        // Fallback: store landmarks-only
                                        if (faceTemplateRef[0] == null)
                                            faceTemplateRef[0] = landmarksJson;

                                        if (faceTemplateRef[0] != null) {
                                            cameraProvider.unbindAll();
                                            userRepository.updateFaceTemplate(sessionManager.getUserId(),
                                                    faceTemplateRef[0]);
                                            requireActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "Đăng ký khuôn mặt thành công!",
                                                        Toast.LENGTH_LONG).show();
                                                NavHostFragment.findNavController(FaceEnrollmentFragment.this)
                                                        .popBackStack();
                                            });
                                        }
                                    } else {
                                        requireActivity()
                                                .runOnUiThread(() -> binding.tvEnrollmentStatus
                                                        .setText(R.string.enrollment_hold_still));
                                    }
                                }
                                isProcessing.set(false);
                                image.close();
                            })
                            .addOnFailureListener(e -> {
                                isProcessing.set(false);
                                image.close();
                            });
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private boolean isGoodFaceForEnrollment(Face face) {
        // Check if all necessary landmarks are present
        if (face.getLandmark(FaceLandmark.LEFT_EYE) == null || face.getLandmark(FaceLandmark.RIGHT_EYE) == null
                || face.getLandmark(FaceLandmark.NOSE_BASE) == null) {
            return false;
        }
        // Check head rotation
        return face.getHeadEulerAngleY() < 10 && face.getHeadEulerAngleY() > -10 && face.getHeadEulerAngleZ() < 10
                && face.getHeadEulerAngleZ() > -10;
    }

    private String landmarksToJson(List<FaceLandmark> landmarks) {
        try {
            // Save landmarks with their type to ensure a stable order when comparing later
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (FaceLandmark landmark : landmarks) {
                if (landmark == null)
                    continue;
                JSONObject point = new JSONObject();
                point.put("type", landmark.getLandmarkType());
                point.put("x", landmark.getPosition().x);
                point.put("y", landmark.getPosition().y);
                array.put(point);
            }
            json.put("landmarks", array);
            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
