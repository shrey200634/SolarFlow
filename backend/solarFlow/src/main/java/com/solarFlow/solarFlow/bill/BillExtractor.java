package com.solarFlow.solarFlow.bill;

public interface BillExtractor {

    ExtractionResult extract(byte[] data, String contentType);
}