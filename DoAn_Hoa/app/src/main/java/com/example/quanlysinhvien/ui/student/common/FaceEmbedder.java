package com.example.quanlysinhvien.ui.student.common;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

public class FaceEmbedder {
    private static final String TAG = "FaceEmbedder";
    private Object interpreter; // org.tensorflow.lite.Interpreter, loaded via reflection
    private Method runMethod;
    private Method closeMethod;

    private final int inputWidth;
    private final int inputHeight;
    private int embeddingDim;

    // Default constructor: MobileFaceNet-like
    public FaceEmbedder(Context context, String modelAssetFilename) {
        this(context, modelAssetFilename, 112, 0);
    }

    // Configurable constructor: specify input size and (optional) embedding dimension
    public FaceEmbedder(Context context, String modelAssetFilename, int inputSize, int embeddingDim) {
        this.inputWidth = inputSize;
        this.inputHeight = inputSize;
        this.embeddingDim = embeddingDim; // may be 0 -> detect from model
        try {
            ByteBuffer model = loadModelFile(context, modelAssetFilename);
            // Load Interpreter class reflectively to avoid hard dependency
            try {
                Class<?> interpreterClass = Class.forName("org.tensorflow.lite.Interpreter");
                Constructor<?> ctor = interpreterClass.getConstructor(ByteBuffer.class);
                interpreter = ctor.newInstance(model);
                runMethod = interpreterClass.getMethod("run", Object.class, Object.class);
                closeMethod = interpreterClass.getMethod("close");

                // Try to detect output tensor shape and use it to set embeddingDim if not provided
                try {
                    Method getOutputTensor = interpreterClass.getMethod("getOutputTensor", int.class);
                    Object tensorObj = getOutputTensor.invoke(interpreter, 0);
                    if (tensorObj != null) {
                        Class<?> tensorClass = Class.forName("org.tensorflow.lite.Tensor");
                        Method shapeMethod = tensorClass.getMethod("shape");
                        Object shapeObj = shapeMethod.invoke(tensorObj);
                        if (shapeObj instanceof int[]) {
                            int[] shape = (int[]) shapeObj;
                            if (shape.length == 2) {
                                int detected = shape[1];
                                if (detected > 0) {
                                    this.embeddingDim = detected;
                                    Log.i(TAG, "Detected embedding dimension from model: " + detected);
                                }
                            } else if (shape.length == 1) {
                                int detected = shape[0];
                                if (detected > 0) {
                                    this.embeddingDim = detected;
                                    Log.i(TAG, "Detected 1D embedding dimension from model: " + detected);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    // ignore detection failure, keep provided embeddingDim
                    Log.w(TAG, "Could not detect model output shape reflectively", t);
                }

            } catch (Throwable t) {
                // Catch NoClassDefFoundError or other linkage errors to avoid crashing the app
                Log.e(TAG, "TensorFlow Lite interpreter not available at runtime", t);
                interpreter = null;
                runMethod = null;
                closeMethod = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model file", e);
            interpreter = null;
            runMethod = null;
            closeMethod = null;
        }

        // Ensure embeddingDim has a sensible default
        if (this.embeddingDim <= 0) {
            // some known models use 192, 128, 512; pick 192 as a safe moderate default
            this.embeddingDim = 192;
            Log.i(TAG, "Using fallback embeddingDim=" + this.embeddingDim);
        }
    }

    public boolean isReady() {
        return interpreter != null && runMethod != null;
    }

    public float[] embed(Bitmap faceBitmap) {
        if (interpreter == null || runMethod == null) return null;
        Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, inputWidth, inputHeight, true);
        ByteBuffer input = convertBitmapToFloatBuffer(resized);

        float[][] output = new float[1][embeddingDim];
        try {
            runMethod.invoke(interpreter, input, output);
            float[] emb = output[0];
            l2Normalize(emb);
            return emb;
        } catch (Throwable e) {
            Log.e(TAG, "Embedding inference failed", e);
            return null;
        }
    }

    private ByteBuffer loadModelFile(Context context, String modelAssetFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelAssetFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private ByteBuffer convertBitmapToFloatBuffer(Bitmap bitmap) {
        ByteBuffer imgData = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3);
        imgData.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputWidth * inputHeight];
        bitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        int pixel = 0;
        for (int i = 0; i < inputWidth; ++i) {
            for (int j = 0; j < inputHeight; ++j) {
                final int val = intValues[pixel++];
                // Normalize to [-1,1]
                imgData.putFloat((((val >> 16) & 0xFF) - 127.5f) / 128.0f);
                imgData.putFloat((((val >> 8) & 0xFF) - 127.5f) / 128.0f);
                imgData.putFloat(((val & 0xFF) - 127.5f) / 128.0f);
            }
        }
        imgData.rewind();
        return imgData;
    }

    private void l2Normalize(float[] v) {
        double sum = 0.0;
        for (float value : v) sum += value * value;
        double norm = Math.sqrt(sum);
        if (norm == 0) return;
        for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norm);
    }

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return -1;
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return dot; // if both L2-normalized, dot is cosine
    }

    public void close() {
        if (interpreter == null || closeMethod == null) return;
        try {
            closeMethod.invoke(interpreter);
        } catch (Throwable e) {
            Log.w(TAG, "Failed to close interpreter", e);
        }
    }
}
