package org.btik.server.video.device.udp2;


import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferPool {
    /**
     * 接收图片缓冲区大小，<br>
     * 与TCP不同的是，若图片大于当前帧大小,会截断则无法得到完整图片，默认40KB
     */
    private static final int RECEIVE_BUFFER_SIZE = 64512;

    /**
     * 帧缓冲池，避免反复new帧缓冲区
     */
    private final ConcurrentLinkedQueue<FrameBuffer> frameBufferPool = new ConcurrentLinkedQueue<>();

    /**
     * 初始缓存区池大小，本身会自动扩容，随着设备增多可以设置合理值
     */
    private int bufferPoolSize = 500;

    public void setBufferPoolSize(int bufferPoolSize) {
        this.bufferPoolSize = bufferPoolSize;
    }

    public int getBufferPoolSize() {
        return bufferPoolSize;
    }

    private void init() {
        for (int i = 0; i < bufferPoolSize; i++) {
            frameBufferPool.add(new FrameBuffer(new byte[RECEIVE_BUFFER_SIZE]));
        }
    }

    public FrameBuffer getFrameBuffer() {
        FrameBuffer buffer = frameBufferPool.poll();
        if (buffer == null) {
            System.out.println("mem up");
            // 自动扩容
            buffer = new FrameBuffer(new byte[RECEIVE_BUFFER_SIZE]);
        }
        return buffer;
    }

    public void returnBuffer(FrameBuffer buffer) {
        frameBufferPool.add(buffer);

    }

    public BufferPool() {
        init();
    }
}
