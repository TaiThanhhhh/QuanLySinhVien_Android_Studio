package com.example.quanlysinhvien.ui.student.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Pair;

import com.google.mlkit.vision.face.FaceLandmark;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FaceUtils {

    // Serialize landmarks including landmark type
    public static String landmarksToJson(List<FaceLandmark> landmarks) {
        if (landmarks == null) return null;
        try {
            JSONObject json = new JSONObject();
            JSONArray array = new JSONArray();
            for (FaceLandmark landmark : landmarks) {
                if (landmark == null) continue;
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

    // Parse typed landmarks into list of (type, PointF)
    public static List<Pair<Integer, PointF>> jsonToTypedLandmarks(String jsonString) throws Exception {
        List<Pair<Integer, PointF>> landmarks = new ArrayList<>();
        if (jsonString == null || jsonString.isEmpty()) return landmarks;
        JSONObject json = new JSONObject(jsonString);
        JSONArray array = json.getJSONArray("landmarks");
        for (int i = 0; i < array.length(); i++) {
            JSONObject point = array.getJSONObject(i);
            int type = point.has("type") ? point.getInt("type") : -1;
            float x = (float) point.getDouble("x");
            float y = (float) point.getDouble("y");
            landmarks.add(new Pair<>(type, new PointF(x, y)));
        }
        return landmarks;
    }

    // Pack embedding into a JSON string along with landmarks
    public static String packTemplateWithEmbedding(String landmarksJson, float[] embedding) {
        try {
            JSONObject root = new JSONObject();
            if (landmarksJson != null && !landmarksJson.isEmpty()) {
                JSONObject landmarksObj = new JSONObject(landmarksJson);
                root.put("landmarks", landmarksObj.getJSONArray("landmarks"));
            }
            if (embedding != null) {
                JSONArray emb = new JSONArray();
                for (float v : embedding) emb.put(v);
                root.put("embedding", emb);
            }
            return root.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static float[] extractEmbeddingFromTemplate(String jsonString) throws Exception {
        if (jsonString == null || jsonString.isEmpty()) return null;
        JSONObject root = new JSONObject(jsonString);
        if (!root.has("embedding")) return null;
        JSONArray emb = root.getJSONArray("embedding");
        float[] res = new float[emb.length()];
        for (int i = 0; i < emb.length(); i++) res[i] = (float) emb.getDouble(i);
        return res;
    }

    public static boolean assetExists(Context context, String assetName) {
        try {
            String[] assets = context.getAssets().list("");
            if (assets == null) return false;
            for (String a : assets) if (a.equals(assetName)) return true;
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    // Align the preview bitmap by the eye line (if available), optionally flip horizontally (for front camera),
    // then crop an expanded bounding box. Returns a cropped Bitmap or null on failure.
    public static Bitmap alignCrop(Bitmap previewBitmap, List<FaceLandmark> landmarks, Rect bbox, boolean flipHorizontally, int marginPercent) {
        if (previewBitmap == null || bbox == null) return null;
        float width = previewBitmap.getWidth();
        float height = previewBitmap.getHeight();

        // Find left and right eye landmarks
        PointF leftEye = null, rightEye = null;
        if (landmarks != null) {
            for (FaceLandmark lm : landmarks) {
                if (lm == null) continue;
                int t = lm.getLandmarkType();
                if (t == FaceLandmark.LEFT_EYE) leftEye = lm.getPosition();
                if (t == FaceLandmark.RIGHT_EYE) rightEye = lm.getPosition();
            }
        }

        // Compute angle (degrees) to rotate so eyes are horizontal
        float angle = 0f;
        if (leftEye != null && rightEye != null) {
            float dy = rightEye.y - leftEye.y;
            float dx = rightEye.x - leftEye.x;
            angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        }

        // Build transform matrix: optional flip around center, then rotate around center
        Matrix m = new Matrix();
        float cx = width / 2f;
        float cy = height / 2f;
        if (flipHorizontally) {
            m.postScale(-1f, 1f, cx, cy);
        }
        m.postRotate(-angle, cx, cy);

        // Create rotated bitmap
        Bitmap rotated = Bitmap.createBitmap(previewBitmap, 0, 0, (int) width, (int) height, m, true);

        // Map bbox corners through same matrix
        float[] pts = new float[]{bbox.left, bbox.top, bbox.right, bbox.top, bbox.right, bbox.bottom, bbox.left, bbox.bottom};
        m.mapPoints(pts);

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < pts.length; i += 2) {
            float x = pts[i];
            float y = pts[i + 1];
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // Expand by marginPercent of max dimension
        float bw = maxX - minX;
        float bh = maxY - minY;
        float margin = Math.max(bw, bh) * (marginPercent / 100f);
        int left = (int) Math.max(0, Math.floor(minX - margin));
        int top = (int) Math.max(0, Math.floor(minY - margin));
        int right = (int) Math.min(rotated.getWidth(), Math.ceil(maxX + margin));
        int bottom = (int) Math.min(rotated.getHeight(), Math.ceil(maxY + margin));

        if (right <= left || bottom <= top) return null;

        try {
            return Bitmap.createBitmap(rotated, left, top, right - left, bottom - top);
        } catch (Exception e) {
            return null;
        }
    }
}
