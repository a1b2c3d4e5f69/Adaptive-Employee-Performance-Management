package com.aepm.config;

import com.aepm.data.repository.SystemConfigurationRepository;
import com.aepm.model.WorkingHoursConfig;

import java.time.LocalTime;
import java.util.List;

public class SystemConfigurationService {

    private final SystemConfigurationRepository repository =
            new SystemConfigurationRepository();

    public List<WorkingHoursConfig> loadWorkingHours()
            throws Exception {

        return repository.getWorkingHours();
    }

    public void saveWorkingHours(
            WorkingHoursConfig config
    ) throws Exception {

        if (config.getGracePeriodMinutes() < 0) {
            throw new IllegalArgumentException(
                    "Grace period cannot be negative."
            );
        }

        if (config.isWorkingDay()) {

            LocalTime start = parseRequired(
                    config.getStartTime(), "Start time"
            );

            LocalTime end = parseRequired(
                    config.getEndTime(), "End time"
            );

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException(
                        "End time must be later than start time."
                );
            }

            String breakStartText = config.getBreakStartTime();
            String breakEndText = config.getBreakEndTime();

            boolean hasBreakStart = breakStartText != null
                    && !breakStartText.isBlank();

            boolean hasBreakEnd = breakEndText != null
                    && !breakEndText.isBlank();

            if (hasBreakStart != hasBreakEnd) {
                throw new IllegalArgumentException(
                        "Both break start and end times are required."
                );
            }

            if (hasBreakStart) {
                LocalTime breakStart = parseRequired(
                        breakStartText, "Break start"
                );

                LocalTime breakEnd = parseRequired(
                        breakEndText, "Break end"
                );

                if (breakStart.isBefore(start)
                        || !breakEnd.isAfter(breakStart)
                        || breakEnd.isAfter(end)) {

                    throw new IllegalArgumentException(
                            "Break must be within working hours."
                    );
                }
            }
        }

        repository.updateWorkingHours(config);
    }

    private LocalTime parseRequired(
            String value,
            String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        try {
            return LocalTime.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    fieldName + " must use HH:mm format."
            );
        }
    }
}