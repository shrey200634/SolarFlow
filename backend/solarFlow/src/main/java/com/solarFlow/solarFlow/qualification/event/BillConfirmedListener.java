package com.solarFlow.solarFlow.qualification.event;

import com.solarFlow.solarFlow.bill.event.BillConfirmedEvent;
import com.solarFlow.solarFlow.qualification.service.QualificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillConfirmedListener {

    private final QualificationService service;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBillConfirmed(BillConfirmedEvent event) {
        try {
            service.qualifyAfterConfirmation(event.leadId());
        } catch (RuntimeException e) {
            log.error("Automatic qualification failed for lead {}; sales can re-run it manually",
                    event.leadId(), e);
        }
    }
}