package org.btik.server.video.device.udp2;

public class FrameBuffer {
    // 2 + 4 + 2字节 2 字节的0 4字节ip 2字节端口
    volatile int channelIndex = -1;

    byte[] data;

    volatile int size;

    volatile long time;

    public FrameBuffer(byte[] data) {
        this.data = data;
    }
}
