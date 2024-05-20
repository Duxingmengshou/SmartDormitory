package org.btik.server.video;

import org.btik.server.video.device.task.AsyncTaskExecutor;
import org.btik.server.video.device.udp2.BufferPool;
import org.btik.server.video.device.udp2.NewUDPDeviceChannel;
import org.btik.server.video.device.udp2.bind.DeviceSnChannelBinder;
import org.btik.server.video.device.web.BioHttpVideoServer;
import org.btik.server.video.device.web.SoSendTimeoutTicker;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class UDP2Main {
    private static final String version = "0.0.3.1";
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
        System.out.println("----- version:" + version + " -----\r\n[gitcode page] => https://gitcode.net/qq_26700087/simpleVideoServer \r\n");
        AsyncTaskExecutor asyncTaskExecutor = new AsyncTaskExecutor();
        asyncTaskExecutor.start();

        BioHttpVideoServer bioHttpVideoServer = new BioHttpVideoServer();
        bioHttpVideoServer.setHttpPort(getIntProp("http.port", 8003));
        bioHttpVideoServer.setAsyncTaskExecutor(asyncTaskExecutor);
        int videoChannelCount = getIntProp("udp.video.channel.size", 128);
        DeviceSnChannelBinder udpDeviceSnChannelBinder = new DeviceSnChannelBinder(videoChannelCount,
                getIntProp("stream.bind.port", 8004) );
        BufferPool bufferPool = new BufferPool();
        bufferPool.setBufferPoolSize(getIntProp("udp.video.buffer.pool.size", 500));
        NewUDPDeviceChannel deviceChannel = new NewUDPDeviceChannel();
        deviceChannel.setVideoServer(bioHttpVideoServer);

        deviceChannel.setBufferPool(bufferPool);
        deviceChannel.setStreamPort(getIntProp("stream.port", 8004));
        deviceChannel.setDispatcherPoolSize(getIntProp("udp.video.dispatcher.thread.size", 8));
        deviceChannel.setFrameDelayTimeout(getIntProp("frame.delay.skip.timeout" , 2000));
        deviceChannel.setVideoChannelCount(videoChannelCount);

        SoSendTimeoutTicker soSendTimeoutTicker = new SoSendTimeoutTicker();
        soSendTimeoutTicker.setTimeout(getIntProp("http.video.so.send.timeout" , 300));
        soSendTimeoutTicker.setVideoChannelCount(videoChannelCount);
        soSendTimeoutTicker.start();
        deviceChannel.setSoSendTimeoutTicker(soSendTimeoutTicker);
        udpDeviceSnChannelBinder.start();
        deviceChannel.start();

        udpDeviceSnChannelBinder.setNewUDPDeviceChannel(deviceChannel);
        bioHttpVideoServer.setDevChannel(deviceChannel);
        bioHttpVideoServer.start();
    }
}
