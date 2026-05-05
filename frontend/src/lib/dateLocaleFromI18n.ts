/** BCP 47 locale for {@link Intl.DateTimeFormat} from an i18n language tag (e.g. `de`, `fr-BE`). */
export function dateLocaleFromI18n(lang?: string | null): string {
  const base = (lang ?? "fr").split("-")[0]?.toLowerCase() ?? "fr";
  switch (base) {
    case "nl":
      return "nl-BE";
    case "en":
      return "en-GB";
    case "de":
      return "de-DE";
    default:
      return "fr-BE";
  }
}
