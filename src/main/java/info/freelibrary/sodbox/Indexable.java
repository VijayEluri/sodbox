
package info.freelibrary.sodbox;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking indexable fields used by Database class to create table descriptors. Indices can be unique
 * or allow duplicates. If index is marked as unique and during transaction commit it is find out that there is
 * already some other object with this key, NotUniqueException will be thrown. Case insensitive attribute is
 * meaningful only for string keys and if set cause ignoring case of key values. Thick index should be used for keys
 * with small set of unique values.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Indexable {

    /**
     * String index is case insensitive.
     */
    boolean caseInsensitive() default false;

    /**
     * Index supports fast access to elements by position.
     */
    boolean randomAccess() default false;

    /**
     * Index is optimized to handle large number of duplicate key values.
     */
    boolean thick() default false;

    /**
     * Index may not contain duplicates.
     */
    boolean unique() default false;

}
