package org.btik.server.video.device.web;

import org.btik.server.util.ByteUtil;
import org.btik.server.video.device.iface.SoSendCheckTarget;
import org.btik.server.video.device.task.AsyncTaskExecutor;
import org.btik.server.video.device.iface.HttpConstant;
import org.btik.server.video.device.iface.VideoChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 视频频道
 * 在线的摄像头均有一个频道
 */
public class MJPEGVideoChannel implements VideoChannel, HttpConstant, SoSendCheckTarget {

    private final AsyncTaskExecutor asyncTaskExecutor;

    /**
     * 暂时没有用，debug时可以分辨属于哪个设备
     */
    private final String channelId;

    private volatile Socket currentSend;

    private volatile long sendStartTime;

    private final Set<Socket> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MJPEGVideoChannel(String channelId, AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        this.channelId = channelId;

    }

    @Override
    public void sendFrame(byte[] frame, int len) {
        byte[] lenStrBytes = ByteUtil.toString(len);
        byte[] lenHexStrBytes = ByteUtil.toHexString(len);

        for (Socket client : clients) {
            try {
                this.sendStartTime = System.currentTimeMillis();
                this.currentSend = client;
                OutputStream outputStream = client.getOutputStream();
                sendChunk(_STREAM_BOUNDARY, outputStream);
                sendChunk(outputStream, _STREAM_PART, lenStrBytes, DOUBLE_LINE);
                sendChunk(frame, len, lenHexStrBytes, outputStream);
                outputStream.flush();
            } catch (IOException e) {
                checkState(client, e);
            }finally {
                this.currentSend = null;
                this.sendStartTime = 0;
            }

        }

    }

    @Override
    public void sendFrame(byte[][] frame, int[] len, int segmentCount) {
        int allLen = 0;
        for (int i = 0; i < segmentCount; i++) {
            allLen += len[i];
        }
        byte[] lenStrBytes = ByteUtil.toString(allLen);

        for (Socket client : clients) {
            try {
                OutputStream outputStream = client.getOutputStream();
                sendChunk(_STREAM_BOUNDARY, outputStream);
                sendChunk(outputStream, _STREAM_PART, lenStrBytes, DOUBLE_LINE);
                sendChunk(outputStream, allLen, len, frame);
                outputStream.flush();
            } catch (IOException e) {
                checkState(client, e);
            }

        }

    }

    /**
     * 加入频道
     */
    public void joinChannel(Socket client) throws IOException {
        System.out.println("open:" + client.getRemoteSocketAddress() + " " + new Date());
        OutputStream outputStream = client.getOutputStream();
        outputStream.write(STREAM_RESP_HEAD_BYTES);
        outputStream.flush();
        client.setTcpNoDelay(true);
        clients.add(client);
    }

    private void checkState(Socket socket, Exception e) {
        disConnect(socket, e);
    }

    void sendChunk(byte[] chunk, OutputStream out) throws IOException {
        int length = chunk.length;
        out.write(NEW_LINE);
        out.write(ByteUtil.toHexString(length));
        out.write(NEW_LINE);
        out.write(chunk);

    }

    void sendChunk(byte[] chunkBuffer, final int len, byte[] lenHexStrBytes, OutputStream out) throws IOException {
        out.write(NEW_LINE);
        out.write(lenHexStrBytes);
        out.write(NEW_LINE);
        out.write(chunkBuffer, 0, len);

    }

    void sendChunk(OutputStream out, byte[]... chunk) throws IOException {
        int length = 0;
        for (byte[] bytes : chunk) {
            length += bytes.length;
        }

        out.write(NEW_LINE);
        out.write(ByteUtil.toHexString(length));
        out.write(NEW_LINE);
        for (byte[] bytes : chunk) {
            out.write(bytes);
        }

    }

    void sendChunk(OutputStream out, int allLen, final int[] length, byte[][] chunk) throws IOException {
        out.write(NEW_LINE);
        out.write(ByteUtil.toHexString(allLen));
        out.write(NEW_LINE);
        for (int i = 0; i < length.length; i++) {
            int len = length[i];
            if (len == 0) {
                break;
            }
            out.write(chunk[i], 0, len);
        }

    }

    void disConnect(Socket socket, Exception e) {
        asyncTaskExecutor.execute(() -> {
            System.err.println(e.getMessage());
            try {
                System.err.println("close:" + socket.getRemoteSocketAddress());
                socket.close();
                clients.remove(socket);
            } catch (IOException e0) {
                System.err.println(e.getMessage());
            }
        });

    }

    @Override
    public Socket currentSend() {
        return currentSend;
    }

    @Override
    public long sendStartTime() {
        return sendStartTime;
    }
}
