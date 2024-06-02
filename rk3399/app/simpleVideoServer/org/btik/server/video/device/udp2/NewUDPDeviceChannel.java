package org.btik.server.video.device.udp2;

import org.btik.server.VideoServer;
import org.btik.server.util.ByteUtil;
import org.btik.server.util.NamePrefixThreadFactory;
import org.btik.server.video.device.iface.DevChannel;
import org.btik.server.video.device.iface.SoSendCheckTarget;
import org.btik.server.video.device.iface.VideoChannel;
import org.btik.server.video.device.web.SoSendTimeoutTicker;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.*;

public class NewUDPDeviceChannel extends Thread implements DevChannel {

    private static final int SN_LEN = 12;
    private volatile boolean runFlag = true;

    /**
     * 帧通道端口号
     */
    private int streamPort;

    private VideoServer videoServer;

    private ExecutorService executorService;

    private BufferPool bufferPool;


    private FrameDispatcher[] frameDispatchers;

    private SoSendTimeoutTicker soSendTimeoutTicker;

    /**
     * 帧分发线程数量，随着设备增多可以适当增加
     */
    private int dispatcherPoolSize = 8;

    private int videoChannelCount = 128;

    /**
     * 防止接收图片过快而，浏览器接收过慢，导致帧积压，当帧接收时间到转发时间超过以下延时之后将会被丢弃。
     */
    private long frameDelayTimeout = 2000;

    public void setStreamPort(int streamPort) {
        this.streamPort = streamPort;
    }

    public void setVideoServer(VideoServer videoServer) {
        this.videoServer = videoServer;
    }

    public void setBufferPool(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public void setVideoChannelCount(int videoChannelCount) {
        this.videoChannelCount = videoChannelCount;
    }

    public void setFrameDelayTimeout(long frameDelayTimeout) {
        this.frameDelayTimeout = frameDelayTimeout;
    }

    public void setSoSendTimeoutTicker(SoSendTimeoutTicker soSendTimeoutTicker) {
        this.soSendTimeoutTicker = soSendTimeoutTicker;
    }

    /**
     * 可选的输入值  1 2 4 8 16 32 64 128 256几个数字，根据cpu核数和设备的数量选择合适的值
     * ，输入其它值也会被映射到以上值，如果只有一个摄像头设备那就一个足够，线程数太多而cpu核数过少，
     * 反而因为线程不断切换使得效率更低
     */
    public void setDispatcherPoolSize(int dispatcherPoolSize) {
        int maximumCapacity = 256;
        int n = -1 >>> Integer.numberOfLeadingZeros(dispatcherPoolSize - 1);
        this.dispatcherPoolSize = (n < 0) ? 1 : (n >= maximumCapacity) ? maximumCapacity : n + 1;
    }


    private VideoChannel[] videoChannelTable;


    @Override
    public synchronized void start() {
        System.out.println("init buffer pool");

        System.out.println("start dispatchers");
        videoChannelTable = new VideoChannel[videoChannelCount];
        frameDispatchers = new FrameDispatcher[dispatcherPoolSize];
        executorService = new ThreadPoolExecutor(dispatcherPoolSize, dispatcherPoolSize,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamePrefixThreadFactory("frameDispatcher"));
        for (int i = 0; i < dispatcherPoolSize; i++) {
            FrameDispatcher msgDispatcher = new FrameDispatcher();
            frameDispatchers[i] = msgDispatcher;
            executorService.submit(msgDispatcher);
        }
        System.out.printf("""
                        dispatcherPoolSize: %d \r
                        frameDelayTimeout: %d\r
                        streamPort:[UDP] %d\r
                        bufferPoolSize:%d\r
                        """,
                dispatcherPoolSize, frameDelayTimeout, streamPort, bufferPool.getBufferPoolSize());

        System.out.println("udp channel loaded");
        super.start();
    }

    @Override
    public void run() {

        try (DatagramSocket serverSocket = new DatagramSocket(streamPort)) {
            FrameBuffer frameBuffer = bufferPool.getFrameBuffer();
            DatagramPacket datagramPacket = new DatagramPacket(frameBuffer.data, 0, frameBuffer.data.length);
            while (runFlag) {
                serverSocket.receive(datagramPacket);
                // 最后一位是通道索引，故长度 -1 才是照片数据
                frameBuffer.size = datagramPacket.getLength() - 1;
                frameBuffer.channelIndex = frameBuffer.data[frameBuffer.size] & 0xff;
                frameBuffer.time = System.currentTimeMillis();
                frameDispatchers[frameBuffer.channelIndex & dispatcherPoolSize - 1].messages.add(frameBuffer);
                // 切换缓冲区
                frameBuffer = bufferPool.getFrameBuffer();
                datagramPacket.setData(frameBuffer.data);
            }
        } catch (IOException e) {
            System.out.println(" start server failed:" + e.getMessage());
        }
    }

    public void shutDown() {
        runFlag = false;
        // 无消息导致阻塞时，没有读到flag,帮助退出阻塞
        for (FrameDispatcher frameDispatcher : frameDispatchers) {
            frameDispatcher.messages.add(new FrameBuffer(new byte[0]));
        }
        // 线程池核心线程也需要停止
        executorService.shutdown();
    }


    @Override
    public int channelIdLen() {
        return SN_LEN;
    }


    class FrameDispatcher implements Runnable {
        LinkedBlockingQueue<FrameBuffer> messages = new LinkedBlockingQueue<>();

        @Override
        public void run() {

            try {
                while (runFlag) {
                    FrameBuffer segment = messages.take();
                    try {
                        int channelIndex = segment.channelIndex;
                        if (channelIndex < 0 || channelIndex > videoChannelCount) {
                            System.out.print("\rThe channel index is illegal ");
                            System.out.print(channelIndex);
                            continue;
                        }
                        VideoChannel videoChannel = videoChannelTable[channelIndex];
                        if (videoChannel == null) {
                            System.out.print("\rvideoChannel index is not registered and may need to be restarted the ESP ,frame size: ");
                            System.out.print(segment.size);
                            continue;
                        }
                        // 延时跳帧
                        if (System.currentTimeMillis() > segment.time + frameDelayTimeout) {
                            System.out.print("\rframe time out continue, size : ");
                            System.out.print(segment.size);
                            continue;
                        }
                        videoChannel.sendFrame(segment.data, segment.size);
                    } catch (Exception e) {
                        if (runFlag) {
                            e.printStackTrace();
                        } else {
                            break;
                        }
                    } finally {
                        // 归还到池里
                        bufferPool.returnBuffer(segment);
                    }

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("exit by:" + e);
            }
            System.out.println("exit : " + getName());
        }


    }

    public void onNewChannelOpen(int channelIndex, byte[] sn) {
        byte[] snHexVal = ByteUtil.toHexBytes(sn);
        byte[] snAddr = new byte[snHexVal.length + 1];
        System.arraycopy(snHexVal, 0, snAddr, 1, SN_LEN);
        VideoChannel channel = videoServer.createChannel(snAddr);
        videoChannelTable[channelIndex] = channel;
        if(channel instanceof SoSendCheckTarget){
            soSendTimeoutTicker.registerChannel(channelIndex, (SoSendCheckTarget) channel);
        }
    }

}
