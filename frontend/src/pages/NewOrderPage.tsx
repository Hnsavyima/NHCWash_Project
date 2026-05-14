import { useState, useEffect, useMemo, useCallback } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  Shirt,
  Droplets,
  Clock,
  Sparkles,
  Wind,
  Scissors,
  Check,
  ArrowLeft,
  ArrowRight,
  Calendar,
  CreditCard,
  Plus,
  Home,
  Banknote,
  CheckCircle2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { useTranslation } from "react-i18next";
import ClientLayout from "@/components/layouts/ClientLayout";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getServices } from "@/services/services";
import { getAddresses, createAddress } from "@/services/addresses";
import { getTimeSlots } from "@/services/timeslots";
import { createOrder } from "@/services/orders";
import { createCheckoutSession } from "@/services/payments";
import { bookAppointment } from "@/services/appointments";
import { serviceUnitPrice, formatAddressLine, type CheckoutPaymentMode } from "@/types";
import { ApiError } from "@/lib/api";
import { toast } from "@/components/ui/sonner";
import { tCatalog } from "@/i18n/catalog";
import type { TimeSlotDto } from "@/types";
import {
  DELIVERY_MIN_HOURS_AFTER_PICKUP_END,
  formatSlotTimeRange,
  generateMockTimeSlots,
  groupSlotsByCalendarDay,
  isDeliveryValidForPickup,
} from "@/utils/mockTimeSlots";
import { dateLocaleFromI18n } from "@/lib/dateLocaleFromI18n";

const serviceIcons = [Shirt, Droplets, Clock, Sparkles, Wind, Scissors];

function orderFlowLocale(lang: string): string {
  return dateLocaleFromI18n(lang);
}

function filterUpcomingSlots(slots: TimeSlotDto[]): TimeSlotDto[] {
  const now = Date.now();
  return slots.filter((s) => new Date(s.endAt).getTime() > now);
}

async function loadTimeSlotsWithFallback(type: "PICKUP" | "DELIVERY"): Promise<TimeSlotDto[]> {
  const rows = await getTimeSlots(type);
  // Do not fallback to mock slots for checkout: mock IDs don't exist in DB,
  // which would make appointment booking fail with "Créneau introuvable".
  return filterUpcomingSlots(rows);
}

const NewOrderPage = () => {
  const { t, i18n } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const qc = useQueryClient();
  const lang = (i18n.language || "fr").slice(0, 2);

  const [step, setStep] = useState(0);
  const [selectedItems, setSelectedItems] = useState<Record<number, number>>({});
  const [instructions, setInstructions] = useState("");
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [showNewAddress, setShowNewAddress] = useState(false);
  const [newAddress, setNewAddress] = useState({ street: "", city: "", postalCode: "", country: "Belgium" });
  const [addrError, setAddrError] = useState<string | null>(null);
  const [selectedPickupSlot, setSelectedPickupSlot] = useState<number | null>(null);
  const [selectedDeliverySlot, setSelectedDeliverySlot] = useState<number | null>(null);
  const [paying, setPaying] = useState(false);
  const [checkoutMode, setCheckoutMode] = useState<CheckoutPaymentMode>("ONLINE");
  const [onSiteSuccess, setOnSiteSuccess] = useState(false);

  const resetOrderWizard = useCallback(() => {
    setOnSiteSuccess(false);
    setStep(0);
    setSelectedItems({});
    setInstructions("");
    setSelectedPickupSlot(null);
    setSelectedDeliverySlot(null);
    setCheckoutMode("ONLINE");
  }, []);

  const { data: services = [], isLoading: loadingSvc } = useQuery({
    queryKey: ["services", lang],
    queryFn: () => getServices(lang),
  });

  useEffect(() => {
    const raw = searchParams.get("preselect") ?? searchParams.get("service");
    if (raw == null) return;
    if (loadingSvc) return;
    const id = Number.parseInt(raw, 10);
    const next = new URLSearchParams(searchParams);
    next.delete("preselect");
    next.delete("service");
    if (!Number.isFinite(id) || id < 1) {
      setSearchParams(next, { replace: true });
      return;
    }
    if (services.some((s) => s.id === id)) {
      setSelectedItems((prev) => (prev[id] ? prev : { ...prev, [id]: 1 }));
    }
    setSearchParams(next, { replace: true });
  }, [loadingSvc, services, searchParams, setSearchParams]);

  const { data: addresses = [], isLoading: loadingAddr } = useQuery({
    queryKey: ["addresses"],
    queryFn: getAddresses,
  });

  const { data: pickupSlots = [], isLoading: loadingPickup } = useQuery({
    queryKey: ["timeslots", "PICKUP"],
    queryFn: () => loadTimeSlotsWithFallback("PICKUP"),
  });

  const { data: deliverySlots = [], isLoading: loadingDelivery } = useQuery({
    queryKey: ["timeslots", "DELIVERY"],
    queryFn: () => loadTimeSlotsWithFallback("DELIVERY"),
  });

  const slotLocale = orderFlowLocale(lang);

  const pickupDayGroups = useMemo(
    () => groupSlotsByCalendarDay(pickupSlots, slotLocale),
    [pickupSlots, slotLocale]
  );

  const deliveryDayGroups = useMemo(
    () => groupSlotsByCalendarDay(deliverySlots, slotLocale),
    [deliverySlots, slotLocale]
  );

  const selectedPickup = useMemo(
    () => pickupSlots.find((s) => s.id === selectedPickupSlot) ?? null,
    [pickupSlots, selectedPickupSlot]
  );

  useEffect(() => {
    if (selectedPickupSlot == null || selectedDeliverySlot == null) return;
    const del = deliverySlots.find((s) => s.id === selectedDeliverySlot);
    const pu = pickupSlots.find((s) => s.id === selectedPickupSlot);
    if (pu && del && !isDeliveryValidForPickup(pu, del)) {
      setSelectedDeliverySlot(null);
    }
  }, [selectedPickupSlot, selectedDeliverySlot, pickupSlots, deliverySlots]);

  useEffect(() => {
    setSelectedAddressId((prev) => {
      if (prev != null) return prev;
      const def = addresses.find((a) => a.defaultAddress) ?? addresses[0];
      return def ? def.id : null;
    });
  }, [addresses]);

  const steps = [t("newOrder.stepServices"), t("newOrder.stepDetails"), t("newOrder.stepAddress"), t("newOrder.stepSchedule"), t("newOrder.stepPayment")];

  const orderItems = Object.entries(selectedItems).map(([id, qty]) => {
    const svc = services.find((s) => s.id === Number(id));
    const unit = svc ? serviceUnitPrice(svc) : 0;
    return { serviceId: Number(id), quantity: qty, line: unit * qty };
  });

  const total = orderItems.reduce((sum, item) => sum + item.line, 0);

  const toggleService = (id: number) => {
    setSelectedItems((prev) => {
      const next = { ...prev };
      if (next[id]) delete next[id];
      else next[id] = 1;
      return next;
    });
  };

  const updateQty = (id: number, qty: number) => {
    if (qty < 1) return;
    setSelectedItems((prev) => ({ ...prev, [id]: qty }));
  };

  const canNext = () => {
    if (step === 0) return Object.keys(selectedItems).length > 0;
    if (step === 1) return true;
    if (step === 2) return selectedAddressId !== null || showNewAddress;
    if (step === 3) {
      if (selectedPickupSlot === null || selectedDeliverySlot === null) return false;
      const pu = pickupSlots.find((s) => s.id === selectedPickupSlot);
      const de = deliverySlots.find((s) => s.id === selectedDeliverySlot);
      if (!pu || !de) return false;
      return isDeliveryValidForPickup(pu, de);
    }
    return true;
  };

  const goNext = async () => {
    setAddrError(null);
    if (step === 2 && showNewAddress) {
      if (!newAddress.street.trim() || !newAddress.city.trim() || !newAddress.postalCode.trim() || !newAddress.country.trim()) {
        setAddrError(t("newOrder.addressRequired", { defaultValue: "Remplissez tous les champs obligatoires." }));
        return;
      }
      try {
        const addr = await createAddress({
          street: newAddress.street.trim(),
          city: newAddress.city.trim(),
          postalCode: newAddress.postalCode.trim(),
          country: newAddress.country.trim(),
          defaultAddress: addresses.length === 0,
        });
        setSelectedAddressId(addr.id);
        setShowNewAddress(false);
        await qc.invalidateQueries({ queryKey: ["addresses"] });
        setStep(3);
        return;
      } catch (e) {
        setAddrError(e instanceof ApiError ? e.message : t("auth.errorGeneric"));
        return;
      }
    }
    if (step === 2 && !showNewAddress && selectedAddressId === null) {
      setAddrError(t("newOrder.pickAddress", { defaultValue: "Choisissez une adresse." }));
      return;
    }
    if (!canNext()) return;
    setStep((s) => s + 1);
  };

  const handlePay = async () => {
    if (!selectedAddressId || !selectedPickupSlot || !selectedDeliverySlot) return;
    setPaying(true);
    try {
      const items = Object.entries(selectedItems).map(([id, qty]) => ({
        serviceId: Number(id),
        quantity: qty,
      }));
      const order = await createOrder({
        items,
        instructions: instructions.trim() || undefined,
        checkoutPaymentMode: checkoutMode,
      });
      await bookAppointment({
        orderId: Number.parseInt(String(order.id), 10),
        timeSlotId: Number(selectedPickupSlot),
        addressId: Number.parseInt(String(selectedAddressId), 10),
        type: "PICKUP",
      });
      await bookAppointment({
        orderId: Number.parseInt(String(order.id), 10),
        timeSlotId: Number(selectedDeliverySlot),
        addressId: Number.parseInt(String(selectedAddressId), 10),
        type: "DELIVERY",
      });
      await qc.invalidateQueries({ queryKey: ["orders"] });
      await qc.invalidateQueries({ queryKey: ["staff-orders"] });
      await qc.invalidateQueries({ queryKey: ["order", order.id] });

      if (checkoutMode === "CASH_ON_SITE") {
        setSelectedItems({});
        setInstructions("");
        setSelectedPickupSlot(null);
        setSelectedDeliverySlot(null);
        setStep(0);
        setCheckoutMode("ONLINE");
        setOnSiteSuccess(true);
        return;
      }

      const paymentResponse = await createCheckoutSession(order.id);
      const url = paymentResponse.url;
      if (!url || typeof url !== "string") {
        throw new Error("No checkout URL returned");
      }
      window.location.href = url;
    } catch (e) {
      console.error(e);
      toast.error(e instanceof ApiError ? e.message : t("auth.errorGeneric"));
    } finally {
      setPaying(false);
    }
  };

  const selectedAddr = addresses.find((a) => a.id === selectedAddressId);
  const pickupSlot = selectedPickup;
  const deliverySlot = deliverySlots.find((s) => s.id === selectedDeliverySlot);

  const formatSlotSummaryLine = (slot: TimeSlotDto) =>
    `${new Date(slot.startAt).toLocaleDateString(slotLocale, { weekday: "short", day: "numeric", month: "short" })} · ${formatSlotTimeRange(slot.startAt, slot.endAt, slotLocale)}`;

  const loadingStep0 = loadingSvc || loadingAddr;

  if (onSiteSuccess) {
    return (
      <ClientLayout>
        <div className="mx-auto max-w-lg space-y-6 px-2">
          <div className="rounded-xl border border-border bg-card p-8 text-center shadow-card">
            <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-success/15 text-success">
              <CheckCircle2 className="h-8 w-8" aria-hidden />
            </div>
            <h1 className="font-display text-xl font-bold text-foreground">{t("newOrder.onSiteSuccessTitle")}</h1>
            <p className="mt-3 text-sm leading-relaxed text-muted-foreground">{t("newOrder.onSiteSuccessBody")}</p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <Button asChild className="gradient-hero border-0 text-primary-foreground">
                <Link to="/dashboard/orders">{t("newOrder.viewOrders")}</Link>
              </Button>
              <Button type="button" variant="outline" onClick={resetOrderWizard}>
                {t("newOrder.newOrderAgain")}
              </Button>
            </div>
          </div>
        </div>
      </ClientLayout>
    );
  }

  return (
    <ClientLayout>
      <div className="mx-auto max-w-2xl space-y-6">
        <div className="flex items-center justify-between">
          {steps.map((s, i) => (
            <div key={s} className="flex items-center gap-2">
              <div
                className={`flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold transition-colors ${
                  i <= step ? "gradient-hero text-primary-foreground" : "bg-muted text-muted-foreground"
                }`}
              >
                {i < step ? <Check className="h-4 w-4" /> : i + 1}
              </div>
              <span className={`hidden text-sm font-medium sm:block ${i <= step ? "text-foreground" : "text-muted-foreground"}`}>
                {s}
              </span>
              {i < steps.length - 1 && (
                <div className={`mx-2 hidden h-px w-8 sm:block ${i < step ? "bg-primary" : "bg-border"}`} />
              )}
            </div>
          ))}
        </div>

        {loadingStep0 && step === 0 && (
          <p className="text-center text-sm text-muted-foreground">…</p>
        )}

        <div className="rounded-xl border border-border bg-card p-6 shadow-card">
          {step === 0 && (
            <div className="space-y-4">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("newOrder.chooseServices")}</h2>
              <div className="grid gap-3 sm:grid-cols-2">
                {services.map((svc, idx) => {
                  const isSelected = svc.id in selectedItems;
                  const Icon = serviceIcons[idx % serviceIcons.length];
                  const unit = serviceUnitPrice(svc);
                  return (
                    <button
                      key={svc.id}
                      type="button"
                      onClick={() => toggleService(svc.id)}
                      className={`flex items-center gap-4 rounded-xl border-2 p-4 text-left transition-all ${
                        isSelected ? "border-primary bg-primary/5" : "border-border hover:border-primary/30"
                      }`}
                    >
                      <div className={`flex h-10 w-10 items-center justify-center rounded-lg ${isSelected ? "bg-primary/10" : "bg-muted"}`}>
                        <Icon className={`h-5 w-5 ${isSelected ? "text-primary" : "text-muted-foreground"}`} />
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-medium text-foreground">{tCatalog(t, svc.name)}</p>
                        <p className="text-xs text-muted-foreground">{unit.toFixed(2)}€ / pièce</p>
                      </div>
                      {isSelected && (
                        <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary">
                          <Check className="h-3 w-3 text-primary-foreground" />
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {step === 1 && (
            <div className="space-y-4">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("newOrder.quantityInstructions")}</h2>
              {Object.entries(selectedItems).map(([idStr, qty]) => {
                const id = Number(idStr);
                const svc = services.find((s) => s.id === id);
                const Icon = serviceIcons[id % serviceIcons.length];
                const unit = svc ? serviceUnitPrice(svc) : 0;
                return (
                  <div key={id} className="flex items-center justify-between rounded-lg border border-border p-4">
                    <div className="flex items-center gap-3">
                      <Icon className="h-5 w-5 text-primary" />
                      <div>
                        <span className="text-sm font-medium text-foreground">
                          {svc?.name != null ? tCatalog(t, svc.name) : `#${id}`}
                        </span>
                        <p className="text-xs text-muted-foreground">{(unit * qty).toFixed(2)}€</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button variant="outline" size="sm" type="button" onClick={() => updateQty(id, qty - 1)}>
                        -
                      </Button>
                      <span className="w-8 text-center text-sm font-semibold text-foreground">{qty}</span>
                      <Button variant="outline" size="sm" type="button" onClick={() => updateQty(id, qty + 1)}>
                        +
                      </Button>
                    </div>
                  </div>
                );
              })}
              <div className="space-y-2">
                <Label>{t("newOrder.specialInstructions")}</Label>
                <Textarea
                  placeholder={t("newOrder.instructionsPlaceholder")}
                  value={instructions}
                  onChange={(e) => setInstructions(e.target.value)}
                />
              </div>
            </div>
          )}

          {step === 2 && (
            <div className="space-y-4">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("newOrder.addressTitle")}</h2>
              {addrError && (
                <p className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive">{addrError}</p>
              )}
              <div className="space-y-3">
                {addresses.map((addr) => (
                  <button
                    key={addr.id}
                    type="button"
                    onClick={() => {
                      setSelectedAddressId(addr.id);
                      setShowNewAddress(false);
                    }}
                    className={`flex w-full items-center gap-4 rounded-xl border-2 p-4 text-left transition-all ${
                      selectedAddressId === addr.id && !showNewAddress ? "border-primary bg-primary/5" : "border-border hover:border-primary/30"
                    }`}
                  >
                    <div
                      className={`flex h-10 w-10 items-center justify-center rounded-lg ${
                        selectedAddressId === addr.id && !showNewAddress ? "bg-primary/10" : "bg-muted"
                      }`}
                    >
                      <Home className={`h-5 w-5 ${selectedAddressId === addr.id && !showNewAddress ? "text-primary" : "text-muted-foreground"}`} />
                    </div>
                    <div className="flex-1">
                      <p className="text-sm font-medium text-foreground">{addr.street}</p>
                      <p className="text-xs text-muted-foreground">{formatAddressLine(addr)}</p>
                    </div>
                    {addr.defaultAddress && (
                      <span className="rounded-full bg-accent/10 px-2 py-0.5 text-xs font-medium text-accent">{t("newOrder.default")}</span>
                    )}
                    {selectedAddressId === addr.id && !showNewAddress && (
                      <div className="flex h-6 w-6 items-center justify-center rounded-full bg-primary">
                        <Check className="h-3 w-3 text-primary-foreground" />
                      </div>
                    )}
                  </button>
                ))}
              </div>

              <button
                type="button"
                onClick={() => setShowNewAddress(!showNewAddress)}
                className="flex items-center gap-2 text-sm font-medium text-primary hover:underline"
              >
                <Plus className="h-4 w-4" /> {t("newOrder.addAddress")}
              </button>

              {showNewAddress && (
                <div className="space-y-3 rounded-lg border border-border p-4">
                  <div className="space-y-2">
                    <Label>{t("newOrder.street")}</Label>
                    <Input
                      placeholder={t("newOrder.streetPlaceholder")}
                      value={newAddress.street}
                      onChange={(e) => setNewAddress((p) => ({ ...p, street: e.target.value }))}
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-2">
                      <Label>{t("newOrder.city")}</Label>
                      <Input
                        placeholder={t("newOrder.cityPlaceholder")}
                        value={newAddress.city}
                        onChange={(e) => setNewAddress((p) => ({ ...p, city: e.target.value }))}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>{t("newOrder.zipCode")}</Label>
                      <Input
                        placeholder={t("newOrder.zipCodePlaceholder")}
                        value={newAddress.postalCode}
                        onChange={(e) => setNewAddress((p) => ({ ...p, postalCode: e.target.value }))}
                      />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label>{t("profile.country", { defaultValue: "Pays" })}</Label>
                    <Input value={newAddress.country} onChange={(e) => setNewAddress((p) => ({ ...p, country: e.target.value }))} />
                  </div>
                </div>
              )}
            </div>
          )}

          {step === 3 && (
            <div className="space-y-6">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("newOrder.scheduleTitle")}</h2>

              {(loadingPickup || loadingDelivery) && (
                <p className="text-sm text-muted-foreground">…</p>
              )}

              <div className="space-y-4">
                <Label className="text-base font-semibold">{t("newOrder.pickupSlot")}</Label>
                {pickupSlots.length === 0 && !loadingPickup && (
                  <p className="text-sm text-muted-foreground">{t("newOrder.noSlots")}</p>
                )}
                <div className="space-y-5">
                  {pickupDayGroups.map((group) => (
                    <div key={group.dateKey} className="space-y-2">
                      <p className="text-sm font-medium text-foreground">{group.label}</p>
                      <div className="flex flex-wrap gap-2">
                        {group.slots.map((slot) => {
                          const selected = selectedPickupSlot === slot.id;
                          return (
                            <button
                              key={slot.id}
                              type="button"
                              onClick={() => {
                                setSelectedPickupSlot(slot.id);
                                setSelectedDeliverySlot(null);
                              }}
                              className={`inline-flex items-center gap-2 rounded-lg border-2 px-3 py-2 text-sm font-medium transition-all ${
                                selected
                                  ? "border-primary bg-primary/10 text-primary shadow-sm"
                                  : "border-border text-foreground hover:border-primary/40"
                              }`}
                            >
                              <Calendar className="h-4 w-4 shrink-0 opacity-70" />
                              {formatSlotTimeRange(slot.startAt, slot.endAt, slotLocale)}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="space-y-4 border-t border-border pt-6">
                <div>
                  <Label className="text-base font-semibold">{t("newOrder.deliverySlot")}</Label>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("newOrder.deliveryRuleHint", { hours: DELIVERY_MIN_HOURS_AFTER_PICKUP_END })}
                  </p>
                </div>
                {!selectedPickup && (
                  <p className="text-sm text-muted-foreground">{t("newOrder.selectPickupFirst")}</p>
                )}
                {deliverySlots.length === 0 && !loadingDelivery && (
                  <p className="text-sm text-muted-foreground">{t("newOrder.noSlots")}</p>
                )}
                <div className="space-y-5">
                  {deliveryDayGroups.map((group) => (
                    <div key={group.dateKey} className="space-y-2">
                      <p className="text-sm font-medium text-foreground">{group.label}</p>
                      <div className="flex flex-wrap gap-2">
                        {group.slots.map((slot) => {
                          const allowed = selectedPickup ? isDeliveryValidForPickup(selectedPickup, slot) : false;
                          const selected = selectedDeliverySlot === slot.id;
                          return (
                            <button
                              key={slot.id}
                              type="button"
                              disabled={!allowed}
                              title={!allowed ? t("newOrder.slotTooSoon", { hours: DELIVERY_MIN_HOURS_AFTER_PICKUP_END }) : undefined}
                              onClick={() => {
                                if (allowed) setSelectedDeliverySlot(slot.id);
                              }}
                              className={`inline-flex items-center gap-2 rounded-lg border-2 px-3 py-2 text-sm font-medium transition-all ${
                                !allowed
                                  ? "cursor-not-allowed border-border/60 bg-muted/40 text-muted-foreground opacity-60"
                                  : selected
                                    ? "border-primary bg-primary/10 text-primary shadow-sm"
                                    : "border-border text-foreground hover:border-primary/40"
                              }`}
                            >
                              <Calendar className="h-4 w-4 shrink-0 opacity-70" />
                              {formatSlotTimeRange(slot.startAt, slot.endAt, slotLocale)}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="space-y-4">
              <h2 className="font-display text-lg font-semibold text-foreground">{t("newOrder.summaryTitle")}</h2>
              <div className="space-y-3">
                <Label className="text-base font-semibold">{t("newOrder.payMethodTitle")}</Label>
                <RadioGroup
                  value={checkoutMode}
                  onValueChange={(v) => setCheckoutMode(v as CheckoutPaymentMode)}
                  className="grid gap-3 md:grid-cols-2"
                >
                  <label
                    htmlFor="checkout-online"
                    className={`flex cursor-pointer flex-col gap-2 rounded-xl border-2 p-4 transition-all ${
                      checkoutMode === "ONLINE" ? "border-primary bg-primary/5 shadow-sm" : "border-border hover:border-primary/40"
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <RadioGroupItem value="ONLINE" id="checkout-online" className="mt-1" />
                      <CreditCard className="h-6 w-6 shrink-0 text-primary" aria-hidden />
                      <div className="min-w-0 flex-1 text-left">
                        <p className="text-sm font-semibold text-foreground">{t("newOrder.payOnlineTitle")}</p>
                        <p className="mt-1 text-xs text-muted-foreground">{t("newOrder.payOnlineDesc")}</p>
                      </div>
                    </div>
                  </label>
                  <label
                    htmlFor="checkout-onsite"
                    className={`flex cursor-pointer flex-col gap-2 rounded-xl border-2 p-4 transition-all ${
                      checkoutMode === "CASH_ON_SITE" ? "border-primary bg-primary/5 shadow-sm" : "border-border hover:border-primary/40"
                    }`}
                  >
                    <div className="flex items-start gap-3">
                      <RadioGroupItem value="CASH_ON_SITE" id="checkout-onsite" className="mt-1" />
                      <Banknote className="h-6 w-6 shrink-0 text-primary" aria-hidden />
                      <div className="min-w-0 flex-1 text-left">
                        <p className="text-sm font-semibold text-foreground">{t("newOrder.payOnSiteTitle")}</p>
                        <p className="mt-1 text-xs text-muted-foreground">{t("newOrder.payOnSiteDesc")}</p>
                      </div>
                    </div>
                  </label>
                </RadioGroup>
              </div>
              <div className="space-y-3 rounded-lg bg-muted/50 p-4">
                {orderItems.map((item) => {
                  const svc = services.find((s) => s.id === item.serviceId);
                  return (
                    <div key={item.serviceId} className="flex items-center justify-between text-sm">
                      <span className="text-foreground">
                        {svc?.name != null ? tCatalog(t, svc.name) : `#${item.serviceId}`} × {item.quantity}
                      </span>
                      <span className="font-medium text-foreground">{item.line.toFixed(2)}€</span>
                    </div>
                  );
                })}
                <div className="flex items-center justify-between border-t border-border pt-3 font-display font-bold text-foreground">
                  <span>{t("newOrder.estimatedTotal")}</span>
                  <span>{total.toFixed(2)}€</span>
                </div>
              </div>

              <div className="space-y-2 rounded-lg border border-border p-4 text-sm">
                {selectedAddr && (
                  <p className="text-muted-foreground">
                    <strong className="text-foreground">{t("newOrder.address")} :</strong> {formatAddressLine(selectedAddr)}
                  </p>
                )}
                {pickupSlot && (
                  <p className="text-muted-foreground">
                    <strong className="text-foreground">{t("newOrder.pickup")} :</strong> {formatSlotSummaryLine(pickupSlot)}
                  </p>
                )}
                {deliverySlot && (
                  <p className="text-muted-foreground">
                    <strong className="text-foreground">{t("newOrder.delivery")} :</strong> {formatSlotSummaryLine(deliverySlot)}
                  </p>
                )}
                {instructions && (
                  <p className="text-muted-foreground">
                    <strong className="text-foreground">{t("newOrder.instructions")} :</strong> {instructions}
                  </p>
                )}
              </div>

              <Button
                type="button"
                className="w-full gap-2 gradient-hero border-0 text-primary-foreground hover:opacity-90"
                size="lg"
                disabled={paying}
                onClick={() => void handlePay()}
              >
                {checkoutMode === "ONLINE" ? (
                  <>
                    <CreditCard className="h-4 w-4" aria-hidden />
                    {paying ? "…" : `${t("newOrder.payOnlineBtn")} · ${total.toFixed(2)} €`}
                  </>
                ) : (
                  <>
                    <Banknote className="h-4 w-4" aria-hidden />
                    {paying ? "…" : t("newOrder.confirmOnSiteBtn")}
                  </>
                )}
              </Button>
            </div>
          )}
        </div>

        <div className="flex justify-between">
          <Button variant="outline" disabled={step === 0} onClick={() => setStep(step - 1)} className="gap-2">
            <ArrowLeft className="h-4 w-4" /> {t("newOrder.back")}
          </Button>
          {step < 4 && (
            <Button type="button" onClick={() => void goNext()} disabled={!canNext()} className="gap-2">
              {t("newOrder.next")} <ArrowRight className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>
    </ClientLayout>
  );
};

export default NewOrderPage;
