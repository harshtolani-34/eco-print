package com.example.eco_print.utils;


public final class WasteReportValidator {

    public enum Field {
        NONE,
        PHOTO,
        WEIGHT,
        DESCRIPTION,
        LOCATION
    }

    public static final class ValidationResult {
        private final boolean valid;
        private final Field field;
        private final String message;
        private final double estimatedWeight;

        private ValidationResult(
                boolean valid,
                Field field,
                String message,
                double estimatedWeight
        ) {
            this.valid = valid;
            this.field = field;
            this.message = message;
            this.estimatedWeight = estimatedWeight;
        }

        public boolean isValid() {
            return valid;
        }

        public Field getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public double getEstimatedWeight() {
            return estimatedWeight;
        }
    }

    private WasteReportValidator() {
        // Utility class.
    }

    public static ValidationResult validate(
            boolean hasPhoto,
            String weightText,
            String description,
            boolean hasLocation
    ) {
        if (!hasPhoto) {
            return error(
                    Field.PHOTO,
                    "Add a clear photo of the plastic waste before submitting."
            );
        }

        double estimatedWeight = 0.0;
        String cleanWeight = weightText == null ? "" : weightText.trim();

        if (!cleanWeight.isEmpty()) {
            try {
                estimatedWeight = Double.parseDouble(cleanWeight);
            } catch (NumberFormatException exception) {
                return error(
                        Field.WEIGHT,
                        "Enter a valid weight or leave the field blank."
                );
            }

            if (!Double.isFinite(estimatedWeight)
                    || estimatedWeight <= 0
                    || estimatedWeight > 10000) {
                return error(
                        Field.WEIGHT,
                        "Weight must be greater than 0 and no more than 10,000 kg."
                );
            }
        }

        String cleanDescription = description == null
                ? ""
                : description.trim();

        if (cleanDescription.length() < 10) {
            return error(
                    Field.DESCRIPTION,
                    "Enter at least 10 characters so the collector can understand the report."
            );
        }

        if (cleanDescription.length() > 500) {
            return error(
                    Field.DESCRIPTION,
                    "Keep the description within 500 characters."
            );
        }

        if (!hasLocation) {
            return error(
                    Field.LOCATION,
                    "Capture the waste location so the collector knows where to go."
            );
        }

        return new ValidationResult(
                true,
                Field.NONE,
                "",
                estimatedWeight
        );
    }

    private static ValidationResult error(
            Field field,
            String message
    ) {
        return new ValidationResult(
                false,
                field,
                message,
                0.0
        );
    }
}
