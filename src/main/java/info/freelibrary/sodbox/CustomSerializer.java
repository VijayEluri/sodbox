
package info.freelibrary.sodbox;

import java.io.IOException;

/**
 * Interface of custom serializer
 */
public interface CustomSerializer {

    /**
     * Create instance of specified class
     *
     * @param cls created object class
     */
    Object create(Class cls);

    /**
     * Check if serializer can pack objects of this class
     *
     * @param cls inspected object class
     * @return true if serializer can pack instances of this class
     */
    boolean isApplicable(Class cls);

    /**
     * Check if serializer can pack this object component
     *
     * @param obj object component to be packed
     * @return true if serializer can pack this object inside some other object
     */
    boolean isEmbedded(Object obj);

    /**
     * Serialize object
     *
     * @param obj object to be packed
     * @param out output stream to which object should be serialized
     */
    void pack(Object obj, SodboxOutputStream out) throws IOException;

    /**
     * Create object from its string representation
     *
     * @param str string representation of object (created by toString() method)
     */
    Object parse(String str) throws Exception;

    /**
     * Get string representation of the object
     *
     * @param str object which string representation is taken
     */
    String print(Object str);

    /**
     * Deserialize object
     *
     * @param obj unpacked object
     * @param in input stream from which object should be deserialized
     */
    void unpack(Object obj, SodboxInputStream in) throws IOException;

    /**
     * Deserialize object
     *
     * @param in input stream from which object should be deserialized
     * @return created and unpacked object
     */
    Object unpack(SodboxInputStream in) throws IOException;

}
