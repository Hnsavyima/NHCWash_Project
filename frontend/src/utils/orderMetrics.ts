/** Compare calendar day using `YYYY-MM-DD` prefix when present (avoids TZ drift on naive LocalDateTime strings). */
export function isSameLocalCalendarDay(iso: string | null, ref: Date = new Date()): boolean {
  if (!iso) return false;
  const datePart = iso.slice(0, 10);
  if (/^\d{4}-\d{2}-\d{2}$/.test(datePart)) {
    const y = ref.getFullYear();
    const m = String(ref.getMonth() + 1).padStart(2, "0");
    const day = String(ref.getDate()).padStart(2, "0");
    return datePart === `${y}-${m}-${day}`;
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return false;
  return (
    d.getFullYear() === ref.getFullYear() &&
    d.getMonth() === ref.getMonth() &&
    d.getDate() === ref.getDate()
  );
}

export function isInSameLocalMonth(iso: string | null, ref: Date = new Date()): boolean {
  if (!iso) return false;
  const monthPart = iso.slice(0, 7);
  if (/^\d{4}-\d{2}$/.test(monthPart)) {
    const y = ref.getFullYear();
    const m = String(ref.getMonth() + 1).padStart(2, "0");
    return monthPart === `${y}-${m}`;
  }
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return false;
  return d.getFullYear() === ref.getFullYear() && d.getMonth() === ref.getMonth();
}
