package org.btik.server.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamePrefixThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private String namePrefix;

    public NamePrefixThreadFactory(String namePrefix) {
        this.namePrefix = namePrefix;
    }


    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, namePrefix + '-' + threadNumber.incrementAndGet());
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }
}
