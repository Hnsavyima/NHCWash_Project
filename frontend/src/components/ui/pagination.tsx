import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

export type PaginationProps = {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
};

type PageEntry = number | "ellipsis";

function buildPageEntries(currentPage: number, totalPages: number): PageEntry[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  const entries = new Set<number>();
  entries.add(1);
  entries.add(totalPages);
  for (let p = currentPage - 1; p <= currentPage + 1; p++) {
    if (p >= 1 && p <= totalPages) entries.add(p);
  }

  const sorted = [...entries].sort((a, b) => a - b);
  const out: PageEntry[] = [];
  for (let i = 0; i < sorted.length; i++) {
    const n = sorted[i];
    if (i > 0) {
      const prev = sorted[i - 1];
      if (n - prev > 1) out.push("ellipsis");
    }
    out.push(n);
  }
  return out;
}

/**
 * Premium SaaS-style pagination bar (client-side lists).
 */
export function Pagination({ currentPage, totalPages, onPageChange, className }: PaginationProps) {
  if (totalPages < 1) return null;
  if (totalPages === 1) return null;

  const pages = buildPageEntries(currentPage, totalPages);
  const isFirst = currentPage <= 1;
  const isLast = currentPage >= totalPages;

  return (
    <nav
      role="navigation"
      aria-label="Pagination"
      className={cn(
        "flex flex-col items-stretch gap-3 border-t border-border bg-card/50 px-4 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-6",
        className,
      )}
    >
      <div className="flex items-center justify-center gap-1 sm:justify-start">
        <button
          type="button"
          disabled={isFirst}
          onClick={() => onPageChange(currentPage - 1)}
          className={cn(
            "inline-flex h-9 items-center gap-1.5 rounded-lg border border-border bg-background px-3 text-sm font-medium text-foreground shadow-sm transition-colors",
            isFirst ? "cursor-not-allowed opacity-50" : "hover:bg-muted/80",
          )}
        >
          <ChevronLeft className="h-4 w-4 shrink-0" aria-hidden />
          Previous
        </button>
        <button
          type="button"
          disabled={isLast}
          onClick={() => onPageChange(currentPage + 1)}
          className={cn(
            "inline-flex h-9 items-center gap-1.5 rounded-lg border border-border bg-background px-3 text-sm font-medium text-foreground shadow-sm transition-colors",
            isLast ? "cursor-not-allowed opacity-50" : "hover:bg-muted/80",
          )}
        >
          Next
          <ChevronRight className="h-4 w-4 shrink-0" aria-hidden />
        </button>
      </div>

      <ul className="flex flex-wrap items-center justify-center gap-1.5 sm:justify-end">
        {pages.map((entry, idx) =>
          entry === "ellipsis" ? (
            <li key={`e-${idx}`} className="flex h-9 w-9 items-center justify-center text-sm text-muted-foreground">
              …
            </li>
          ) : (
            <li key={entry}>
              <button
                type="button"
                onClick={() => onPageChange(entry)}
                aria-current={entry === currentPage ? "page" : undefined}
                className={cn(
                  "flex h-9 min-w-[2.25rem] items-center justify-center rounded-lg px-2.5 text-sm font-semibold tabular-nums transition-colors",
                  entry === currentPage
                    ? "bg-blue-600 text-white shadow-md dark:bg-blue-600"
                    : "text-gray-600 hover:bg-gray-100 dark:text-gray-300 dark:hover:bg-gray-800",
                )}
              >
                {entry}
              </button>
            </li>
          ),
        )}
      </ul>
    </nav>
  );
}
