
package info.freelibrary.sodbox.impl;

import info.freelibrary.sodbox.Assert;
import info.freelibrary.sodbox.Key;

class BtreeKey {

    Key key;

    int oid;

    int oldOid;

    BtreeKey(final Key key, final int oid) {
        this.key = key;
        this.oid = oid;
    }

    final void extract(final Page pg, final int offs, final int type) {
        final byte[] data = pg.data;

        switch (type) {
            case ClassDescriptor.tpBoolean:
                key = new Key(data[offs] != 0);
                break;
            case ClassDescriptor.tpByte:
                key = new Key(data[offs]);
                break;
            case ClassDescriptor.tpShort:
                key = new Key(Bytes.unpack2(data, offs));
                break;
            case ClassDescriptor.tpChar:
                key = new Key((char) Bytes.unpack2(data, offs));
                break;
            case ClassDescriptor.tpInt:
            case ClassDescriptor.tpObject:
            case ClassDescriptor.tpEnum:
                key = new Key(Bytes.unpack4(data, offs));
                break;
            case ClassDescriptor.tpLong:
            case ClassDescriptor.tpDate:
                key = new Key(Bytes.unpack8(data, offs));
                break;
            case ClassDescriptor.tpFloat:
                key = new Key(Bytes.unpackF4(data, offs));
                break;
            case ClassDescriptor.tpDouble:
                key = new Key(Bytes.unpackF8(data, offs));
                break;
            default:
                Assert.failed("Invalid type");
        }
    }

    final void getByteArray(final Page pg, final int i) {
        final int len = BtreePage.getKeyStrSize(pg, i);
        final int offs = BtreePage.firstKeyOffs + BtreePage.getKeyStrOffs(pg, i);
        final byte[] bval = new byte[len];
        System.arraycopy(pg.data, offs, bval, 0, len);
        key = new Key(bval);
    }

    final void getStr(final Page pg, final int i) {
        final int len = BtreePage.getKeyStrSize(pg, i);
        int offs = BtreePage.firstKeyOffs + BtreePage.getKeyStrOffs(pg, i);
        final char[] sval = new char[len];
        for (int j = 0; j < len; j++) {
            sval[j] = (char) Bytes.unpack2(pg.data, offs);
            offs += 2;
        }
        key = new Key(sval);
    }

    final void pack(final Page pg, final int i) {
        final byte[] dst = pg.data;
        switch (key.type) {
            case ClassDescriptor.tpBoolean:
            case ClassDescriptor.tpByte:
                dst[BtreePage.firstKeyOffs + i] = (byte) key.ival;
                break;
            case ClassDescriptor.tpShort:
            case ClassDescriptor.tpChar:
                Bytes.pack2(dst, BtreePage.firstKeyOffs + i * 2, (short) key.ival);
                break;
            case ClassDescriptor.tpInt:
            case ClassDescriptor.tpObject:
            case ClassDescriptor.tpEnum:
                Bytes.pack4(dst, BtreePage.firstKeyOffs + i * 4, key.ival);
                break;
            case ClassDescriptor.tpLong:
            case ClassDescriptor.tpDate:
                Bytes.pack8(dst, BtreePage.firstKeyOffs + i * 8, key.lval);
                break;
            case ClassDescriptor.tpFloat:
                Bytes.pack4(dst, BtreePage.firstKeyOffs + i * 4, Float.floatToIntBits((float) key.dval));
                break;
            case ClassDescriptor.tpDouble:
                Bytes.pack8(dst, BtreePage.firstKeyOffs + i * 8, Double.doubleToLongBits(key.dval));
                break;
            default:
                Assert.failed("Invalid type");
        }
        Bytes.pack4(dst, BtreePage.firstKeyOffs + (BtreePage.maxItems - i - 1) * 4, oid);
    }
}
