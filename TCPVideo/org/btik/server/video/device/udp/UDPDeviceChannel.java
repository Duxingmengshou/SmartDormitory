package org.btik.server.video.device.udp;

import org.btik.server.video.device.iface.DevChannel;
import org.btik.server.VideoServer;
import org.btik.server.util.ByteUtil;
import org.btik.server.util.NamePrefixThreadFactory;
import org.btik.server.video.device.task.AsyncTaskExecutor;
import org.btik.server.video.device.iface.VideoChannel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.*;


/**
 * 发送帧设备接入通道
 */
public class UDPDeviceChannel extends Thread implements DevChannel {
    private static final int SN_LEN = 16;
    private volatile boolean runFlag = true;

    /**
     * 帧通道端口号
     */
    private int streamPort;

    private VideoServer videoServer;

    private AsyncTaskExecutor asyncTaskExecutor;

    private ExecutorService executorService;

    private BufferPool bufferPool;


    private FrameDispatcher[] frameDispatchers;

    /**
     * 帧分发线程数量，随着设备增多可以适当增加
     */
    private int dispatcherPoolSize = 8;

    public void setStreamPort(int streamPort) {
        this.streamPort = streamPort;
    }

    public void setVideoServer(VideoServer videoServer) {
        this.videoServer = videoServer;
    }

    public void setAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
    }

    public void setBufferPool(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
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


    private final ConcurrentHashMap<Long, UDPDev> devMap = new ConcurrentHashMap<>();


    @Override
    public synchronized void start() {
        System.out.println("init buffer pool");


        System.out.println("start dispatchers");
        frameDispatchers = new FrameDispatcher[dispatcherPoolSize];
        executorService = new ThreadPoolExecutor(dispatcherPoolSize, dispatcherPoolSize,
                0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new NamePrefixThreadFactory("frameDispatcher"));
        for (int i = 0; i < dispatcherPoolSize; i++) {
            FrameDispatcher msgDispatcher = new FrameDispatcher();
            frameDispatchers[i] = msgDispatcher;
            executorService.submit(msgDispatcher);
        }

        System.out.println("udp channel loaded");
        super.start();
    }

    @Override
    public void run() {

        try (DatagramSocket serverSocket = new DatagramSocket(streamPort)) {
            FrameSegmentBuffer frameSegmentBuffer = bufferPool.getFrameBuffer();
            DatagramPacket datagramPacket = new DatagramPacket(frameSegmentBuffer.data, 0, frameSegmentBuffer.data.length);
            while (runFlag) {
                serverSocket.receive(datagramPacket);
                InetAddress address = datagramPacket.getAddress();
                frameSegmentBuffer.address = (long) address.hashCode() << 16 | datagramPacket.getPort();
                frameSegmentBuffer.size = datagramPacket.getLength();
                frameDispatchers[(int) (frameSegmentBuffer.address & dispatcherPoolSize - 1)].messages.add(frameSegmentBuffer);
                // 切换缓冲区
                frameSegmentBuffer = bufferPool.getFrameBuffer();
                datagramPacket.setData(frameSegmentBuffer.data);
            }
        } catch (IOException e) {
            System.out.println(" start server failed:" + e.getMessage());
        }
    }

    public void shutDown() {
        runFlag = false;
        // 无消息导致阻塞时，没有读到flag,帮助退出阻塞
        for (FrameDispatcher frameDispatcher : frameDispatchers) {
            frameDispatcher.messages.add(new FrameSegmentBuffer(new byte[0]));
        }
        // 线程池核心线程也需要停止
        executorService.shutdown();
    }


    @Override
    public int channelIdLen() {
        return SN_LEN;
    }


    static class UDPDev {
        byte[][] frame;

        int[] sizeTable;
        VideoChannel videoChannel;

        BufferPool bufferPool;
        long address;

        int segmentIndex = 0;

        public UDPDev(byte[][] frame, VideoChannel videoChannel, BufferPool bufferPool, long address) {
            this.frame = frame;
            this.sizeTable = new int[frame.length];
            this.videoChannel = videoChannel;
            this.address = address;
            this.bufferPool = bufferPool;
        }

        public void appendSegment(FrameSegmentBuffer frameSegmentBuffer) {

            int size = frameSegmentBuffer.size;
            // 判断结束标识
            if (size == 0) {
                videoChannel.sendFrame(frame, sizeTable, segmentIndex + 1);
                free();
            } else {
                try {
                    sizeTable[segmentIndex] = size;
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    // 将指针复位，防止累积后持续越界
                    free();
                    return;
                }

                byte[] data = frameSegmentBuffer.data;
                frame[segmentIndex++] = data;
                // 被帧空间指向后,为帧片段重新分配内存
                frameSegmentBuffer.data = bufferPool.getBuffer();
            }

        }

        void free() {
            for (int i = 0; i < segmentIndex; i++) {
                bufferPool.returnBufferData(frame[i]);
                frame[i] = null;
                sizeTable[i] = 0;
            }
            segmentIndex = 0;
        }
    }

    class FrameDispatcher implements Runnable {
        LinkedBlockingQueue<FrameSegmentBuffer> messages = new LinkedBlockingQueue<>();

        @Override
        public void run() {

            try {
                while (runFlag) {
                    long l = System.currentTimeMillis();
                    FrameSegmentBuffer segment = messages.take();
                    try {
                        long address = segment.address;
                        UDPDev dev = devMap.get(address);
                        if (dev != null) {
                            dev.appendSegment(segment);
                        } else {
                            onNewStreamOpen(segment);
                        }
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

    private void onNewStreamOpen(FrameSegmentBuffer frame) {
        byte[] sn = new byte[SN_LEN + 1];
        System.arraycopy(ByteUtil.toHexString(frame.address), 0, sn, 1, SN_LEN);
        VideoChannel channel = videoServer.createChannel(sn);
        devMap.put(frame.address, new UDPDev(new byte[200][], channel, bufferPool, frame.address));
    }

}
