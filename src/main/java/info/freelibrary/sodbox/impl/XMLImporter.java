
package info.freelibrary.sodbox.impl;

import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import info.freelibrary.sodbox.Key;
import info.freelibrary.sodbox.XMLImportException;
import info.freelibrary.sodbox.impl.ClassDescriptor.FieldDescriptor;

public class XMLImporter {

    static class XMLElement {

        static final int NO_VALUE = 0;

        static final int STRING_VALUE = 1;

        static final int INT_VALUE = 2;

        static final int REAL_VALUE = 3;

        static final int NULL_VALUE = 4;

        final static Collection<XMLElement> EMPTY_COLLECTION = new ArrayList<XMLElement>();

        private XMLElement next;

        private XMLElement prev;

        private final String name;

        private HashMap<String, XMLElement> siblings;

        private HashMap<String, String> attributes;

        private String svalue;

        private long ivalue;

        private double rvalue;

        private int valueType;

        private int counter;

        XMLElement(final String name) {
            this.name = name;
            valueType = NO_VALUE;
        }

        final void addAttribute(final String name, final String value) {
            if (attributes == null) {
                attributes = new HashMap();
            }
            attributes.put(name, value);
        }

        final void addSibling(final XMLElement elem) {
            if (siblings == null) {
                siblings = new HashMap<String, XMLElement>();
            }
            final XMLElement prev = siblings.get(elem.name);
            if (prev != null) {
                elem.next = null;
                elem.prev = prev.prev;
                elem.prev.next = elem;
                prev.prev = elem;
                prev.counter += 1;
            } else {
                siblings.put(elem.name, elem);
                elem.prev = elem;
                elem.counter = 1;
            }
        }

        final String getAttribute(final String name) {
            return attributes != null ? attributes.get(name) : null;
        }

        final int getCounter() {
            return counter;
        }

        final XMLElement getFirstSibling() {
            for (final XMLElement e : getSiblings()) {
                return e;
            }
            return null;
        }

        final long getIntValue() {
            return ivalue;
        }

        final String getName() {
            return name;
        }

        final XMLElement getNextSibling() {
            return next;
        }

        final double getRealValue() {
            return rvalue;
        }

        final XMLElement getSibling(final String name) {
            if (siblings != null) {
                return siblings.get(name);
            }
            return null;
        }

        final Collection<XMLElement> getSiblings() {
            return siblings != null ? siblings.values() : EMPTY_COLLECTION;
        }

        final String getStringValue() {
            return svalue;
        }

        final boolean isIntValue() {
            return valueType == INT_VALUE;
        }

        final boolean isNullValue() {
            return valueType == NULL_VALUE;
        }

        final boolean isRealValue() {
            return valueType == REAL_VALUE;
        }

        final boolean isStringValue() {
            return valueType == STRING_VALUE;
        }

        final void setIntValue(final long val) {
            ivalue = val;
            valueType = INT_VALUE;
        }

        final void setNullValue() {
            valueType = NULL_VALUE;
        }

        final void setRealValue(final double val) {
            rvalue = val;
            valueType = REAL_VALUE;
        }

        final void setStringValue(final String val) {
            svalue = val;
            valueType = STRING_VALUE;
        }
    }

    static class XMLScanner {

        static final int XML_IDENT = 0;

        static final int XML_SCONST = 1;

        static final int XML_ICONST = 2;

        static final int XML_FCONST = 3;

        static final int XML_LT = 4;

        static final int XML_GT = 5;

        static final int XML_LTS = 6;

        static final int XML_GTS = 7;

        static final int XML_EQ = 8;

        static final int XML_EOF = 9;

        Reader reader;

        int line;

        int column;

        char[] sconst;

        long iconst;

        double fconst;

        int slen;

        String ident;

        int size;

        int ungetChar;

        boolean hasUngetChar;

        XMLScanner(final Reader in) {
            reader = in;
            sconst = new char[size = 1024];
            line = 1;
            column = 0;
            hasUngetChar = false;
        }

        final int get() throws XMLImportException {
            if (hasUngetChar) {
                hasUngetChar = false;
                return ungetChar;
            }
            try {
                final int ch = reader.read();
                if (ch == '\n') {
                    line += 1;
                    column = 0;
                } else if (ch == '\t') {
                    column += column + 8 & ~7;
                } else {
                    column += 1;
                }
                return ch;
            } catch (final IOException x) {
                throw new XMLImportException(line, column, x.getMessage());
            }
        }

        final int getColumn() {
            return column;
        }

        final String getIdentifier() {
            return ident;
        }

        final long getInt() {
            return iconst;
        }

        final int getLine() {
            return line;
        }

        final double getReal() {
            return fconst;
        }

        final String getString() {
            return new String(sconst, 0, slen);
        }

        final int scan() throws XMLImportException {
            int i, ch, quote;
            boolean floatingPoint;

            while (true) {
                do {
                    if ((ch = get()) < 0) {
                        return XML_EOF;
                    }
                } while (ch <= ' ');

                switch (ch) {
                    case '<':
                        ch = get();
                        if (ch == '?') {
                            while ((ch = get()) != '?') {
                                if (ch < 0) {
                                    throw new XMLImportException(line, column, "Bad XML file format");
                                }
                            }
                            if ((ch = get()) != '>') {
                                throw new XMLImportException(line, column, "Bad XML file format");
                            }
                            continue;
                        }
                        if (ch != '/') {
                            unget(ch);
                            return XML_LT;
                        }
                        return XML_LTS;
                    case '>':
                        return XML_GT;
                    case '/':
                        ch = get();
                        if (ch != '>') {
                            unget(ch);
                            throw new XMLImportException(line, column, "Bad XML file format");
                        }
                        return XML_GTS;
                    case '=':
                        return XML_EQ;
                    case '"':
                    case '\'':
                        quote = ch;
                        i = 0;
                        while (true) {
                            ch = get();
                            if (ch < 0) {
                                throw new XMLImportException(line, column, "Bad XML file format");
                            } else if (ch == '&') {
                                switch (get()) {
                                    case 'a':
                                        ch = get();
                                        if (ch == 'm') {
                                            if (get() == 'p' && get() == ';') {
                                                ch = '&';
                                                break;
                                            }
                                        } else if (ch == 'p' && get() == 'o' && get() == 's' && get() == ';') {
                                            ch = '\'';
                                            break;
                                        }
                                        throw new XMLImportException(line, column, "Bad XML file format");
                                    case 'l':
                                        if (get() != 't' || get() != ';') {
                                            throw new XMLImportException(line, column, "Bad XML file format");
                                        }
                                        ch = '<';
                                        break;
                                    case 'g':
                                        if (get() != 't' || get() != ';') {
                                            throw new XMLImportException(line, column, "Bad XML file format");
                                        }
                                        ch = '>';
                                        break;
                                    case 'q':
                                        if (get() != 'u' || get() != 'o' || get() != 't' || get() != ';') {
                                            throw new XMLImportException(line, column, "Bad XML file format");
                                        }
                                        ch = '"';
                                        break;
                                    default:
                                        throw new XMLImportException(line, column, "Bad XML file format");
                                }
                            } else if (ch == quote) {
                                slen = i;
                                return XML_SCONST;
                            }
                            if (i == size) {
                                final char[] newBuf = new char[size *= 2];
                                System.arraycopy(sconst, 0, newBuf, 0, i);
                                sconst = newBuf;
                            }
                            sconst[i++] = (char) ch;
                        }
                    case '-':
                    case '+':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        i = 0;
                        floatingPoint = false;
                        while (true) {
                            if (!Character.isDigit((char) ch) && ch != '-' && ch != '+' && ch != '.' && ch != 'E') {
                                unget(ch);
                                try {
                                    if (floatingPoint) {
                                        fconst = Double.parseDouble(new String(sconst, 0, i));
                                        return XML_FCONST;
                                    } else {
                                        iconst = Long.parseLong(new String(sconst, 0, i));
                                        return XML_ICONST;
                                    }
                                } catch (final NumberFormatException x) {
                                    throw new XMLImportException(line, column, "Bad XML file format");
                                }
                            }
                            if (i == size) {
                                throw new XMLImportException(line, column, "Bad XML file format");
                            }
                            sconst[i++] = (char) ch;
                            if (ch == '.') {
                                floatingPoint = true;
                            }
                            ch = get();
                        }
                    default:
                        i = 0;
                        while (Character.isLetterOrDigit((char) ch) || ch == '-' || ch == ':' || ch == '_' ||
                                ch == '.') {
                            if (i == size) {
                                throw new XMLImportException(line, column, "Bad XML file format");
                            }
                            if (ch == '-') {
                                ch = '$';
                            }
                            sconst[i++] = (char) ch;
                            ch = get();
                        }
                        unget(ch);
                        if (i == 0) {
                            throw new XMLImportException(line, column, "Bad XML file format");
                        }
                        ident = new String(sconst, 0, i);
                        return XML_IDENT;
                }
            }
        }

        final void unget(final int ch) {
            if (ch == '\n') {
                line -= 1;
            } else {
                column -= 1;
            }
            ungetChar = ch;
            hasUngetChar = true;
        }
    }

    static final String dateFormat = "EEE, d MMM yyyy kk:mm:ss z";

    static final DateFormat httpFormatter = new SimpleDateFormat(dateFormat, Locale.ENGLISH);

    StorageImpl storage;

    XMLScanner scanner;

    int[] idMap;

    public XMLImporter(final StorageImpl storage, final Reader reader) {
        this.storage = storage;
        scanner = new XMLScanner(reader);
    }

    final Key createCompoundKey(final int[] types, final String[] values) throws XMLImportException {
        final ByteBuffer buf = new ByteBuffer();
        int dst = 0;

        try {
            for (int i = 0; i < types.length; i++) {
                final String value = values[i];
                switch (types[i]) {
                    case ClassDescriptor.tpBoolean:
                        buf.extend(dst + 1);
                        buf.arr[dst++] = (byte) (Integer.parseInt(value) != 0 ? 1 : 0);
                        break;
                    case ClassDescriptor.tpByte:
                        buf.extend(dst + 1);
                        buf.arr[dst++] = Byte.parseByte(value);
                        break;
                    case ClassDescriptor.tpChar:
                        buf.extend(dst + 2);
                        Bytes.pack2(buf.arr, dst, (short) Integer.parseInt(value));
                        dst += 2;
                        break;
                    case ClassDescriptor.tpShort:
                        buf.extend(dst + 2);
                        Bytes.pack2(buf.arr, dst, Short.parseShort(value));
                        dst += 2;
                        break;
                    case ClassDescriptor.tpInt:
                    case ClassDescriptor.tpEnum:
                        buf.extend(dst + 4);
                        Bytes.pack4(buf.arr, dst, Integer.parseInt(value));
                        dst += 4;
                        break;
                    case ClassDescriptor.tpObject:
                        buf.extend(dst + 4);
                        Bytes.pack4(buf.arr, dst, mapId(Integer.parseInt(value)));
                        dst += 4;
                        break;
                    case ClassDescriptor.tpLong:
                    case ClassDescriptor.tpDate:
                        buf.extend(dst + 8);
                        Bytes.pack8(buf.arr, dst, Long.parseLong(value));
                        dst += 8;
                        break;
                    case ClassDescriptor.tpFloat:
                        buf.extend(dst + 4);
                        Bytes.pack4(buf.arr, dst, Float.floatToIntBits(Float.parseFloat(value)));
                        dst += 4;
                        break;
                    case ClassDescriptor.tpDouble:
                        buf.extend(dst + 8);
                        Bytes.pack8(buf.arr, dst, Double.doubleToLongBits(Double.parseDouble(value)));
                        dst += 8;
                        break;
                    case ClassDescriptor.tpString:
                    case ClassDescriptor.tpClass:
                        dst = buf.packString(dst, value);
                        break;
                    case ClassDescriptor.tpArrayOfByte:
                        buf.extend(dst + 4 + (value.length() >>> 1));
                        Bytes.pack4(buf.arr, dst, value.length() >>> 1);
                        dst += 4;
                        for (int j = 0, n = value.length(); j < n; j += 2) {
                            buf.arr[dst++] = (byte) (getHexValue(value.charAt(j)) << 4 | getHexValue(value.charAt(j +
                                    1)));
                        }
                        break;
                    default:
                        throwException("Bad key type");
                }
            }
        } catch (final NumberFormatException x) {
            throwException("Failed to convert key value");
        }
        return new Key(buf.toArray());
    }

    final void createIndex(final String indexType) throws XMLImportException {
        Btree btree = null;
        int tkn;
        int oid = 0;
        boolean unique = false;
        String className = null;
        String fieldName = null;
        String[] fieldNames = null;
        int[] types = null;
        long autoinc = 0;
        String type = null;
        while ((tkn = scanner.scan()) == XMLScanner.XML_IDENT) {
            final String attrName = scanner.getIdentifier();
            if (scanner.scan() != XMLScanner.XML_EQ || scanner.scan() != XMLScanner.XML_SCONST) {
                throwException("Attribute value expected");
            }
            final String attrValue = scanner.getString();
            if (attrName.equals("id")) {
                oid = mapId(parseInt(attrValue));
            } else if (attrName.equals("unique")) {
                unique = parseInt(attrValue) != 0;
            } else if (attrName.equals("class")) {
                className = attrValue;
            } else if (attrName.equals("type")) {
                type = attrValue;
            } else if (attrName.equals("autoinc")) {
                autoinc = parseInt(attrValue);
            } else if (attrName.equals("field")) {
                fieldName = attrValue;
            } else if (attrName.startsWith("type")) {
                final int typeNo = Integer.parseInt(attrName.substring(4));
                if (types == null || types.length <= typeNo) {
                    final int[] newTypes = new int[typeNo + 1];
                    if (types != null) {
                        System.arraycopy(types, 0, newTypes, 0, types.length);
                    }
                    types = newTypes;
                }
                types[typeNo] = mapType(attrValue);
            } else if (attrName.startsWith("field")) {
                final int fieldNo = Integer.parseInt(attrName.substring(5));
                if (fieldNames == null || fieldNames.length <= fieldNo) {
                    final String[] newFieldNames = new String[fieldNo + 1];
                    if (fieldNames != null) {
                        System.arraycopy(fieldNames, 0, newFieldNames, 0, fieldNames.length);
                    }
                    fieldNames = newFieldNames;
                }
                fieldNames[fieldNo] = attrValue;
            }
        }
        if (tkn != XMLScanner.XML_GT) {
            throwException("Unclosed element tag");
        }
        if (oid == 0) {
            throwException("ID is not specified or index");
        }
        if (className != null) {
            final Class cls = ClassDescriptor.loadClass(storage, className);
            if (fieldName != null) {
                if (indexType.equals("info.freelibrary.sodbox.impl.BtreeCaseInsensitiveFieldIndex")) {
                    btree = new BtreeCaseInsensitiveFieldIndex(cls, fieldName, unique, autoinc);
                } else {
                    btree = new BtreeFieldIndex(cls, fieldName, unique, autoinc);
                }
            } else if (fieldNames != null) {
                if (indexType.equals("info.freelibrary.sodbox.impl.BtreeCaseInsensitiveMultiFieldIndex")) {
                    btree = new BtreeCaseInsensitiveMultiFieldIndex(cls, fieldNames, unique);
                } else {
                    btree = new BtreeMultiFieldIndex(cls, fieldNames, unique);
                }
            } else {
                throwException("Field name is not specified for field index");
            }
        } else {
            if (types != null) {
                btree = new BtreeCompoundIndex(types, unique);
            } else if (type == null) {
                if (indexType.equals("info.freelibrary.sodbox.impl.PersistentSet")) {
                    btree = new PersistentSet(unique);
                } else {
                    throwException("Key type is not specified for index");
                }
            } else {
                btree = new Btree(mapType(type), unique);
            }
        }
        storage.assignOid(btree, oid, false);

        while ((tkn = scanner.scan()) == XMLScanner.XML_LT) {
            if (scanner.scan() != XMLScanner.XML_IDENT || !scanner.getIdentifier().equals("ref")) {
                throwException("<ref> element expected");
            }
            final XMLElement ref = readElement("ref");
            Key key = null;
            if (fieldNames != null) {
                final String[] values = new String[fieldNames.length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = getAttribute(ref, "key" + i);
                }
                key = createCompoundKey(((BtreeMultiFieldIndex) btree).types, values);
            } else if (types != null) {
                final String[] values = new String[types.length];
                for (int i = 0; i < values.length; i++) {
                    values[i] = getAttribute(ref, "key" + i);
                }
                key = createCompoundKey(types, values);
            } else {
                key = createKey(btree.type, getAttribute(ref, "key"));
            }
            final Object obj = new PersistentStub(storage, mapId(getIntAttribute(ref, "id")));
            btree.insert(key, obj, false);
        }
        if (tkn != XMLScanner.XML_LTS || scanner.scan() != XMLScanner.XML_IDENT || !scanner.getIdentifier().equals(
                indexType) || scanner.scan() != XMLScanner.XML_GT) {
            throwException("Element is not closed");
        }
        final byte[] data = storage.packObject(btree, false);
        final int size = ObjectHeader.getSize(data, 0);
        final long pos = storage.allocate(size, 0);
        storage.setPos(oid, pos | StorageImpl.DB_MODIFIED_FLAG);

        storage.myPagePool.put(pos & ~StorageImpl.DB_FLAGS_MASK, data, size);
    }

    final Key createKey(final int type, final String value) throws XMLImportException {
        try {
            Date date;
            switch (type) {
                case ClassDescriptor.tpBoolean:
                    return new Key(Integer.parseInt(value) != 0);
                case ClassDescriptor.tpByte:
                    return new Key(Byte.parseByte(value));
                case ClassDescriptor.tpChar:
                    return new Key((char) Integer.parseInt(value));
                case ClassDescriptor.tpShort:
                    return new Key(Short.parseShort(value));
                case ClassDescriptor.tpInt:
                case ClassDescriptor.tpEnum:
                    return new Key(Integer.parseInt(value));
                case ClassDescriptor.tpObject:
                    return new Key(new PersistentStub(storage, mapId(Integer.parseInt(value))));
                case ClassDescriptor.tpLong:
                    return new Key(Long.parseLong(value));
                case ClassDescriptor.tpFloat:
                    return new Key(Float.parseFloat(value));
                case ClassDescriptor.tpDouble:
                    return new Key(Double.parseDouble(value));
                case ClassDescriptor.tpString:
                    return new Key(value);
                case ClassDescriptor.tpArrayOfByte: {
                    final byte[] buf = new byte[value.length() >> 1];
                    for (int i = 0; i < buf.length; i++) {
                        buf[i] = (byte) (getHexValue(value.charAt(i * 2)) << 4 | getHexValue(value.charAt(i * 2 +
                                1)));
                    }
                    return new Key(buf);
                }
                case ClassDescriptor.tpDate:
                    if (value.equals("null")) {
                        date = null;
                    } else {
                        date = httpFormatter.parse(value, new ParsePosition(0));
                        if (date == null) {
                            throwException("Invalid date");
                        }
                    }
                    return new Key(date);
                default:
                    throwException("Bad key type");
            }
        } catch (final NumberFormatException x) {
            throwException("Failed to convert key value");
        }
        return null;
    }

    final void createObject(final XMLElement elem) throws XMLImportException {
        final Class cls = ClassDescriptor.loadClass(storage, elem.name);
        final ClassDescriptor desc = storage.getClassDescriptor(cls);
        final int oid = mapId(getIntAttribute(elem, "id"));
        final ByteBuffer buf = new ByteBuffer();
        int offs = ObjectHeader.sizeof;
        buf.extend(offs);

        offs = packObject(elem, desc, offs, buf);

        ObjectHeader.setSize(buf.arr, 0, offs);
        ObjectHeader.setType(buf.arr, 0, desc.getOid());

        final long pos = storage.allocate(offs, 0);
        storage.setPos(oid, pos | StorageImpl.DB_MODIFIED_FLAG);
        storage.myPagePool.put(pos, buf.arr, offs);
    }

    final String getAttribute(final XMLElement elem, final String name) throws XMLImportException {
        final String value = elem.getAttribute(name);
        if (value == null) {
            throwException("Attribute " + name + " is not set");
        }
        return value;
    }

    final int getHexValue(final char ch) throws XMLImportException {
        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        } else if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        } else if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        } else {
            throwException("Bad hexadecimal constant");
        }
        return -1;
    }

    final int getIntAttribute(final XMLElement elem, final String name) throws XMLImportException {
        final String value = elem.getAttribute(name);
        if (value == null) {
            throwException("Attribute " + name + " is not set");
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException x) {
            throwException("Attribute " + name + " should has integer value");
        }
        return -1;
    }

    final int importBinary(final XMLElement elem, int offs, final ByteBuffer buf, final String fieldName)
            throws XMLImportException {
        if (elem == null || elem.isNullValue()) {
            offs = buf.packI4(offs, -1);
        } else if (elem.isStringValue()) {
            final String hexStr = elem.getStringValue();
            final int len = hexStr.length();
            buf.extend(offs + 4 + len / 2);
            Bytes.pack4(buf.arr, offs, len / 2);
            offs += 4;
            for (int j = 0; j < len; j += 2) {
                buf.arr[offs++] = (byte) (getHexValue(hexStr.charAt(j)) << 4 | getHexValue(hexStr.charAt(j + 1)));
            }
        } else {
            final XMLElement ref = elem.getSibling("ref");
            if (ref != null) {
                buf.extend(offs + 4);
                Bytes.pack4(buf.arr, offs, mapId(getIntAttribute(ref, "id")));
                offs += 4;
            } else {
                XMLElement item = elem.getSibling("element");
                int len = item == null ? 0 : item.getCounter();
                buf.extend(offs + 4 + len);
                Bytes.pack4(buf.arr, offs, len);
                offs += 4;
                while (--len >= 0) {
                    if (item.isIntValue()) {
                        buf.arr[offs] = (byte) item.getIntValue();
                    } else if (item.isRealValue()) {
                        buf.arr[offs] = (byte) item.getRealValue();
                    } else {
                        throwException("Conversion for field " + fieldName + " is not possible");
                    }
                    item = item.getNextSibling();
                    offs += 1;
                }
            }
        }
        return offs;
    }

    public void importDatabase() throws XMLImportException {
        if (scanner.scan() != XMLScanner.XML_LT || scanner.scan() != XMLScanner.XML_IDENT || !scanner.getIdentifier()
                .equals("database")) {
            throwException("No root element");
        }
        if (scanner.scan() != XMLScanner.XML_IDENT || !scanner.getIdentifier().equals("root") || scanner
                .scan() != XMLScanner.XML_EQ || scanner.scan() != XMLScanner.XML_SCONST || scanner
                        .scan() != XMLScanner.XML_GT) {
            throwException("Database element should have \"root\" attribute");
        }
        int rootId = 0;
        try {
            rootId = Integer.parseInt(scanner.getString());
        } catch (final NumberFormatException x) {
            throwException("Incorrect root object specification");
        }
        idMap = new int[rootId * 2];
        idMap[rootId] = storage.allocateId();
        storage.myHeader.myRootPage[1 - storage.myCurrentIndex].myRootObject = idMap[rootId];

        int tkn;
        while ((tkn = scanner.scan()) == XMLScanner.XML_LT) {
            if (scanner.scan() != XMLScanner.XML_IDENT) {
                throwException("Element name expected");
            }
            final String elemName = scanner.getIdentifier();
            if (elemName.equals("info.freelibrary.sodbox.impl.Btree") || elemName.equals(
                    "info.freelibrary.sodbox.impl.BitIndexImpl") || elemName.equals(
                            "info.freelibrary.sodbox.impl.PersistentSet") || elemName.equals(
                                    "info.freelibrary.sodbox.impl.BtreeFieldIndex") || elemName.equals(
                                            "info.freelibrary.sodbox.impl.BtreeCaseInsensitiveFieldIndex") || elemName
                                                    .equals("info.freelibrary.sodbox.impl.BtreeCompoundIndex") ||
                    elemName.equals("info.freelibrary.sodbox.impl.BtreeMultiFieldIndex") || elemName.equals(
                            "info.freelibrary.sodbox.impl.BtreeCaseInsensitiveMultiFieldIndex")) {
                createIndex(elemName);
            } else {
                createObject(readElement(elemName));
            }
        }
        if (tkn != XMLScanner.XML_LTS || scanner.scan() != XMLScanner.XML_IDENT || !scanner.getIdentifier().equals(
                "database") || scanner.scan() != XMLScanner.XML_GT) {
            throwException("Root element is not closed");
        }
    }

    int importRef(final XMLElement elem, int offs, final ByteBuffer buf) throws XMLImportException {
        int oid = 0;
        if (elem != null) {
            if (elem.isStringValue()) {
                final String str = elem.getStringValue();
                offs = buf.packI4(offs, -1 - ClassDescriptor.tpString);
                return buf.packString(offs, str);
            } else {
                final XMLElement value = elem.getFirstSibling();
                if (value == null) {
                    throwException("object reference expected");
                }
                final String name = value.getName();
                if (name.equals("scalar")) {
                    final int tid = getIntAttribute(value, "type");
                    final String hexStr = getAttribute(value, "value");
                    final int len = hexStr.length();
                    buf.extend(offs + 4 + len / 2);
                    Bytes.pack4(buf.arr, offs, -1 - tid);
                    offs += 4;
                    if (tid == ClassDescriptor.tpCustom) {
                        try {
                            final Object obj = storage.mySerializer.parse(hexStr);
                            storage.mySerializer.pack(obj, buf.getOutputStream());
                            offs = buf.size();
                        } catch (final Exception x) {
                            throwException("exception in custom serializer: " + x);
                        }
                    } else {
                        for (int j = 0; j < len; j += 2) {
                            buf.arr[offs++] = (byte) (getHexValue(hexStr.charAt(j)) << 4 | getHexValue(hexStr.charAt(
                                    j + 1)));
                        }
                    }
                    return offs;
                } else if (name == "class") {
                    final String className = getAttribute(value, "name");
                    offs = buf.packI4(offs, -1 - ClassDescriptor.tpClass);
                    return buf.packString(offs, className);
                } else if (name.equals("ref")) {
                    oid = mapId(getIntAttribute(value, "id"));
                } else {
                    final Class cls = ClassDescriptor.loadClass(storage, name);
                    final ClassDescriptor desc = storage.getClassDescriptor(cls);
                    offs = buf.packI4(offs, -ClassDescriptor.tpValueTypeBias - desc.getOid());
                    if (desc.isCollection) {
                        XMLElement item = value.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        offs = buf.packI4(offs, len);
                        while (--len >= 0) {
                            offs = importRef(item, offs, buf);
                            item = item.getNextSibling();
                        }
                    } else {
                        offs = packObject(value, desc, offs, buf);
                    }
                    return offs;
                }
            }
        }
        return buf.packI4(offs, oid);
    }

    final int mapId(final int id) {
        int oid = 0;
        if (id != 0) {
            if (id >= idMap.length) {
                final int[] newMap = new int[id * 2];
                System.arraycopy(idMap, 0, newMap, 0, idMap.length);
                idMap = newMap;
                idMap[id] = oid = storage.allocateId();
            } else {
                oid = idMap[id];
                if (oid == 0) {
                    idMap[id] = oid = storage.allocateId();
                }
            }
        }
        return oid;
    }

    final int mapType(final String signature) throws XMLImportException {
        for (int i = 0; i < ClassDescriptor.signature.length; i++) {
            if (ClassDescriptor.signature[i].equals(signature)) {
                return i;
            }
        }
        throwException("Bad type");
        return -1;
    }

    final int packObject(final XMLElement objElem, final ClassDescriptor desc, int offs, final ByteBuffer buf)
            throws XMLImportException {
        final ClassDescriptor.FieldDescriptor[] flds = desc.allFields;
        for (final FieldDescriptor fd : flds) {
            final String fieldName = fd.fieldName;
            final XMLElement elem = objElem != null ? objElem.getSibling(fieldName) : null;

            switch (fd.type) {
                case ClassDescriptor.tpByte:
                    buf.extend(offs + 1);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            buf.arr[offs] = (byte) elem.getIntValue();
                        } else if (elem.isRealValue()) {
                            buf.arr[offs] = (byte) elem.getRealValue();
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 1;
                    continue;
                case ClassDescriptor.tpBoolean:
                    buf.extend(offs + 1);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            buf.arr[offs] = (byte) (elem.getIntValue() != 0 ? 1 : 0);
                        } else if (elem.isRealValue()) {
                            buf.arr[offs] = (byte) (elem.getRealValue() != 0.0 ? 1 : 0);
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 1;
                    continue;
                case ClassDescriptor.tpShort:
                case ClassDescriptor.tpChar:
                    buf.extend(offs + 2);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack2(buf.arr, offs, (short) elem.getIntValue());
                        } else if (elem.isRealValue()) {
                            Bytes.pack2(buf.arr, offs, (short) elem.getRealValue());
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 2;
                    continue;
                case ClassDescriptor.tpInt:
                    buf.extend(offs + 4);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack4(buf.arr, offs, (int) elem.getIntValue());
                        } else if (elem.isRealValue()) {
                            Bytes.pack4(buf.arr, offs, (int) elem.getRealValue());
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 4;
                    continue;
                case ClassDescriptor.tpLong:
                    buf.extend(offs + 8);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack8(buf.arr, offs, elem.getIntValue());
                        } else if (elem.isRealValue()) {
                            Bytes.pack8(buf.arr, offs, (long) elem.getRealValue());
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 8;
                    continue;
                case ClassDescriptor.tpFloat:
                    buf.extend(offs + 4);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack4(buf.arr, offs, Float.floatToIntBits(elem.getIntValue()));
                        } else if (elem.isRealValue()) {
                            Bytes.pack4(buf.arr, offs, Float.floatToIntBits((float) elem.getRealValue()));
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 4;
                    continue;
                case ClassDescriptor.tpDouble:
                    buf.extend(offs + 8);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack8(buf.arr, offs, Double.doubleToLongBits(elem.getIntValue()));
                        } else if (elem.isRealValue()) {
                            Bytes.pack8(buf.arr, offs, Double.doubleToLongBits(elem.getRealValue()));
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 8;
                    continue;
                case ClassDescriptor.tpEnum:
                    buf.extend(offs + 4);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack4(buf.arr, offs, (int) elem.getIntValue());
                        } else if (elem.isRealValue()) {
                            Bytes.pack4(buf.arr, offs, (int) elem.getRealValue());
                        } else if (elem.isNullValue()) {
                            Bytes.pack4(buf.arr, offs, -1);
                        } else if (elem.isStringValue()) {
                            Bytes.pack4(buf.arr, offs, Enum.valueOf((Class) fd.field.getType(), elem.getStringValue())
                                    .ordinal());
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 4;
                    continue;
                case ClassDescriptor.tpDate:
                    buf.extend(offs + 8);
                    if (elem != null) {
                        if (elem.isIntValue()) {
                            Bytes.pack8(buf.arr, offs, elem.getIntValue());
                        } else if (elem.isNullValue()) {
                            Bytes.pack8(buf.arr, offs, -1);
                        } else if (elem.isStringValue()) {
                            final Date date = httpFormatter.parse(elem.getStringValue(), new ParsePosition(0));
                            if (date == null) {
                                throwException("Invalid date");
                            }
                            Bytes.pack8(buf.arr, offs, date.getTime());
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                    }
                    offs += 8;
                    continue;
                case ClassDescriptor.tpString:
                case ClassDescriptor.tpClass:
                    if (elem != null) {
                        String value = null;
                        if (elem.isIntValue()) {
                            value = Long.toString(elem.getIntValue());
                        } else if (elem.isRealValue()) {
                            value = Double.toString(elem.getRealValue());
                        } else if (elem.isStringValue()) {
                            value = elem.getStringValue();
                        } else if (elem.isNullValue()) {
                            value = null;
                        } else {
                            throwException("Conversion for field " + fieldName + " is not possible");
                        }
                        offs = buf.packString(offs, value);
                        continue;
                    }
                    offs = buf.packI4(offs, -1);
                    continue;
                case ClassDescriptor.tpObject:
                    offs = importRef(elem, offs, buf);
                    continue;
                case ClassDescriptor.tpValue:
                    offs = packObject(elem, fd.valueDesc, offs, buf);
                    continue;
                case ClassDescriptor.tpRaw:
                case ClassDescriptor.tpArrayOfByte:
                    offs = importBinary(elem, offs, buf, fieldName);
                    continue;
                case ClassDescriptor.tpCustom: {
                    if (!elem.isStringValue()) {
                        throwException("text element expected");
                    }
                    final String str = elem.getStringValue();
                    try {
                        final Object obj = storage.mySerializer.parse(str);
                        storage.mySerializer.pack(obj, buf.getOutputStream());
                        offs = buf.size();
                    } catch (final Exception x) {
                        throwException("exception in custom serializer: " + x);
                    }
                    break;
                }
                case ClassDescriptor.tpArrayOfBoolean:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                buf.arr[offs] = (byte) (item.getIntValue() != 0 ? 1 : 0);
                            } else if (item.isRealValue()) {
                                buf.arr[offs] = (byte) (item.getRealValue() != 0.0 ? 1 : 0);
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 1;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfChar:
                case ClassDescriptor.tpArrayOfShort:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 2);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                Bytes.pack2(buf.arr, offs, (short) item.getIntValue());
                            } else if (item.isRealValue()) {
                                Bytes.pack2(buf.arr, offs, (short) item.getRealValue());
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 2;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfInt:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 4);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                Bytes.pack4(buf.arr, offs, (int) item.getIntValue());
                            } else if (item.isRealValue()) {
                                Bytes.pack4(buf.arr, offs, (int) item.getRealValue());
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 4;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfEnum:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 4);
                        Bytes.pack4(buf.arr, offs, len);
                        final Class enumType = fd.field.getType();
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                Bytes.pack4(buf.arr, offs, (int) item.getIntValue());
                            } else if (item.isRealValue()) {
                                Bytes.pack4(buf.arr, offs, (int) item.getRealValue());
                            } else if (item.isNullValue()) {
                                Bytes.pack4(buf.arr, offs, -1);
                            } else if (item.isStringValue()) {
                                Bytes.pack4(buf.arr, offs, Enum.valueOf(enumType, item.getStringValue()).ordinal());
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 4;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfLong:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 8);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                Bytes.pack8(buf.arr, offs, item.getIntValue());
                            } else if (item.isRealValue()) {
                                Bytes.pack8(buf.arr, offs, (long) item.getRealValue());
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 8;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfFloat:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 4);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                Bytes.pack4(buf.arr, offs, Float.floatToIntBits(item.getIntValue()));
                            } else if (item.isRealValue()) {
                                Bytes.pack4(buf.arr, offs, Float.floatToIntBits((float) item.getRealValue()));
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 4;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfDouble:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 8);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isIntValue()) {
                                Bytes.pack8(buf.arr, offs, Double.doubleToLongBits(item.getIntValue()));
                            } else if (item.isRealValue()) {
                                Bytes.pack8(buf.arr, offs, Double.doubleToLongBits(item.getRealValue()));
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 8;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfDate:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4 + len * 8);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            if (item.isNullValue()) {
                                Bytes.pack8(buf.arr, offs, -1);
                            } else if (item.isStringValue()) {
                                final Date date = httpFormatter.parse(item.getStringValue(), new ParsePosition(0));
                                if (date == null) {
                                    throwException("Invalid date");
                                }
                                Bytes.pack8(buf.arr, offs, date.getTime());
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            item = item.getNextSibling();
                            offs += 8;
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfString:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        buf.extend(offs + 4);
                        Bytes.pack4(buf.arr, offs, len);
                        offs += 4;
                        while (--len >= 0) {
                            String value = null;
                            if (item.isIntValue()) {
                                value = Long.toString(item.getIntValue());
                            } else if (item.isRealValue()) {
                                value = Double.toString(item.getRealValue());
                            } else if (item.isStringValue()) {
                                value = item.getStringValue();
                            } else if (item.isNullValue()) {
                                value = null;
                            } else {
                                throwException("Conversion for field " + fieldName + " is not possible");
                            }
                            offs = buf.packString(offs, value);
                            item = item.getNextSibling();
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfObject:
                case ClassDescriptor.tpLink:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        offs = buf.packI4(offs, len);
                        while (--len >= 0) {
                            offs = importRef(item, offs, buf);
                            item = item.getNextSibling();
                        }
                    }
                    continue;
                case ClassDescriptor.tpArrayOfValue:
                    if (elem == null || elem.isNullValue()) {
                        offs = buf.packI4(offs, -1);
                    } else {
                        XMLElement item = elem.getSibling("element");
                        int len = item == null ? 0 : item.getCounter();
                        offs = buf.packI4(offs, len);
                        final ClassDescriptor elemDesc = fd.valueDesc;
                        while (--len >= 0) {
                            offs = packObject(item, elemDesc, offs, buf);
                            item = item.getNextSibling();
                        }
                    }
                    continue;
            }
        }
        return offs;
    }

    final int parseInt(final String str) throws XMLImportException {
        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException x) {
            throwException("Bad integer constant");
        }
        return -1;
    }

    final XMLElement readElement(final String name) throws XMLImportException {
        final XMLElement elem = new XMLElement(name);
        String attribute;
        int tkn;
        while (true) {
            switch (scanner.scan()) {
                case XMLScanner.XML_GTS:
                    return elem;
                case XMLScanner.XML_GT:
                    while ((tkn = scanner.scan()) == XMLScanner.XML_LT) {
                        if (scanner.scan() != XMLScanner.XML_IDENT) {
                            throwException("Element name expected");
                        }
                        final String siblingName = scanner.getIdentifier();
                        final XMLElement sibling = readElement(siblingName);
                        elem.addSibling(sibling);
                    }
                    switch (tkn) {
                        case XMLScanner.XML_SCONST:
                            elem.setStringValue(scanner.getString());
                            tkn = scanner.scan();
                            break;
                        case XMLScanner.XML_ICONST:
                            elem.setIntValue(scanner.getInt());
                            tkn = scanner.scan();
                            break;
                        case XMLScanner.XML_FCONST:
                            elem.setRealValue(scanner.getReal());
                            tkn = scanner.scan();
                            break;
                        case XMLScanner.XML_IDENT:
                            if (scanner.getIdentifier().equals("null")) {
                                elem.setNullValue();
                            } else {
                                elem.setStringValue(scanner.getIdentifier());
                            }
                            tkn = scanner.scan();
                    }
                    if (tkn != XMLScanner.XML_LTS || scanner.scan() != XMLScanner.XML_IDENT || !scanner
                            .getIdentifier().equals(name) || scanner.scan() != XMLScanner.XML_GT) {
                        throwException("Element is not closed");
                    }
                    return elem;
                case XMLScanner.XML_IDENT:
                    attribute = scanner.getIdentifier();
                    if (scanner.scan() != XMLScanner.XML_EQ || scanner.scan() != XMLScanner.XML_SCONST) {
                        throwException("Attribute value expected");
                    }
                    elem.addAttribute(attribute, scanner.getString());
                    continue;
                default:
                    throwException("Unexpected token");
            }
        }
    }

    final void throwException(final String message) throws XMLImportException {
        throw new XMLImportException(scanner.getLine(), scanner.getColumn(), message);
    }
}
