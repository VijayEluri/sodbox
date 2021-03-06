
package info.freelibrary.sodbox;

/**
 * Class for checking program invariants. Analog of C <code>assert()</code> macro. The Java compiler doesn't provide
 * information about compiled file and line number, so the place of assertion failure can be located only by analyzing
 * the stack trace of the thrown AssertionFailed exception.
 *
 * @see info.freelibrary.sodbox.AssertionFailed
 */
public class Assert {

    /**
     * Throw assertion failed exception.
     */
    public static final void failed() {
        throw new AssertionFailed();
    }

    /**
     * Throw assertion failed exception with given description.
     */
    public static final void failed(final String description) {
        throw new AssertionFailed(description);
    }

    /**
     * Check specified condition and raise <code>AssertionFailed</code> exception if it is not true.
     *
     * @param cond result of checked condition
     */
    public static final void that(final boolean cond) {
        if (!cond) {
            throw new AssertionFailed();
        }
    }

    /**
     * Check specified condition and raise <code>AssertionFailed</code> exception if it is not true.
     *
     * @param description string describing checked condition
     * @param cond result of checked condition
     */
    public static final void that(final String description, final boolean cond) {
        if (!cond) {
            throw new AssertionFailed(description);
        }
    }

}
