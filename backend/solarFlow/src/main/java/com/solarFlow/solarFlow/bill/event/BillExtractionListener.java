package com.solarFlow.solarFlow.bill.event;

import com.solarFlow.solarFlow.bill.service.BillExtractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BillExtractionListener {

    private final BillExtractionService service;

    // AFTER_COMMIT: the document row is guaranteed visible to the background thread
    @Async("extractionExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUploaded(DocumentUploadedEvent event) {
        service.extract(event.documentId());
    }
}