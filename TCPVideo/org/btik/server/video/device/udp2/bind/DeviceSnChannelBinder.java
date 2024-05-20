package org.btik.server.video.device.udp2.bind;


import org.btik.server.util.ByteUtil;
import org.btik.server.video.device.udp2.NewUDPDeviceChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * 绑定sn和通道索引
 *
 * @author lustre
 * @since 2023/2/7 21:51
 */
public class DeviceSnChannelBinder extends Thread {

    private final int port;

    private volatile boolean runFlag = true;

    /**
     * videoChannelSnBinder
     */
    private static final byte[] HEAD = {'S', 'N', ':'};

    /**
     * 可用的通道索引
     */
    private final LinkedList<Integer> channelIndexQueue = new LinkedList<>();

    private final HashMap<String, Integer> snChannelMap = new HashMap<>(64);


    private NewUDPDeviceChannel newUDPDeviceChannel;


    public DeviceSnChannelBinder(int size, int port) {
        super("UDPDeviceSnChannelBinder");
        if (size < 0 || size > 0xff) {
            throw new IllegalArgumentException("The count of channels can only be between 0~255 ");
        }
        for (int i = 0; i < size; i++) {
            channelIndexQueue.add(i);
        }
        this.port = port;
        System.out.printf("snChannelBinderPort:[TCP] %d \r\nbinder stared\r\n", port);

    }

    public void setNewUDPDeviceChannel(NewUDPDeviceChannel newUDPDeviceChannel) {
        this.newUDPDeviceChannel = newUDPDeviceChannel;
    }


    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            byte[] buffer = new byte[255];
            while (runFlag) {
                try (Socket cam = serverSocket.accept()) {
                    cam.setSoTimeout(2000);
                    cam.setTcpNoDelay(true);
                    int len = cam.getInputStream().read(buffer);
                    // 需要 SN:{SN的长度}{,,,} sn长度至少为1 。例如SN为A 则 报文为 SN:1A
                    if (len < 5) {
                        cam.close();
                        continue;
                    }
                    if (!(ByteUtil.equals(HEAD, 0, buffer, 0, HEAD.length))) {
                        cam.close();
                        continue;
                    }
                    int snLen = buffer[3];
                    if (snLen + HEAD.length > len) {
                        cam.close();
                        continue;
                    }
                    byte[] snBytes = Arrays.copyOfRange(buffer, 4, 4 + buffer[3]);
                    String sn = ByteUtil.toHexString(snBytes);
                    Integer index = snChannelMap.get(sn);
                    if (index == null) {
                        index = channelIndexQueue.poll();
                        if (index == null) {
                            System.err.println("[warn] These channels have been exhausted. ");
                            cam.close();
                            continue;
                        }
                        snChannelMap.put(sn, index);
                    }

                    OutputStream outputStream = cam.getOutputStream();
                    outputStream.write(index);
                    outputStream.flush();
                    newUDPDeviceChannel.onNewChannelOpen(index, snBytes);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

            }
        } catch (IOException e) {
            System.out.println(" start server failed:" + e.getMessage());
        }

    }


    public void shutDown() {
        runFlag = false;
    }


}
