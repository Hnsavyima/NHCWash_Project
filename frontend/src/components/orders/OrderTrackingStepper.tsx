import { Fragment } from "react";
import { useTranslation } from "react-i18next";
import {
  CheckCircle,
  Clock,
  Loader,
  Package,
  Truck,
  type LucideIcon,
} from "lucide-react";
import type { OrderDto } from "@/types";

export type OrderTrackingStepperProps = {
  order: Pick<OrderDto, "status">;
};

const FULFILLMENT_STAGE_ICONS: Record<string, LucideIcon> = {
  PENDING: Clock,
  RECEIVED: Package,
  PROCESSING: Loader,
  READY: CheckCircle,
  DELIVERED: Truck,
};

/**
 * Physical fulfillment timeline only (laundry journey).
 * Single source for the dashboard order detail tracking UI.
 */
export function OrderTrackingStepper({ order }: OrderTrackingStepperProps) {
  const { t } = useTranslation();

  const fulfillmentStages = ["PENDING", "RECEIVED", "PROCESSING", "READY", "DELIVERED"];

  const currentStatus = (order.status || "PENDING").toUpperCase();

  const mappedLogisticsStatus = currentStatus === "PAID" ? "PENDING" : currentStatus;
  const rawIndex = fulfillmentStages.indexOf(mappedLogisticsStatus);
  const activeIndex = rawIndex >= 0 ? rawIndex : 0;

  const trackingSteps = fulfillmentStages.map((stage, i) => ({
    key: stage,
    label: t(`status.${stage}`),
    icon: FULFILLMENT_STAGE_ICONS[stage] ?? Clock,
    isCompleted: i < activeIndex,
    isCurrent: i === activeIndex,
  }));

  return (
    <div className="flex w-full flex-col gap-2">
      <div className="flex w-full items-center">
        {trackingSteps.map((step, i) => {
          const StepIcon = step.icon;
          const connectorActive = i < trackingSteps.length - 1 && i + 1 <= activeIndex;
          return (
            <Fragment key={`order-tracking-${step.key}-icon`}>
              <div className="flex min-w-0 flex-1 justify-center">
                <div
                  className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full transition-colors ${
                    step.isCurrent
                      ? "gradient-hero text-primary-foreground"
                      : step.isCompleted
                        ? "bg-primary/10 text-primary"
                        : "bg-muted text-muted-foreground"
                  }`}
                >
                  <StepIcon className="h-5 w-5" />
                </div>
              </div>
              {i < trackingSteps.length - 1 ? (
                <div
                  role="presentation"
                  className={`h-0.5 min-w-[8px] flex-1 ${connectorActive ? "bg-primary" : "bg-border"}`}
                  aria-hidden
                />
              ) : null}
            </Fragment>
          );
        })}
      </div>
      <div className="flex w-full">
        {trackingSteps.map((step, i) => {
          const reached = step.isCompleted || step.isCurrent;
          return (
            <Fragment key={`order-tracking-${step.key}-label`}>
              <div
                className={`min-w-0 flex-1 px-0.5 text-center text-xs font-medium ${
                  reached ? "text-foreground" : "text-muted-foreground"
                }`}
              >
                {step.label}
              </div>
              {i < trackingSteps.length - 1 ? <div className="min-w-[8px] flex-1" aria-hidden /> : null}
            </Fragment>
          );
        })}
      </div>
    </div>
  );
}
