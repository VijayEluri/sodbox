
package info.freelibrary.sodbox;

/**
 * Exception throw by storage implementation.
 */
public class StorageError extends RuntimeException {

    public static final int STORAGE_NOT_OPENED = 1;

    public static final int STORAGE_ALREADY_OPENED = 2;

    public static final int FILE_ACCESS_ERROR = 3;

    public static final int KEY_NOT_UNIQUE = 4;

    public static final int KEY_NOT_FOUND = 5;

    public static final int SCHEMA_CHANGED = 6;

    public static final int UNSUPPORTED_TYPE = 7;

    public static final int UNSUPPORTED_INDEX_TYPE = 8;

    public static final int INCOMPATIBLE_KEY_TYPE = 9;

    public static final int NOT_ENOUGH_SPACE = 10;

    public static final int DATABASE_CORRUPTED = 11;

    public static final int CONSTRUCTOR_FAILURE = 12;

    public static final int DESCRIPTOR_FAILURE = 13;

    public static final int ACCESS_TO_STUB = 14;

    public static final int INVALID_OID = 15;

    public static final int DELETED_OBJECT = 16;

    public static final int ACCESS_VIOLATION = 17;

    public static final int CLASS_NOT_FOUND = 18;

    public static final int NULL_VALUE = 19;

    public static final int INDEXED_FIELD_NOT_FOUND = 20;

    public static final int LOCK_FAILED = 21;

    public static final int NO_SUCH_PROPERTY = 22;

    public static final int BAD_PROPERTY_VALUE = 23;

    public static final int SERIALIZE_PERSISTENT = 24;

    public static final int EMPTY_VALUE = 25;

    public static final int UNSUPPORTED_ENCODING = 26;

    public static final int STORAGE_IS_USED = 27;

    public static final int BAD_REPLICATION_PORT = 28;

    public static final int CONNECTION_FAILURE = 29;

    public static final int TOO_MUCH_OBJECTS = 30;

    public static final int FULL_TEXT_INDEX_ERROR = 31;

    public static final int KEY_IS_NULL = 32;

    public static final int INVALID_OPERATION = 33;

    public static final int READ_ONLY_DATABASE = 34;

    public static final int NOT_IN_TRANSACTION = 35;

    public static final int WRONG_CIPHER_KEY = 36;

    public static final int STORAGE_CANNOT_BE_CREATED = 37;

    private static final long serialVersionUID = -1077039779052096595L;

    private static final String[] MESSAGE_TEXT = { "", "Storage not opened", "Storage already opened",
        "File access error", "Key not unique", "Key not found", "Database schema was changed for", "Unsupported type",
        "Unsupported index type", "Incompatible key type", "Not enough space", "Database file is corrupted",
        "Failed to instantiate the object of", "Failed to build descriptor for", "Stub object is accessed",
        "Invalid object reference", "Access to the deleted object", "Object access violation", "Failed to locate",
        "Null value", "Could not find indexed field", "Lock could not be granted", "No such database property",
        "Bad property value", "Attempt to store persistent object as raw object",
        "Attempt to store java.lang.Object as value", "Unsupported encoding", "Storage is used by other application",
        "Invalid replication node port", "Connection failure", "Too much objects",
        "Failed to insert document in full text index", "Index key is null", "Invalid operation",
        "Database is opened in read-only mode", "Not within serializable transaction context",
        "Wrong cipher key for the encrypted database", "Storage cannot be created" };

    private final int myErrorCode;

    private Exception myOrigEx;

    /**
     * Creates a storage error with the supplied error code.
     *
     * @param aErrorCode An error code
     */
    public StorageError(final int aErrorCode) {
        super(MESSAGE_TEXT[aErrorCode]);
        myErrorCode = aErrorCode;
    }

    /**
     * Creates a storage error with the supplied error code and parent exception.
     *
     * @param aErrorCode An error code
     * @param aException A parent exception
     */
    public StorageError(final int aErrorCode, final Exception aException) {
        super(MESSAGE_TEXT[aErrorCode] + ": " + aException, aException);

        myErrorCode = aErrorCode;
        myOrigEx = aException;
    }

    /**
     * Creates a storage error with the supplied error code and additional details.
     *
     * @param aErrorCode An error code
     * @param aParam Additional details
     */
    public StorageError(final int aErrorCode, final Object aParam) {
        super(MESSAGE_TEXT[aErrorCode] + " " + aParam);

        myErrorCode = aErrorCode;
    }

    /**
     * Creates a storage error with the supplied error code, additional details, and parent exception.
     *
     * @param aErrorCode An error code
     * @param aParam Additional details
     * @param aException A parent exception
     */
    public StorageError(final int aErrorCode, final Object aParam, final Exception aException) {
        super(MESSAGE_TEXT[aErrorCode] + " " + aParam + ": " + aException, aException);

        myErrorCode = aErrorCode;
        myOrigEx = aException;
    }

    /**
     * Get exception error code (see definitions above).
     */
    public int getErrorCode() {
        return myErrorCode;
    }

    /**
     * Get original exception if StorageError exception was thrown as the result of catching some other exception within
     * Storage implementation. StorageError is used as wrapper of other exceptions to avoid cascade propagation of
     * throws and try/catch constructions.
     *
     * @return original exception or <code>null</code> if there is no such exception
     */
    public Exception getOriginalException() {
        return myOrigEx;
    }

}
