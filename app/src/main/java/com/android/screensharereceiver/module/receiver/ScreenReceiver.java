package com.android.screensharereceiver.module.receiver;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.android.screensharereceiver.module.connection.tcp.TcpConnection;
import com.android.screensharereceiver.common.constant.Constants;
import com.android.screensharereceiver.common.utils.ByteUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 获取屏幕内容，进行编码后发送至传屏接收端
 */
public class ScreenReceiver implements Runnable {

    private static final String TAG = "ScreenReceiver";
    private Surface surface;
    private MediaCodec decoder;
    private int width;
    private int height;
    private String mimeType;
    private int frameRate;
    private int bitRate;
    private int IFrameInterval;

    private AtomicBoolean quit = new AtomicBoolean(false);

    private ByteBuffer inputBuffer;
    private ByteBuffer outputBuffer;
    private MediaFormat inputFormat;
    MediaFormat mediaFormat;
    private ExecutorService executorService;

    private TcpConnection tcpConnection = TcpConnection.getInstance();
    private CmdListener cmdListener;

    public interface CmdListener {
        void onReceiveDisconnectCmd();
        void onReceiveStartScreenShare();
        void onReceiveStopScreenShare();
    }

    /**
     * @param surface 播放视频的Surface
     * @param width 虚拟显示的宽度（像素）
     * @param height 虚拟显示的高度（像素）
     * @param mimeType mime类型
     * @param frameRate 帧率
     * @param bitRate 码率（每秒传送的比特数）
     * @param IFrameInterval I帧间隔
     */
    public ScreenReceiver(Surface surface, int width, int height, String mimeType, int frameRate,
                          int bitRate, int IFrameInterval, CmdListener cmdListener) {
        Log.i(TAG, "new ScreenReceiver");
        this.surface = surface;
        this.width = width;
        this.height = height;
        this.mimeType = mimeType;
        this.frameRate = frameRate;
        this.bitRate = bitRate;
        this.IFrameInterval = IFrameInterval;
        this.cmdListener = cmdListener;
        initThreadPool();
    }

    public ScreenReceiver(Surface surface, CmdListener cmdListener) {
        this(surface, 360, 640, "video/avc", 30,
                6000000, 10, cmdListener);
    }

    private void initThreadPool() {
        Log.i(TAG, "initThreadPool");
        int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        int KEEP_ALIVE_TIME = 1;
        TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        executorService = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES * 2,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, taskQueue);
    }

    @Override
    public void run() {
        Log.i(TAG, "run");
        prepareDecoder();
    }

    private void prepareDecoder() {
        Log.i(TAG, "prepareDecoder");
        // width 内容的宽度(以像素为单位) height 内容的高度(以像素为单位)
        mediaFormat = MediaFormat.createVideoFormat(mimeType, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFrameInterval);

        try {
            decoder = MediaCodec.createDecoderByType(mimeType);
            decoder.configure(mediaFormat, surface, null, 0);
            decoder.start();
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    while (!quit.get()) {
                        int inputIndex = decoder.dequeueInputBuffer(10000);
                        if (inputIndex >= 0) {
                            inputBuffer = decoder.getInputBuffer(inputIndex);
                            // 获取数据
                            byte[] result = tcpConnection.receiveData();
                            if (result == null) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, 0);
                                continue;
                            }
                            // 如果收到开始传屏消息
                            if (ByteUtils.bytesToInt(result) == Constants.START_SCREEN_SHARE) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, 0);
                                cmdListener.onReceiveStartScreenShare();
                                continue;
                            }
                            // 如果收到停止传屏消息
                            if (ByteUtils.bytesToInt(result) == Constants.STOP_SCREEN_SHARE) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, 0);
                                cmdListener.onReceiveStopScreenShare();
                                continue;
                            }
                            // 如果收到断开连接消息
                            if (ByteUtils.bytesToInt(result) == Constants.DISCONNECT) {
                                decoder.queueInputBuffer(inputIndex, 0, 0, 0, 0);
                                stop();
                                if (cmdListener != null) {
                                    cmdListener.onReceiveDisconnectCmd();
                                }
                                continue;
                            }

                            inputBuffer.put(result);

                            decoder.queueInputBuffer(inputIndex, 0, result.length, 0, 0);
                        }
                        int outputIndex = decoder.dequeueOutputBuffer(new MediaCodec.BufferInfo(), 10000);
                        if (outputIndex >= 0) {
                            decoder.releaseOutputBuffer(outputIndex, true);
                        }
                    }
                    release();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        Log.i(TAG, "stop");
        quit.set(true);
    }

    public void release() {
        Log.i(TAG, "release");
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
    }
}
