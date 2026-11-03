package com.solarFlow.solarFlow.bill;

import com.solarFlow.solarFlow.auth.AuthUser;
import com.solarFlow.solarFlow.lead.Lead;
import com.solarFlow.solarFlow.lead.LeadService;
import com.solarFlow.solarFlow.lead.LeadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillDocumentService {

    private final LeadService leadService;
    private final LeadDocumentRepo documents;
    private final DocumentStorage storage;
    private final DocumentValidator validator;

    @Transactional
    public DocumentResponseDTO upload(UUID leadId, MultipartFile file, AuthUser actor) {
        Lead lead = leadService.findAccessible(leadId, actor);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }

        String contentType = validator.validate(data);
        String sha256 = sha256Hex(data);

        String key = storage.save(new ByteArrayInputStream(data));
        try {
            LeadDocument doc = LeadDocument.builder()
                    .lead(lead)
                    .storageKey(key)
                    .contentType(contentType)
                    .sizeBytes(data.length)
                    .sha256(sha256)
                    .status(DocumentStatus.UPLOADED)
                    .build();

            LeadDocument saved = documents.saveAndFlush(doc);
            if (lead.getStatus() == LeadStatus.NEW) {
                lead.setStatus(LeadStatus.BILL_UPLOADED);
            }
            return DocumentResponseDTO.from(saved);
        } catch (RuntimeException e) {
            storage.delete(key);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> listForLead(UUID leadId, AuthUser actor) {
        leadService.findAccessible(leadId, actor);
        return documents.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(DocumentResponseDTO::from)
                .toList();
    }

    private static String sha256Hex(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}