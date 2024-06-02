package org.btik.server;


import org.btik.server.video.device.iface.VideoChannel;

/**
 * 视频服务
 */
public interface VideoServer {

    /**
     * @param channelId 通道号
     */
    VideoChannel createChannel(byte[] channelId);
}
