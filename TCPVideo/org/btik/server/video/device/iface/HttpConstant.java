package org.btik.server.video.device.iface;

import java.nio.charset.StandardCharsets;

public interface HttpConstant {

    byte HTTP_PATH_SEPARATOR = '/';

    byte[] uri = "GET /video".getBytes(StandardCharsets.UTF_8);
    int URI_LEN = uri.length;
    byte[] NOT_FOUND = ("""
            HTTP/1.1 404 \r
            Content-Type: text/html; charset=utf-8\r
            Content-Length: 12\r
            \r
            <h3>404</h3>""").getBytes(StandardCharsets.UTF_8);
    String PART_BOUNDARY = "123456789000000000000987654321";
    String STREAM_RESP_HEAD = "HTTP/1.1 200 OK\r\n" +
            "Content-Type: multipart/x-mixed-replace;boundary=" + PART_BOUNDARY + "\r\n" +
            "Transfer-Encoding: chunked\r\n" +
            "Access-Control-Allow-Origin: *\r\n";
    byte[] STREAM_RESP_HEAD_BYTES = STREAM_RESP_HEAD.getBytes(StandardCharsets.UTF_8);

    byte[] _STREAM_BOUNDARY = ("\r\n--" + PART_BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8);

    byte[] _STREAM_PART =
            "Content-Type: image/jpeg\r\nContent-Length: ".getBytes(StandardCharsets.UTF_8);

    byte[] NEW_LINE = new byte[]{'\r', '\n'};

    byte[] DOUBLE_LINE = new byte[]{'\r', '\n', '\r', '\n'};
}
