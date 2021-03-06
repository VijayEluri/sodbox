
package info.freelibrary.sodbox;

/**
 * Interface of file. Programmer can provide its own implementation of this interface, adding such features as support
 * of flash cards, encrypted files.... Implementation of this interface should throw StorageError exception in case of
 * failure.
 */
public interface IFile {

    /**
     * Close file.
     */
    void close();

    /**
     * Length of the file.
     */
    long length();

    /**
     * Lock file.
     *
     * @param shared if lock is shared
     */
    void lock(boolean shared);

    /**
     * Read data from the file.
     *
     * @param pos offset in the file
     * @param buf array to receive read data (size is always equal to database page size)
     * @return number of bytes actually read
     */
    int read(long pos, byte[] buf);

    /**
     * Flush all fields changes to the disk.
     */
    void sync();

    /**
     * Try lock file.
     *
     * @param shared if lock is shared
     * @return <code>true</code> if file was successfully locked or locking in not implemented, <code>false</code> if
     *         file is locked by some other application
     */
    boolean tryLock(boolean shared);

    /**
     * Unlock file.
     */
    void unlock();

    /**
     * Write data to the file.
     *
     * @param pos offset in the file
     * @param buf array with data to be writer (size is always equal to database page size)
     */
    void write(long pos, byte[] buf);

}
