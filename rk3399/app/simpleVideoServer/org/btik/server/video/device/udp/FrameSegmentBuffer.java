package org.btik.server.video.device.udp;

/**
 * 帧片段
 */
public class FrameSegmentBuffer {
    // 2 + 4 + 2字节 2 字节的0 4字节ip 2字节端口
    long address;

    byte[] data;

    int size;

    public FrameSegmentBuffer(byte[] data) {
        this.data = data;
    }
}
