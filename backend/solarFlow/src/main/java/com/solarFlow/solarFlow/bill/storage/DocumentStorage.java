package com.solarFlow.solarFlow.bill.storage;

import java.io.InputStream;

public interface DocumentStorage {

    String save(InputStream in);

    InputStream open(String key);

    void delete(String key);
}