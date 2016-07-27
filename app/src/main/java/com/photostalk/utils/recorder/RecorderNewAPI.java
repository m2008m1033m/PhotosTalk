package com.photostalk.utils.recorder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import com.photostalk.R;
import com.photostalk.utils.Notifications;
import com.photostalk.utils.PhotosTalkUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;


@TargetApi(18)
public class RecorderNewAPI extends Recorder {

    private static final int SAMPLING_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mAudioRecord;

    private short[] mRecordedData;
    private int mTotalShorts;
    private int mElapsedTime;
    private boolean mHasRecorded;

    public RecorderNewAPI(Activity activity, OnRecordingListener onRecordingListener, MediaPlayer.OnCompletionListener onCompletionListener) {
        super(activity, onRecordingListener, onCompletionListener);
    }

    @Override
    public boolean record(final int maxDuration) {
        if (getState() != NONE) return false;

        final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        final short[] buffer = new short[minBufferSize];

        /**
         * maximum possible number of shorts, used to store the whole audio data
         */
        mRecordedData = new short[SAMPLING_RATE * maxDuration];
        mTotalShorts = 0;


        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize
        );

        setState(RECORDING);


        /**
         * processing the bytes from the recording object
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                final long recordingStartingTime = System.currentTimeMillis();
                mAudioRecord.startRecording();
                int currentOffset = 0;

                // runnable for invoking the onUpdate method
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        /**
                         * updating via the complete buffer
                         */
                        mOnRecordingListener.onUpdate(buffer, minBufferSize, mElapsedTime);

                        short max = 0;
                        for (short value : buffer) {
                            if (Math.abs(value) > max)
                                max = (short) Math.abs(value);
                        }

                        /**
                         * update via the max value (amplitude)
                         */
                        mOnRecordingListener.onUpdate(max, mElapsedTime);
                    }
                };

                // handler for the updating runnable
                Handler h = new Handler(Looper.getMainLooper());

                while (mAudioRecord != null && mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    /**
                     * check the time:
                     */
                    mElapsedTime = (int) ((System.currentTimeMillis() - recordingStartingTime) / 1000.0f);
                    if (mElapsedTime >= maxDuration) {
                        cancel();
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                mOnRecordingListener.onMaxDurationReached();
                            }
                        });

                        break;
                    }

                    /**
                     * read fully
                     */
                    int length = minBufferSize;
                    int offset = 0;
                    while (length > 0 && mAudioRecord != null) {
                        synchronized (mAudioRecord) {
                            int read = mAudioRecord.read(buffer, offset, length);
                            length -= read;
                            offset += read;
                        }
                    }

                    /**
                     * copy new shorts to the recorded data
                     */
                    if (mRecordedData == null)
                        break; // just in case it was stopped from other threads
                    System.arraycopy(buffer, 0, mRecordedData, currentOffset, minBufferSize);
                    mTotalShorts += minBufferSize;
                    currentOffset += minBufferSize;

                    /**
                     * notify for the new data
                     */
                    if (mOnRecordingListener != null)
                        h.post(r);

                    try {
                        Thread.sleep(17);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        ).start();

        return true;
    }

    @Override
    public void stop() {
        stop(false);
    }

    @Override
    public void cancel() {
        stop(true);
    }

    @Override
    public boolean hasRecorded() {
        return mHasRecorded;
    }

    @Override
    public void clean() {
        mHasRecorded = false;
    }

    public static final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
    public static final int COMPRESSED_AUDIO_FILE_BIT_RATE = 64000; // 64kbps
    public static final int SAMPLING_RATE_NEW = 44100;
    public static final int BUFFER_SIZE = 44100;
    public static final int CODEC_TIMEOUT_IN_MS = 5000;

    private synchronized void stop(boolean isCancelled) {
        if (getState() != RECORDING) return;
        mAudioRecord.stop();
        mAudioRecord.release();
        mAudioRecord = null;
        setState(NONE);
        if (!isCancelled) {
            if (!PhotosTalkUtils.saveWave(getFileName() + ".wav", SAMPLING_RATE, (short) 1, mRecordedData, mTotalShorts))
                Notifications.showSnackbar(mActivity, mActivity.getString(R.string.error_while_saving_file));
            else {
                /**
                 * save mp4 file
                 */
                try {
                    String filePath = getFileName() + ".wav";
                    File inputFile = new File(filePath);
                    FileInputStream fis = new FileInputStream(inputFile);

                    File outputFile = new File(getFileName());
                    if (outputFile.exists()) outputFile.delete();

                    MediaMuxer mux = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                    MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, SAMPLING_RATE_NEW, 1);
                    outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                    outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);
                    outputFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

                    MediaCodec codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
                    codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    codec.start();

                    ByteBuffer[] codecInputBuffers = codec.getInputBuffers(); // Note: Array of buffers
                    ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

                    MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();
                    byte[] tempBuffer = new byte[BUFFER_SIZE];
                    boolean hasMoreData = true;
                    double presentationTimeUs = 0;
                    int audioTrackIdx = 0;
                    int totalBytesRead = 0;
                    int percentComplete = 0;
                    do {
                        int inputBufIndex = 0;
                        while (inputBufIndex != -1 && hasMoreData) {
                            inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

                            if (inputBufIndex >= 0) {
                                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                                dstBuf.clear();

                                int bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                                if (bytesRead == -1) { // -1 implies EOS
                                    hasMoreData = false;
                                    codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                } else {
                                    totalBytesRead += bytesRead;
                                    dstBuf.put(tempBuffer, 0, bytesRead);
                                    codec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);
                                    presentationTimeUs = 1000000l * (totalBytesRead / 2) / SAMPLING_RATE;
                                }
                            }
                        }
                        // Drain audio
                        int outputBufIndex = 0;
                        while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                            outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS);
                            if (outputBufIndex >= 0) {
                                ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                                encodedData.position(outBuffInfo.offset);
                                encodedData.limit(outBuffInfo.offset + outBuffInfo.size);
                                if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
                                    codec.releaseOutputBuffer(outputBufIndex, false);
                                } else {
                                    mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo);
                                    codec.releaseOutputBuffer(outputBufIndex, false);
                                }
                            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                outputFormat = codec.getOutputFormat();
                                audioTrackIdx = mux.addTrack(outputFormat);
                                mux.start();
                            } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                                // NO OP
                            } else {
                            }
                        }
                        percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0);
                    } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    fis.close();
                    mux.stop();
                    mux.release();
                    inputFile.delete();
                    mHasRecorded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                /**
                 * end
                 */


            }


        }
        mRecordedData = null;
        mTotalShorts = 0;
    }

}
