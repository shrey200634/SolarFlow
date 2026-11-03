package com.solarFlow.solarFlow.bill;

import com.solarFlow.solarFlow.bill.storage.LocalDocumentStorage;
import com.solarFlow.solarFlow.bill.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalDocumentStorageTest {

    @TempDir
    Path tmp;

    @Test
    void saveAndOpenRoundTrip() throws Exception {
        LocalDocumentStorage s = new LocalDocumentStorage(tmp.toString());
        byte[] data = "hello bill".getBytes();

        String key = s.save(new ByteArrayInputStream(data));

        assertEquals(32, key.length());
        try (var in = s.open(key)) {
            assertArrayEquals(data, in.readAllBytes());
        }
    }

    @Test
    void deleteRemovesFile() {
        LocalDocumentStorage s = new LocalDocumentStorage(tmp.toString());
        String key = s.save(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        s.delete(key);

        assertThrows(StorageException.class, () -> s.open(key));
    }

    @Test
    void rejectsPathTraversalKeys() {
        LocalDocumentStorage s = new LocalDocumentStorage(tmp.toString());

        assertThrows(StorageException.class, () -> s.open("../../etc/passwd"));
        assertThrows(StorageException.class, () -> s.open("..\\..\\secret"));
        assertThrows(StorageException.class, () -> s.open("short"));
        assertThrows(StorageException.class, () -> s.delete("../x"));
    }
}