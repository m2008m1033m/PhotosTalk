package com.photostalk.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by mohammed on 2/27/16.
 */
public class FileUtils {

    public static void copy(String srcName, String dstName) throws IOException {
        InputStream in = new FileInputStream(srcName);
        OutputStream out = new FileOutputStream(dstName);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

}
