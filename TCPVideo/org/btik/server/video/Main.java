package org.btik.server.video;

import org.btik.server.video.device.task.AsyncTaskExecutor;
import org.btik.server.video.device.tcp.BioDeviceChannel;
import org.btik.server.video.device.web.BioHttpVideoServer;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 启动类
 */
public class Main {
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
    private static String getProp(String key, String def) {
        Object o = properties.get(key);
        if (o == null) {
            return def;
        }
        return String.valueOf(o);
    }

    public static void main(String[] args) {
        AsyncTaskExecutor asyncTaskExecutor = new AsyncTaskExecutor();
        asyncTaskExecutor.start();

        BioHttpVideoServer bioHttpVideoServer = new BioHttpVideoServer();
        bioHttpVideoServer.setHttpPort(Integer.parseInt(
                getProp("http.port", "8003")));
        bioHttpVideoServer.setAsyncTaskExecutor(asyncTaskExecutor);

        BioDeviceChannel bioDeviceChannel = new BioDeviceChannel();
        bioDeviceChannel.setAsyncTaskExecutor(asyncTaskExecutor);
        bioDeviceChannel.setClientsLimit(Integer.parseInt(
                getProp("http.clients.limit", "10")));
        bioDeviceChannel.setVideoServer(bioHttpVideoServer);
        bioDeviceChannel.setStreamPort(Integer.parseInt(
                getProp("stream.port", "8004")));
        bioDeviceChannel.start();

        bioHttpVideoServer.setDevChannel(bioDeviceChannel);
        bioHttpVideoServer.start();
    }

}
