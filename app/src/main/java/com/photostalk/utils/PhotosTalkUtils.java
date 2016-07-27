package com.photostalk.utils;

import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created by mohammed on 4/29/16.
 */
public class PhotosTalkUtils {
    public static boolean saveWave(String fileName, int samplingRate, short numberOfChannels, short[] data, int length) {
        int bitsPerSample = 16;
        int bytesPerSample = bitsPerSample / 8;

        int chunkSize = 36 + (length * bytesPerSample);
        int byteRate = samplingRate * numberOfChannels * bytesPerSample;
        int blockAlign = numberOfChannels * bytesPerSample;
        int chunk2Size = bytesPerSample * length;

        byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (chunkSize & 0xFF);
        header[5] = (byte) ((chunkSize >> 8) & 0xFF);
        header[6] = (byte) ((chunkSize >> 16) & 0xFF);
        header[7] = (byte) ((chunkSize >> 24) & 0xFF);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = (byte) 16;
        header[17] = (byte) 0;
        header[18] = (byte) 0;
        header[19] = (byte) 0;
        header[20] = (byte) 1;
        header[21] = (byte) 0;
        header[22] = (byte) (numberOfChannels & 0xFF);
        header[23] = (byte) ((numberOfChannels >> 8) & 0xFF);
        header[24] = (byte) (samplingRate & 0xFF);
        header[25] = (byte) ((samplingRate >> 8) & 0xFF);
        header[26] = (byte) ((samplingRate >> 16) & 0xFF);
        header[27] = (byte) ((samplingRate >> 24) & 0xFF);
        header[28] = (byte) (byteRate & 0xFF);
        header[29] = (byte) ((byteRate >> 8) & 0xFF);
        header[30] = (byte) ((byteRate >> 16) & 0xFF);
        header[31] = (byte) ((byteRate >> 24) & 0xFF);
        header[32] = (byte) (blockAlign & 0xFF);
        header[33] = (byte) ((blockAlign >> 8) & 0xFF);
        header[34] = (byte) 16;
        header[35] = (byte) 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (chunk2Size & 0xFF);
        header[41] = (byte) ((chunk2Size >> 8) & 0xFF);
        header[42] = (byte) ((chunk2Size >> 16) & 0xFF);
        header[43] = (byte) ((chunk2Size >> 24) & 0xFF);

        byte[] dataBytes = new byte[chunk2Size];
        int cursor = 0;
        for (int i = 0; i < length; i++) {
            short value = data[i];
            dataBytes[cursor++] = (byte) (value & 0xFF);
            dataBytes[cursor++] = (byte) ((value >> 8) & 0xFF);
        }

        try {
            OutputStream outputStream = new FileOutputStream(fileName);
            outputStream.write(header, 0, header.length);
            outputStream.write(dataBytes, 0, dataBytes.length);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getDurationFormatted(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return ((minutes < 100) ? "0" : "") + minutes + ":" + ((seconds < 10) ? "0" : "") + seconds;
    }


}
