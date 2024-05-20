package org.btik.server.video;

import org.btik.server.video.device.task.AsyncTaskExecutor;
import org.btik.server.video.device.udp.BufferPool;
import org.btik.server.video.device.udp.UDPDeviceChannel;
import org.btik.server.video.device.web.BioHttpVideoServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UDPMain {
    static Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(new FileInputStream("light-video.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取配置信息
     *
     * @param key 配置key
     * @param def 获取为空时的默认值
     */
    private static int getIntProp(String key, int def) {
        Object o = properties.get(key);
        if (o == null) {
            return def;
        }
        return Integer.parseInt(o.toString());
    }

    public static void main(String[] args) {
        AsyncTaskExecutor asyncTaskExecutor = new AsyncTaskExecutor();
        asyncTaskExecutor.start();

        BioHttpVideoServer bioHttpVideoServer = new BioHttpVideoServer();
        bioHttpVideoServer.setHttpPort(getIntProp("http.port", 8003));
        bioHttpVideoServer.setAsyncTaskExecutor(asyncTaskExecutor);

        BufferPool bufferPool = new BufferPool();
        bufferPool.setBufferPoolSize(getIntProp("udp.video.buffer.pool.size", 500));
        UDPDeviceChannel deviceChannel = new UDPDeviceChannel();
        deviceChannel.setAsyncTaskExecutor(asyncTaskExecutor);
        deviceChannel.setVideoServer(bioHttpVideoServer);

        deviceChannel.setBufferPool(bufferPool);
        deviceChannel.setStreamPort(getIntProp("stream.port", 8004));
        deviceChannel.setDispatcherPoolSize(getIntProp("udp.video.dispatcher.thread.size", 8));
        deviceChannel.start();

        bioHttpVideoServer.setDevChannel(deviceChannel);
        bioHttpVideoServer.start();
    }
}
