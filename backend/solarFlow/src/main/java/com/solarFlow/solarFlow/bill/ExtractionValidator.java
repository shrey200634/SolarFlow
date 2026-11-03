package com.solarFlow.solarFlow.bill;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ExtractionValidator {

    public static final String CONSUMER_NUMBER = "consumerNumber";
    public static final String TARIFF = "tariff";
    public static final String SANCTIONED_LOAD = "sanctionedLoadKw";
    public static final String MONTHLY_UNITS = "monthlyUnits";

    public record FieldReport(String field, double confidence, boolean reviewRequired, List<String> issues) { }

    public record Report(List<FieldReport> fields, boolean lowQuality) {

        public FieldReport field(String name) {
            return fields.stream().filter(f -> f.field().equals(name)).findFirst().orElseThrow();
        }

        public List<String> reviewFields() {
            return fields.stream().filter(FieldReport::reviewRequired).map(FieldReport::field).toList();
        }
    }

    private final Pattern consumerPattern;
    private final Pattern tariffPattern;
    private final double fieldThreshold;
    private final double lowQualityThreshold;
    private final BigDecimal minLoadKw;
    private final BigDecimal maxLoadKw;
    private final BigDecimal maxUnits;
    private final BigDecimal minRate;
    private final BigDecimal maxRate;

    public ExtractionValidator(
            @Value("${solarflow.bill.consumer-number-pattern}") String consumerPattern,
            @Value("${solarflow.bill.tariff-pattern}") String tariffPattern,
            @Value("${solarflow.bill.field-confidence-threshold}") double fieldThreshold,
            @Value("${solarflow.bill.low-quality-threshold}") double lowQualityThreshold,
            @Value("${solarflow.bill.sanctioned-load-min-kw}") BigDecimal minLoadKw,
            @Value("${solarflow.bill.sanctioned-load-max-kw}") BigDecimal maxLoadKw,
            @Value("${solarflow.bill.monthly-units-max}") BigDecimal maxUnits,
            @Value("${solarflow.bill.implied-rate-min}") BigDecimal minRate,
            @Value("${solarflow.bill.implied-rate-max}") BigDecimal maxRate) {
        this.consumerPattern = Pattern.compile(consumerPattern);
        this.tariffPattern = Pattern.compile(tariffPattern);
        this.fieldThreshold = fieldThreshold;
        this.lowQualityThreshold = lowQualityThreshold;
        this.minLoadKw = minLoadKw;
        this.maxLoadKw = maxLoadKw;
        this.maxUnits = maxUnits;
        this.minRate = minRate;
        this.maxRate = maxRate;
    }

    /** declaredMonthlyBill is what the customer typed on the lead form (may be null). */
    public Report validate(ExtractionResult r, BigDecimal declaredMonthlyBill) {
        List<FieldReport> fields = new ArrayList<>();
        fields.add(checkConsumerNumber(r.consumerNumber()));
        fields.add(checkTariff(r.tariff()));
        fields.add(checkLoad(r.sanctionedLoadKw()));
        fields.add(checkUnits(r.monthlyUnits(), declaredMonthlyBill));

        boolean nothingRead = r.consumerNumber().value() == null && r.tariff().value() == null
                && r.sanctionedLoadKw().value() == null && r.monthlyUnits().value() == null;
        boolean lowQuality = nothingRead || r.overallConfidence() < lowQualityThreshold;

        return new Report(fields, lowQuality);
    }

    // ---------------- per-field checks ----------------

    private FieldReport checkConsumerNumber(ExtractionResult.Field<String> f) {
        List<String> issues = new ArrayList<>();
        if (f.value() == null) {
            issues.add("MISSING");
        } else {
            if (!consumerPattern.matcher(f.value()).matches()) issues.add("BAD_FORMAT");
            addLowConfidence(f.confidence(), issues);
        }
        return report(CONSUMER_NUMBER, f.confidence(), issues);
    }

    private FieldReport checkTariff(ExtractionResult.Field<String> f) {
        List<String> issues = new ArrayList<>();
        if (f.value() == null) {
            issues.add("MISSING");
        } else {
            if (!tariffPattern.matcher(f.value()).matches()) issues.add("UNKNOWN_TARIFF");
            addLowConfidence(f.confidence(), issues);
        }
        return report(TARIFF, f.confidence(), issues);
    }

    private FieldReport checkLoad(ExtractionResult.Field<BigDecimal> f) {
        List<String> issues = new ArrayList<>();
        if (f.value() == null) {
            issues.add("MISSING");   // optional for sizing, but highlighted so the customer can fill it in
        } else {
            if (f.value().compareTo(minLoadKw) < 0 || f.value().compareTo(maxLoadKw) > 0) issues.add("OUT_OF_RANGE");
            addLowConfidence(f.confidence(), issues);
        }
        return report(SANCTIONED_LOAD, f.confidence(), issues);
    }

    private FieldReport checkUnits(ExtractionResult.Field<BigDecimal> f, BigDecimal declaredBill) {
        List<String> issues = new ArrayList<>();
        if (f.value() == null) {
            issues.add("MISSING");
        } else {
            BigDecimal units = f.value();
            if (units.signum() < 0 || units.compareTo(maxUnits) > 0) {
                issues.add("OUT_OF_RANGE");
            } else if (units.signum() > 0 && declaredBill != null && declaredBill.signum() > 0) {
                // implied ₹ per unit = declared bill / units (BigDecimal, scale 2, HALF_UP)
                BigDecimal rate = declaredBill.divide(units, 2, RoundingMode.HALF_UP);
                if (rate.compareTo(minRate) < 0 || rate.compareTo(maxRate) > 0) issues.add("UNITS_BILL_MISMATCH");
            }
            addLowConfidence(f.confidence(), issues);
        }
        return report(MONTHLY_UNITS, f.confidence(), issues);
    }

    private void addLowConfidence(double confidence, List<String> issues) {
        if (confidence < fieldThreshold) issues.add("LOW_CONFIDENCE");
    }

    private static FieldReport report(String field, double confidence, List<String> issues) {
        return new FieldReport(field, confidence, !issues.isEmpty(), List.copyOf(issues));
    }
}