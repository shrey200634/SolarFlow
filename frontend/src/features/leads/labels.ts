import type { LeadStatus, Ownership, RoofType } from '../../types/api'

export const roofTypeLabel: Record<RoofType, string> = {
  RCC_FLAT: 'RCC flat roof',
  TIN_SHEET: 'Tin sheet roof',
  TILED: 'Tiled roof',
  ASBESTOS: 'Asbestos sheet roof',
  OTHER: 'Other roof type',
}

export const ownershipLabel: Record<Ownership, string> = {
  OWNED: 'Owned',
  RENTED: 'Rented',
  LEASED: 'Leased',
}

type Tone = 'neutral' | 'success' | 'warning' | 'danger' | 'info'

/**
 * What a homeowner sees for each status. Plain language only: no scores,
 * no internal stage names, and a low score is never presented as a rejection.
 */
export const homeownerStatus: Record<LeadStatus, { label: string; hint: string; tone: Tone }> = {
  NEW: { label: 'Bill needed', hint: 'Upload your electricity bill to continue.', tone: 'warning' },
  BILL_UPLOADED: { label: 'Check bill details', hint: 'Confirm the details we read from your bill.', tone: 'warning' },
  BILL_CONFIRMED: { label: 'Under review', hint: 'We are reviewing your enquiry.', tone: 'info' },
  QUALIFIED: { label: 'Under review', hint: 'We are reviewing your enquiry.', tone: 'info' },
  SALES_CALL_PENDING: { label: 'We will call you', hint: 'Our team will call you shortly to discuss your options.', tone: 'info' },
  SURVEY_RECOMMENDED: { label: 'Site visit planned', hint: 'We recommend a short visit to your roof.', tone: 'success' },
  QUOTED: { label: 'Quotation ready', hint: 'Your quotation is ready to view.', tone: 'success' },
  ACCEPTED: { label: 'Accepted', hint: 'Thank you. We are planning your installation.', tone: 'success' },
  LOST: { label: 'Closed', hint: 'This enquiry is closed.', tone: 'neutral' },
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}
export function formatINR(value: number | string): string {
  return `₹${Number(value).toLocaleString('en-IN')}`
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * The link shown on each lead card. While the bill is still needed it goes to the bill page,
 * after that it opens the lead detail page (status, bill link and quotation).
 */
export function homeownerAction(status: LeadStatus, leadId: string): { to: string; label: string } {
  if (status === 'NEW') return { to: `/app/${leadId}/bill`, label: 'Upload bill' }
  if (status === 'BILL_UPLOADED') return { to: `/app/${leadId}/bill`, label: 'Review bill' }
  if (status === 'QUOTED') return { to: `/app/${leadId}`, label: 'View quotation' }
  return { to: `/app/${leadId}`, label: 'View details' }
}