package com.lulippe.paymybuddy.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class IbanUtil {
    private static final int IBAN_MIN_SIZE = 15;
    private static final int IBAN_MAX_SIZE = 34;
    private static final long IBAN_MAX = 999999999;
    private static final int IBAN_MODULUS = 97;
    /**
     * Checks whether an IBAN (International Bank Account Number) is valid.
     *
     * <p>The validation process follows the IBAN standard:</p>
     * <ol>
     *     <li>Normalization: removes spaces and converts all letters to uppercase.</li>
     *     <li>Length check: the IBAN must contain between {@value IBAN_MIN_SIZE} and {@value IBAN_MAX_SIZE} characters.</li>
     *     <li>Rearrangement: the first four characters (country code + check digits) are moved to the end of the string.</li>
     *     <li>Letter-to-number conversion: letters are replaced with numeric values (A=10, B=11, ..., Z=35).</li>
     *     <li>Modulo 97 calculation: the resulting number is divided by 97; the IBAN is valid if the remainder is 1.</li>
     * </ol>
     *
     * @param iban the string representing the IBAN to validate, can contain spaces or lowercase letters
     * @return {@code true} if the IBAN is valid according to the standard, {@code false} otherwise
     */
    public boolean isIbanValid(final String iban) {
        final String trimmed = normalizeIban(iban);

        if (!hasValidLength(trimmed)) {
            return false;
        }

        final String reformat = reformatIban(trimmed);

        return computeMod97(reformat) == 1;
    }

    private String normalizeIban(final String iban) {
        return iban == null ? "" : iban
                .replaceAll("\\s+", "")
                .toUpperCase();
    }

    private boolean hasValidLength(final String trimmedIban) {
        return trimmedIban.length() >= IBAN_MIN_SIZE && trimmedIban.length() <= IBAN_MAX_SIZE;
    }

    private String reformatIban(final String trimmedIban) {
        return trimmedIban.substring(4) + trimmedIban.substring(0, 4);
    }

    private int computeMod97(final String reformatIban) {
        long total = 0;

        for (int i = 0; i < reformatIban.length(); i++) {

            final int charValue = Character.getNumericValue(reformatIban.charAt(i));

            if (charValue < 0 || charValue > 35) {
                return -1;
            }

            total = (charValue > 9 ? total * 100 : total * 10) + charValue;

            if (total > IBAN_MAX) {
                total = (total % IBAN_MODULUS);
            }
        }
        return (int) (total % IBAN_MODULUS);
    }
}
