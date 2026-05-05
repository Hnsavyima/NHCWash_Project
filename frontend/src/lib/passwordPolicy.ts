/** Matches backend `PasswordRules.PATTERN` (8+ chars, one digit, one of !@#$%^&*). */
export const PASSWORD_POLICY_REGEX = /^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$/;
