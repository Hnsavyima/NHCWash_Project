import { useQuery } from "@tanstack/react-query";
import { getStaffOrders } from "@/services/orders";

const STAFF_POLL_MS = 30_000;

export function useStaffOrders() {
  return useQuery({
    queryKey: ["staff-orders"],
    queryFn: getStaffOrders,
    refetchInterval: STAFF_POLL_MS,
    refetchIntervalInBackground: true,
  });
}
