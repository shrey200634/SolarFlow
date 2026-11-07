import type { BillFieldKey } from '../../api/documents.api'

/** Backend validation codes (ExtractionValidator.validateConfirmedValues) in plain words. */
const issues: Record<string, { field: BillFieldKey; message: string }> = {
  CONSUMER_NUMBER_MISSING: { field: 'consumerNumber', message: 'Enter the consumer number from your bill.' },
  CONSUMER_NUMBER_BAD_FORMAT: { field: 'consumerNumber', message: 'This consumer number looks wrong. Check it against your bill.' },
  TARIFF_MISSING: { field: 'tariff', message: 'Enter the tariff category from your bill.' },
  TARIFF_UNKNOWN: { field: 'tariff', message: 'We do not recognise this tariff. Copy it exactly as printed on the bill.' },
  SANCTIONED_LOAD_MISSING: { field: 'sanctionedLoadKw', message: 'Enter the sanctioned load in kW.' },
  SANCTIONED_LOAD_OUT_OF_RANGE: { field: 'sanctionedLoadKw', message: 'This sanctioned load is outside the range we handle. Check the kW value.' },
  MONTHLY_UNITS_MISSING: { field: 'monthlyUnits', message: 'Enter the units (kWh) used this month.' },
  MONTHLY_UNITS_OUT_OF_RANGE: { field: 'monthlyUnits', message: 'These units look too high or negative. Check the bill.' },
  UNITS_BILL_MISMATCH: { field: 'monthlyUnits', message: 'These units do not match the monthly bill amount you entered. Check both.' },
}

export interface ParsedIssues {
  fields: Partial<Record<BillFieldKey, string>>
  unknown: string[]
}

/** Message looks like "Bill confirmation validation failed: CODE_A, CODE_B". */
export function parseBillIssues(message: string): ParsedIssues {
  const out: ParsedIssues = { fields: {}, unknown: [] }
  const idx = message.indexOf(':')
  if (idx === -1) return out
  for (const code of message.slice(idx + 1).split(',').map((c) => c.trim()).filter(Boolean)) {
    const known = issues[code]
    if (known) out.fields[known.field] ??= known.message
    else out.unknown.push(code)
  }
  return out
}