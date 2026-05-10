import type { TimeSlotDto } from "@/types";

/** Minimum delay between end of pickup window and start of delivery window (hours). */
export const DELIVERY_MIN_HOURS_AFTER_PICKUP_END = 48;

/** Two-hour windows during standard business hours (lunch gap 13:00–14:00). */
const SLOT_WINDOWS: readonly { startH: number; startM: number; endH: number; endM: number }[] = [
  { startH: 9, startM: 0, endH: 11, endM: 0 },
  { startH: 11, startM: 0, endH: 13, endM: 0 },
  { startH: 14, startM: 0, endH: 16, endM: 0 },
  { startH: 16, startM: 0, endH: 18, endM: 0 },
];

function isWeekend(d: Date): boolean {
  const day = d.getDay();
  return day === 0 || day === 6;
}

function startOfLocalDay(d: Date): Date {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}

/**
 * Generates mock pickup or delivery slots on upcoming weekdays (Mon–Fri),
 * starting `startAfterDays` after today (default: tomorrow), for `businessDayCount` working days.
 */
export function generateMockTimeSlots(
  slotType: "PICKUP" | "DELIVERY",
  options?: { businessDayCount?: number; startAfterDays?: number }
): TimeSlotDto[] {
  const businessDayCount = options?.businessDayCount ?? 10;
  const startAfterDays = options?.startAfterDays ?? 1;
  const idBase = slotType === "PICKUP" ? 9_000_000 : 9_100_000;

  const slots: TimeSlotDto[] = [];
  const day = startOfLocalDay(new Date());
  day.setDate(day.getDate() + startAfterDays);

  let businessDays = 0;
  let n = 0;
  while (businessDays < businessDayCount) {
    if (!isWeekend(day)) {
      for (const w of SLOT_WINDOWS) {
        const start = new Date(day);
        start.setHours(w.startH, w.startM, 0, 0);
        const end = new Date(day);
        end.setHours(w.endH, w.endM, 0, 0);
        n += 1;
        slots.push({
          id: idBase + n,
          slotType,
          startAt: start.toISOString(),
          endAt: end.toISOString(),
          capacityMax: 8,
          remainingCapacity: 8,
          active: true,
        });
      }
      businessDays += 1;
    }
    day.setDate(day.getDate() + 1);
  }

  return slots;
}

export function isDeliveryValidForPickup(pickup: TimeSlotDto, delivery: TimeSlotDto): boolean {
  const pickupEnd = new Date(pickup.endAt).getTime();
  const minDeliveryStart = pickupEnd + DELIVERY_MIN_HOURS_AFTER_PICKUP_END * 60 * 60 * 1000;
  return new Date(delivery.startAt).getTime() >= minDeliveryStart;
}

export type DaySlotGroup = { dateKey: string; label: string; slots: TimeSlotDto[] };

export function groupSlotsByCalendarDay(slots: TimeSlotDto[], locale: string): DaySlotGroup[] {
  const map = new Map<string, TimeSlotDto[]>();
  for (const s of slots) {
    const d = new Date(s.startAt);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(s);
  }
  const formatter = new Intl.DateTimeFormat(locale, {
    weekday: "long",
    day: "numeric",
    month: "long",
  });
  return [...map.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([dateKey, daySlots]) => {
      const first = daySlots[0];
      const label = formatter.format(new Date(first.startAt));
      return {
        dateKey,
        label: label.charAt(0).toUpperCase() + label.slice(1),
        slots: daySlots.sort((x, y) => new Date(x.startAt).getTime() - new Date(y.startAt).getTime()),
      };
    });
}

export function formatSlotTimeRange(isoStart: string, isoEnd: string, locale: string): string {
  const opt: Intl.DateTimeFormatOptions = { hour: "2-digit", minute: "2-digit" };
  const a = new Date(isoStart).toLocaleTimeString(locale, opt);
  const b = new Date(isoEnd).toLocaleTimeString(locale, opt);
  return `${a} – ${b}`;
}
