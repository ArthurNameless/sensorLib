// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.sensor.facedetection;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Camera;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.sensor.SensorDelegate;
import com.sensor.VisionProcessorBase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import com.sensor.common.BitmapUtils;
import com.sensor.common.FrameMetadata;
import com.sensor.common.GraphicOverlay;
import com.sensor.common.RNFileUtils;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.IterativeLinearSolver;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.NonSquareOperatorException;
import org.apache.commons.math3.linear.RealLinearOperator;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Face Detector Demo.
 */
public class FaceDetectionProcessor extends VisionProcessorBase<List<FirebaseVisionFace>> {

    private static final String TAG = "FaceDetectionProcessor";
    private static final Float DENFLATION_COEF = 0.2f;
    private static final Float INFLATION_COEF = 1.4f;
    private static final float downsample = 2.0f;
    private CustomEncoder customEncoder;
    // TODO probably remove? It is not like a good place to send requests.
    private final RequestQueue requestQueue;

    private final FirebaseVisionFaceDetector detector;
    private final Context context;

    private final Bitmap overlayBitmap;

    private SensorDelegate mDelegate;
//    private WritableArray faceURIs;

    public FaceDetectionProcessor(Resources resources, @NonNull SensorDelegate delegate, Context context) {
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .enableTracking()
                        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
                        .setContourMode(FirebaseVisionFaceDetectorOptions.NO_CONTOURS)
                        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                        .build();

        this.context = context;
        requestQueue = Volley.newRequestQueue(context);
        detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
        mDelegate = delegate;
        overlayBitmap = null;
        try {
            customEncoder = new CustomEncoder();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        try {
            detector.close();
        } catch (IOException e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: " + e);
        }
    }

    @Override
    protected Task<List<FirebaseVisionFace>> detectInImage(FirebaseVisionImage originalImage) {
        Bitmap bitmap = originalImage.getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                (int)(bitmap.getWidth() / downsample),
                (int)(bitmap.getHeight() / downsample),
                false);
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(scaledBitmap);
        return detector.detectInImage(image);
    }

    @Override
    protected void onSuccess(
            @Nullable Bitmap originalCameraImage,
            @NonNull List<FirebaseVisionFace> faces,
            @NonNull FrameMetadata frameMetadata,
            @NonNull GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        if (originalCameraImage != null) {
//            CameraImageGraphic imageGraphic = new CameraImageGraphic(graphicOverlay, originalCameraImage);
//            graphicOverlay.add(imageGraphic);
        }

//        for (int i = 0; i < faces.size(); ++i) {
//            FirebaseVisionFace face = faces.get(i);
//            int cameraFacing =
//                    frameMetadata != null ? frameMetadata.getCameraFacing() :
//                            Camera.CameraInfo.CAMERA_FACING_BACK;
//            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, face, cameraFacing, overlayBitmap, downsample);
//            graphicOverlay.add(faceGraphic);
//        }
        graphicOverlay.postInvalidate();

        if (originalCameraImage != null) {
            Log.d(TAG, "Height" + frameMetadata.getHeight());
            Log.d(TAG, "Width" + frameMetadata.getWidth());
            Log.d(TAG, "Rotation" + frameMetadata.getRotation());
            Log.d(TAG, "CameraFacing" + frameMetadata.getCameraFacing());
            WritableArray faceData = extractFaces(originalCameraImage, faces);
            mDelegate.onFacesDetected(faceData);
        }
    }

    private Bitmap transformImage(Bitmap image, FirebaseVisionFace face) {
        FirebaseVisionPoint mouthLeft = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT).getPosition();
        FirebaseVisionPoint mouthRight = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT).getPosition();
        FirebaseVisionPoint noseBase = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE).getPosition();
        FirebaseVisionPoint leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE).getPosition();
        FirebaseVisionPoint rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE).getPosition();

        final double[][] srcValues = new double[][] {
                new double[]{mouthLeft.getX(),  mouthLeft.getY(),  1.0f},
                new double[]{mouthRight.getX(), mouthRight.getY(), 1.0f},
                new double[]{noseBase.getX(),   noseBase.getY(),   1.0f},
                new double[]{leftEye.getX(),    leftEye.getY(),    1.0f},
                new double[]{rightEye.getX(),   rightEye.getY(),   1.0f}
        };

        final double[][] refValues = new double[][] {
                new double[]{30.29459953, 51.69630051, 1.0f},
                new double[]{65.53179932, 51.50139999, 1.0f},
                new double[]{48.02519989, 71.73660278, 1.0f},
                new double[]{33.54930115, 92.3655014,  1.0f},
                new double[]{62.72990036, 92.20410156, 1.0f},
        };

        final RealMatrix a = MatrixUtils.createRealMatrix(refValues);
        final RealMatrix b = MatrixUtils.createRealMatrix(srcValues);

        final SingularValueDecomposition svd = new SingularValueDecomposition(a);
        final DecompositionSolver solver = svd.getSolver();
        final RealMatrix x = solver.solve(b);

        Log.d(TAG, x.toString());

//        final ImageView view = new ImageView(context);
//        view.setImageBitmap(image);
//        view.setScaleType(ImageView.ScaleType.MATRIX);
        Matrix transformMatrix = new Matrix();
        transformMatrix.setValues(new float[]{(float)x.getEntry(0,0), (float)x.getEntry(1,0), (float)x.getEntry(2,0),
                (float)x.getEntry(0,1), (float)x.getEntry(1,1), (float)x.getEntry(2,1),
                (float)x.getEntry(0,2), (float)x.getEntry(1,2), (float)x.getEntry(2,2)});
//        view.setImageMatrix(transformMatrix);
        Canvas canvas = new Canvas();
        // canvas.setBitmap(image);
//        view.draw(canvas);
        canvas.drawBitmap(image, transformMatrix, null);

        return image;
    }

    private void sendVectorBytes(float[][] vector) {
        ByteBuffer vectorBuffer = ByteBuffer.allocate(2048);
        for (int i = 0; i < 512; i++) {
            vectorBuffer.putFloat(vector[0][i]);
        }
        byte[] vectorBytes = vectorBuffer.order(ByteOrder.BIG_ENDIAN).array();
        try {
            String url = "http://192.168.0.197:8020/";
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Authorization", mDelegate.getAuthToken());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addBinaryBody("vector1", vectorBytes, ContentType.APPLICATION_OCTET_STREAM, "vector1");
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity _responseEntity = response.getEntity();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    private WritableArray extractFaces(Bitmap originalCameraImage, List<FirebaseVisionFace> faces) {
        String cacheDirectory = mDelegate.getCacheDirectory();
        WritableArray faceData = Arguments.createArray();
        if (cacheDirectory != null) {
            for (FirebaseVisionFace face : faces) {
                // Bitmap scaledBitmap = transformImage(originalCameraImage, face);

                int x = (int) (downsample * face.getBoundingBox().exactCenterX() - (downsample * face.getBoundingBox().width() / 2) - (face.getBoundingBox().width() * DENFLATION_COEF));
                int y = (int) (downsample * face.getBoundingBox().exactCenterY() - (downsample * face.getBoundingBox().height() / 2) - (face.getBoundingBox().height() * DENFLATION_COEF));
                int height = (int) (downsample * face.getBoundingBox().height() * INFLATION_COEF);
                int width = (int) (downsample * face.getBoundingBox().width() * INFLATION_COEF);
                Bitmap cropped;
                Bitmap scaledBitmap;
                try {
                    cropped = Bitmap.createBitmap(originalCameraImage, x, y, width, height);
                    scaledBitmap = BitmapUtils.scaleBitmap(cropped);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                ByteArrayOutputStream imageStream = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, imageStream);
                String fileUri = "";
                try {
                    String filePath = writeStreamToFile(cacheDirectory, imageStream);
                    File imageFile = new File(filePath);
                    fileUri = Uri.fromFile(imageFile).toString();
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                WritableMap trackedFace = Arguments.createMap();
                trackedFace.putString("fileURI", fileUri);
                trackedFace.putString("trackingId", Integer.toString(face.getTrackingId()));
                faceData.pushMap(trackedFace);

                // Encoding starts here
                if (customEncoder == null) {
                    Log.e(TAG, "Custom encoder failed to load.");
                } else {
                    try {
                        customEncoder.encodeFaceBitmap(cropped).addOnSuccessListener(
                                result -> {
                                    Log.d(TAG, "Face encoded.");
                                    // sendVectorBytes(result);

                                }
                        );
                    } catch (FirebaseMLException e) {
                        e.printStackTrace();
                    }
                }


            }
        }
        return faceData;
    }

    private String writeStreamToFile(String cacheDirectory, ByteArrayOutputStream inputStream) throws IOException {
        String outputPath = null;
        IOException exception = null;
        FileOutputStream outputStream = null;

        try {
            outputPath = RNFileUtils.getOutputFilePath(cacheDirectory, ".jpg");
            outputStream = new FileOutputStream(outputPath);
            inputStream.writeTo(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            exception = e;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (exception != null) {
            throw exception;
        }

        return outputPath;
    }
}
