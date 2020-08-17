package com.sensorlib.sensor.common;

import android.net.Uri;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


public class RNFileUtils {

    public static File ensureDirExists(File dir) throws IOException {
        if (!(dir.isDirectory() || dir.mkdirs())) {
            throw new IOException("Couldn't create directory '" + dir + "'");
        }
        return dir;
    }

    public static String getOutputFilePath(String directory, String extension) throws IOException {
        ensureDirExists(new File(directory));
        String filename = UUID.randomUUID().toString();
        return directory + File.separator + filename + extension;
    }

    public static Uri uriFromFile(File file) {
        return Uri.fromFile(file);
    }

}

