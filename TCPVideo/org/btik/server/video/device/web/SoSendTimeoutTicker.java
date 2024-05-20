package org.btik.server.video.device.web;


import org.btik.server.video.device.iface.SoSendCheckTarget;

import java.io.IOException;
import java.net.Socket;

/**
 * 暂时没有找到java设置tcp socket 的 so_sndtimeo的方法这里使用一个定时器来代替
 *
 * @author lustre
 * @since 2023/3/25 16:44
 */
public class SoSendTimeoutTicker extends Thread {


    boolean runFlag = true;

    private final byte[] lock = new byte[0];

    private int videoChannelCount = 128;

    private int timeout = 300;


    private final SoSendCheckTarget[] checkTargets;

    public void setVideoChannelCount(int videoChannelCount) {
        this.videoChannelCount = Math.max(videoChannelCount, 1);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public SoSendTimeoutTicker() {
        super("SoSendTimeoutTicker");
        checkTargets = new SoSendCheckTarget[videoChannelCount];
    }

    @Override
    public void run() {
        while (runFlag) {
            synchronized (lock) {
                try {
                    for (SoSendCheckTarget checkTarget : checkTargets) {
                        if (checkTarget == null) {
                            continue;
                        }
                        Socket socket = checkTarget.currentSend();
                        if (socket == null) {
                            continue;
                        }
                        long now = System.currentTimeMillis();
                        long startTime = checkTarget.sendStartTime();
                        if (startTime == 0) {
                            continue;
                        }
                        if (now > timeout + startTime) {
                            try {
                                socket.shutdownOutput();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                    lock.wait(100);
                } catch (InterruptedException ignored) {
                    if (!runFlag) {
                        return;
                    }
                }
            }
        }
    }


    public void shutdown(String msg) {
        runFlag = false;
    }

    public void registerChannel(int channelIndex, SoSendCheckTarget soSendCheckTarget) {
        checkTargets[channelIndex] = soSendCheckTarget;
    }
}
