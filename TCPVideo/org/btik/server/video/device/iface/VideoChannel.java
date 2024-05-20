package org.btik.server.video.device.iface;

public interface VideoChannel {
    /**
     * 字节数组发送给不同客户端
     *
     * @param frame 一帧jpeg
     */
    void sendFrame(byte[] frame, int len);

    /**
     * 字节数组发送给不同客户端
     *
     * @param frame 一帧jpeg
     */
    void sendFrame(byte[][] frame, int[] len, int segmentCount);
}
