package com.sensorlib.sensor.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.SystemClock;

import androidx.annotation.NonNull;

import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A {@code FirebaseModelInterpreter} based image classifier.
 */
public class CustomEncoder {

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "CustomModel:Encoder";

    /**
     * Name of the floating point model uploaded to the Firebase console.
     */
    private static final String MODEL_NAME = "converted_model.tflite";

    /**
     * Dimensions of inputs.
     */
    private static final int DIM_BATCH_SIZE = 1;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int DIM_IMG_SIZE_X = 112;
    private static final int DIM_IMG_SIZE_Y = 112;
    private static final int DIM_OUTPUT_SIZE = 512;
    private static final int QUANT_NUM_OF_BYTES_PER_CHANNEL = 1;
    private static final int FLOAT_NUM_OF_BYTES_PER_CHANNEL = 4;

    /* Preallocated buffers for storing image data in. */
    private final int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    /**
     * An instance of the driver class to run model inference with Firebase.
     */
    private FirebaseModelInterpreter interpreter;

    /**
     * Data configuration of input & output data of model.
     */
    private final FirebaseModelInputOutputOptions dataOptions;

    private ByteBuffer imgData;

    /**
     * Initializes an {@code CustomImageClassifier}.
     */
    CustomEncoder() throws FirebaseMLException {

        final FirebaseCustomLocalModel localModel =
                new FirebaseCustomLocalModel.Builder().setAssetFilePath(MODEL_NAME).build();
        FirebaseModelInterpreterOptions interpreterOptions =
                new FirebaseModelInterpreterOptions.Builder(localModel).build();
        try {
            interpreter =
                    FirebaseModelInterpreter.getInstance(interpreterOptions);
        } catch (FirebaseMLException e) {
            Log.e(TAG, "Failed to build FirebaseModelInterpreter. ", e);
        }

        Log.d(TAG, "Created a Custom Encoder.");
        int[] inputDims = {DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE};
        int[] outputDims = {DIM_BATCH_SIZE, DIM_OUTPUT_SIZE};

        int dataType = FirebaseModelDataType.FLOAT32;
        dataOptions =
                new FirebaseModelInputOutputOptions.Builder()
                        .setInputFormat(0, dataType, inputDims)
                        .setOutputFormat(0, dataType, outputDims)
                        .build();
        Log.d(TAG, "Configured input & output data for the custom image classifier.");
    }

    /**
     * Classifies a frame from the preview stream.
     */
    Task<float[][]> encodeFaceBitmap(Bitmap face)
            throws FirebaseMLException {
        if (interpreter == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            List<String> uninitialized = new ArrayList<>();
            uninitialized.add("Uninitialized Classifier.");
            Tasks.forResult(uninitialized);
        }
        long startTime = SystemClock.uptimeMillis();
        // Create input data.
        loadBitmap(face);

        FirebaseModelInputs inputs = new FirebaseModelInputs.Builder().add(this.imgData).build();
        // Here's where the magic happens!!
        return interpreter
                .run(inputs, dataOptions)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Model run error: " + e.getMessage());
                        e.printStackTrace();
                    }
                })
                .continueWith(
                        new Continuation<FirebaseModelOutputs, float[][]>() {
                            @Override
                            public float[][] then(@NonNull Task<FirebaseModelOutputs> task) throws Exception {
                                float[][] outputs = task.getResult().<float[][]>getOutput(0);
                                long endTime = SystemClock.uptimeMillis();
                                Log.d(TAG, "Total timecost to encode face: " + (endTime - startTime));
                                Log.d(TAG, "Output dimensions number: " + outputs.length);
                                Log.d(TAG, "First dimension size: " + outputs[0].length);
                                return outputs;
                            }
                        });
    }

    private synchronized void loadBitmap(Bitmap bitmap) {
        if (this.imgData == null) {  // 4 stands for float32
            this.imgData = ByteBuffer.allocateDirect(4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        } else {
            this.imgData.clear();
        }

        this.imgData.order(ByteOrder.nativeOrder());
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);
        this.imgData.rewind();

        Log.d(TAG, "length: " + intValues.length + " width: " + scaledBitmap.getWidth() + " height: " + scaledBitmap.getHeight());
        scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(),
                scaledBitmap.getHeight());
        // Convert the image to int points.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                this.imgData.putFloat(((val >> 16) & 0xFF) / 127.5f - 1.0f);
                this.imgData.putFloat(((val >> 8) & 0xFF) / 127.5f - 1.0f);
                this.imgData.putFloat((val & 0xFF) / 127.5f - 1.0f);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffer: " + (endTime - startTime));

    }

}
