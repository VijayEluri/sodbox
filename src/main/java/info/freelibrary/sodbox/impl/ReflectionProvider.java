
package info.freelibrary.sodbox.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public interface ReflectionProvider {

    Constructor getDefaultConstructor(Class cls) throws Exception;

    void set(Field field, Object object, Object value) throws Exception;

    void setBoolean(Field field, Object object, boolean value) throws Exception;

    void setByte(Field field, Object object, byte value) throws Exception;

    void setChar(Field field, Object object, char value) throws Exception;

    void setDouble(Field field, Object object, double value) throws Exception;

    void setFloat(Field field, Object object, float value) throws Exception;

    void setInt(Field field, Object object, int value) throws Exception;

    void setLong(Field field, Object object, long value) throws Exception;

    void setShort(Field field, Object object, short value) throws Exception;

}
