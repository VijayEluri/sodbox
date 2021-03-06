
package info.freelibrary.sodbox.fulltext;

/**
 * Occurrence of word in the document
 */
public class Occurrence implements Comparable {

    /**
     * Word (lowercased)
     */
    public String word;

    /**
     * Position of word in document text (0 based)
     */
    public int position;

    /**
     * Word occurrence kind. It is up to the document scanner implementation how to enumerate occurence kinds. These
     * is only one limitation - number of difference kinds should not exceed 8.
     */
    public int kind;

    /**
     * Occurrence constructor
     * 
     * @param word lowercased word
     * @param position offset of word from the beginning of document text
     * @param kind word occurrence kind (should be less than 8)
     */
    public Occurrence(final String word, final int position, final int kind) {
        this.word = word;
        this.position = position;
        this.kind = kind;
    }

    @Override
    public int compareTo(final Object o) {
        final Occurrence occ = (Occurrence) o;
        int diff = word.compareTo(occ.word);
        if (diff == 0) {
            diff = position - occ.position;
        }
        return diff;
    }
}