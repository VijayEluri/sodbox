
package info.freelibrary.sodbox.impl;

public class Page extends LRU implements Comparable {

    static final int psDirty = 0x01;// page has been modified

    static final int psRaw = 0x02;// page is loaded from the disk

    static final int psWait = 0x04;// other thread(s) wait load operation completion

    public static final int pageSizeLog = 12;

    public static final int pageSize = 1 << pageSizeLog;

    Page collisionChain;

    int accessCount;

    int writeQueueIndex;

    int state;

    long offs;

    byte data[];

    @Override
    public int compareTo(final Object o) {
        final long po = ((Page) o).offs;
        return offs < po ? -1 : offs == po ? 0 : 1;
    }
}
