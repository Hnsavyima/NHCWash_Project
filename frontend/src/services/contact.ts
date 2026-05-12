import { apiRequest } from "@/lib/api";

export type ContactFormData = {
  name: string;
  email: string;
  subject: string;
  message: string;
};

export async function submitContactForm(data: ContactFormData): Promise<void> {
  await apiRequest<void>("/contact", {
    method: "POST",
    body: JSON.stringify({
      name: data.name.trim(),
      email: data.email.trim(),
      subject: data.subject.trim(),
      message: data.message.trim(),
    }),
    skipAuth: true,
  });
}
