package org.btik.server.video.device.iface;

import java.net.Socket;

/**
 * @author lustre
 * @since 2023/3/25 16:51
 */
public interface SoSendCheckTarget {
    Socket currentSend();

    long sendStartTime();
}
