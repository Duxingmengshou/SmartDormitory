package org.btik.server.video.device.udp;

import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferPool {
    /**
     * 接收图片缓冲区大小，<br>
     * 与TCP不同的是，若图片大于当前帧大小,会截断则无法得到完整图片，默认40KB
     */
    private static final int RECEIVE_BUFFER_SIZE = 40960;
    /**
     * 帧平片段缓冲池，避免反复new帧缓冲区
     */
    private final ConcurrentLinkedQueue<FrameSegmentBuffer> frameSegmentBufferPool = new ConcurrentLinkedQueue<>();

    private final ConcurrentLinkedQueue<byte[]> frameSegmentBufferDataPool = new ConcurrentLinkedQueue<>();

    /**
     * 初始缓存区池大小，本身会自动扩容，随着设备增多可以设置合理值
     */
    private int bufferPoolSize = 500;

    public void setBufferPoolSize(int bufferPoolSize) {
        this.bufferPoolSize = bufferPoolSize;
    }

    private void init() {
        for (int i = 0; i < bufferPoolSize; i++) {
            frameSegmentBufferPool.add(new FrameSegmentBuffer(new byte[RECEIVE_BUFFER_SIZE]));
        }
        for (int i = 0; i < bufferPoolSize * 3; i++) {
            frameSegmentBufferDataPool.add(new byte[RECEIVE_BUFFER_SIZE]);
        }
    }

    public FrameSegmentBuffer getFrameBuffer() {
        FrameSegmentBuffer buffer = frameSegmentBufferPool.poll();
        if (buffer == null) {
            System.out.println("mem up");
            // 自动扩容
            buffer = new FrameSegmentBuffer(new byte[RECEIVE_BUFFER_SIZE]);
        }
        return buffer;
    }

    public void returnBuffer(FrameSegmentBuffer buffer) {
        frameSegmentBufferPool.add(buffer);

    }

    public byte[] getBuffer() {
        byte[] buffer = frameSegmentBufferDataPool.poll();
        if (buffer == null) {
            System.out.println("data mem up");
            // 自动扩容
            buffer = new byte[RECEIVE_BUFFER_SIZE];
        }
        return buffer;
    }

    public void returnBufferData(byte[] buffer) {
        if (buffer != null) {
            frameSegmentBufferDataPool.add(buffer);
        }

    }

    public BufferPool() {
        init();
    }
}
