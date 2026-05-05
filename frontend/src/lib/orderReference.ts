/**
 * Extracts CMD-123 from scanned text (QR may include only the reference or surrounding noise).
 */
export function extractOrderReferenceFromScan(raw: string): string | null {
  const t = raw.trim();
  const m = t.match(/CMD-(\d+)/i);
  if (m) {
    return `CMD-${m[1]}`;
  }
  if (/^\d+$/.test(t)) {
    return t;
  }
  return null;
}
