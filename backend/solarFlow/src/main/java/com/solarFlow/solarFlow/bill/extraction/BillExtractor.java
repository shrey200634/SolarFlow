package com.solarFlow.solarFlow.bill.extraction;

public interface BillExtractor {

    ExtractionResult extract(byte[] data, String contentType);

    default String name (){
        return  getClass().getSimpleName();
    }
}