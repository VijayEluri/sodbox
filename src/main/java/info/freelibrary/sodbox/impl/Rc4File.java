
package info.freelibrary.sodbox.impl;

import info.freelibrary.sodbox.IFile;

// Rc4Cipher - the RC4 encryption method
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/

public class Rc4File implements IFile {

    private final IFile file;

    private byte[] cipherBuf;

    private byte[] pattern;

    private long length;

    private byte[] zeroPage;

    public Rc4File(final IFile file, final String key) {
        this.file = file;
        length = file.length() & ~(Page.pageSize - 1);
        setKey(key.getBytes());
    }

    public Rc4File(final String filePath, final boolean readOnly, final boolean noFlush, final String key) {
        file = new OSFile(filePath, readOnly, noFlush);
        length = file.length() & ~(Page.pageSize - 1);
        setKey(key.getBytes());
    }

    @Override
    public void close() {
        file.close();
    }

    private final void crypt(final byte[] clearText, final byte[] cipherText) {
        for (int i = 0; i < clearText.length; i++) {
            cipherText[i] = (byte) (clearText[i] ^ pattern[i]);
        }
    }

    @Override
    public long length() {
        return file.length();
    }

    @Override
    public void lock(final boolean shared) {
        file.lock(shared);
        length = file.length() & ~(Page.pageSize - 1);
    }

    @Override
    public int read(final long pos, final byte[] buf) {
        if (pos < length) {
            final int rc = file.read(pos, buf);
            crypt(buf, buf);
            return rc;
        }
        return 0;
    }

    private void setKey(final byte[] key) {
        final byte[] state = new byte[256];
        for (int counter = 0; counter < 256; ++counter) {
            state[counter] = (byte) counter;
        }
        int index1 = 0;
        int index2 = 0;
        for (int counter = 0; counter < 256; ++counter) {
            index2 = key[index1] + state[counter] + index2 & 0xff;
            final byte temp = state[counter];
            state[counter] = state[index2];
            state[index2] = temp;
            index1 = (index1 + 1) % key.length;
        }
        pattern = new byte[Page.pageSize];
        cipherBuf = new byte[Page.pageSize];
        int x = 0;
        int y = 0;
        for (int i = 0; i < Page.pageSize; i++) {
            x = x + 1 & 0xff;
            y = y + state[x] & 0xff;
            final byte temp = state[x];
            state[x] = state[y];
            state[y] = temp;
            pattern[i] = state[state[x] + state[y] & 0xff];
        }
    }

    @Override
    public void sync() {
        file.sync();
    }

    @Override
    public boolean tryLock(final boolean shared) {
        return file.tryLock(shared);
    }

    @Override
    public void unlock() {
        file.unlock();
    }

    @Override
    public void write(final long pos, final byte[] buf) {
        if (pos > length) {
            if (zeroPage == null) {
                zeroPage = new byte[Page.pageSize];
                crypt(zeroPage, zeroPage);
            }
            do {
                file.write(length, zeroPage);
            } while ((length += Page.pageSize) < pos);
        }
        if (pos == length) {
            length += Page.pageSize;
        }
        crypt(buf, cipherBuf);
        file.write(pos, cipherBuf);
    }
}
