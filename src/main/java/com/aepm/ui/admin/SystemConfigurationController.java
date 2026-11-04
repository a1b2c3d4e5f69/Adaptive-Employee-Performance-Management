package com.aepm.ui.admin;

import com.aepm.config.SystemConfigurationService;
import com.aepm.model.User;
import com.aepm.model.WorkingHoursConfig;
import com.aepm.ui.navigation.NavigationManager;
import com.aepm.util.SessionManager;
import com.aepm.ui.common.AEPMDialog;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.aepm.model.Holiday;
import com.aepm.data.repository.HolidayRepository;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.LocalDate;
import java.util.Objects;
import java.util.List;


public class SystemConfigurationController {

    @FXML private Label welcomeLabel;
    @FXML private Label topNameLabel;
    @FXML private ComboBox<String> dayCombo;
    @FXML private CheckBox workingDayCheck;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField breakStartField;
    @FXML private TextField breakEndField;
    @FXML private TextField gracePeriodField;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    
    @FXML private TextField holidayNameField;
    @FXML private DatePicker holidayStartDatePicker;
    @FXML private DatePicker holidayEndDatePicker;
    @FXML private TextArea holidayDescriptionField;
    @FXML private CheckBox holidayNonWorkingCheck;

    @FXML private TableView<Holiday> holidayTable;
    @FXML private TableColumn<Holiday, String> holidayNameColumn;
    @FXML private TableColumn<Holiday, LocalDate> holidayStartDateColumn;
    @FXML private TableColumn<Holiday, LocalDate> holidayEndDateColumn;
    @FXML private TableColumn<Holiday, String> holidayTypeColumn;
    @FXML private TableColumn<Holiday, String> holidayDescriptionColumn;

    @FXML private Label holidayStatusLabel;

    private final HolidayRepository holidayRepository =
            new HolidayRepository();


    private final SystemConfigurationService service =
            new SystemConfigurationService();

    private List<WorkingHoursConfig> configurations;
    private boolean loading = false;
    private String previousSelectedDay;
    private boolean restoringDaySelection = false;


    @FXML
    private void handleAttendanceManagement(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminAttendance.fxml"
        );
    }

    @FXML
    private void handleEscalationRules(ActionEvent event) {

        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();

        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminEscalationRules.fxml"
        );
    }

    @FXML
    private void handleSystemConfiguration(ActionEvent event) {
        // Already on System Configuration page.
        // No navigation needed.
    }

    @FXML
    public void initialize() {
        holidayTable.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
        );
        // Center all Holiday List columns
        holidayNameColumn.setStyle("-fx-alignment: CENTER;");
        holidayStartDateColumn.setStyle("-fx-alignment: CENTER;");
        holidayEndDateColumn.setStyle("-fx-alignment: CENTER;");
        holidayTypeColumn.setStyle("-fx-alignment: CENTER;");
        holidayDescriptionColumn.setStyle("-fx-alignment: CENTER;");

        User user = SessionManager.getCurrentUser();

        String name = "Administrator";

        if (user != null) {
            name = user.getFullName();

            if (name == null || name.isBlank()) {
                name = user.getUsername();
            }

            if (name == null || name.isBlank()) {
                name = "Administrator";
            }
        }

        welcomeLabel.setText("Welcome, " + name);
        topNameLabel.setText(name);

        dayCombo.setItems(FXCollections.observableArrayList(
                "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday", "Sunday"
        ));

        dayCombo.valueProperty().addListener(
            (obs, oldDay, newDay) -> {

                if (restoringDaySelection || loading) {
                    return;
                }

                if (newDay == null || newDay.equals(previousSelectedDay)) {
                    return;
                }

                if (previousSelectedDay != null && hasUnsavedChanges()) {

                    ButtonType discardButton = new ButtonType(
                        "Discard Changes",
                        ButtonBar.ButtonData.NO
                    );

                    ButtonType keepButton = new ButtonType(
                        "Keep Editing",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                    );

                    Alert alert = new Alert(
                        Alert.AlertType.CONFIRMATION,
                        "You have unsaved changes for " + previousSelectedDay
                            + ". Discard them and switch to " + newDay + "?",
                        discardButton,
                        keepButton
                    );

                    alert.setTitle("Unsaved Changes");
                    alert.setHeaderText("Your changes have not been saved.");

                    ButtonType result = alert.showAndWait()
                            .orElse(keepButton);

                    if (result != discardButton) {

                        restoringDaySelection = true;

                        try {
                            dayCombo.getSelectionModel()
                                    .select(previousSelectedDay);
                        } finally {
                            restoringDaySelection = false;
                        }

                        return;
                    }
                }

                previousSelectedDay = newDay;
                displaySelectedDay();
            }
        );

        workingDayCheck.selectedProperty().addListener(
            (obs, oldValue, newValue) -> {
                updateFieldState();
                updateSaveButtonState();
            }
        );

        startTimeField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateSaveButtonState()
        );

        endTimeField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateSaveButtonState()
        );

        breakStartField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateSaveButtonState()
        );

        breakEndField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateSaveButtonState()
        );

        gracePeriodField.textProperty().addListener(
            (obs, oldValue, newValue) -> updateSaveButtonState()
        );

        loadConfiguration();
        initializeHolidays();
    }


    private boolean hasUnsavedChanges() {

        WorkingHoursConfig config = configurations == null
                ? null
                : configurations.stream()
                    .filter(c -> previousSelectedDay != null
                            && previousSelectedDay.equalsIgnoreCase(
                                c.getDayOfWeek()
                            ))
                    .findFirst()
                    .orElse(null);

        if (config == null) {
            return false;
        }

        boolean working = workingDayCheck.isSelected();

        boolean changed = working != config.isWorkingDay();

        if (working) {
            changed = changed
                    || !normalizeTime(startTimeField.getText())
                        .equals(normalizeTime(config.getStartTime()))
                    || !normalizeTime(endTimeField.getText())
                        .equals(normalizeTime(config.getEndTime()))
                    || !normalizeTime(breakStartField.getText())
                        .equals(normalizeTime(config.getBreakStartTime()))
                    || !normalizeTime(breakEndField.getText())
                        .equals(normalizeTime(config.getBreakEndTime()));
        }

        changed = changed
                || !gracePeriodField.getText().trim().equals(
                    String.valueOf(config.getGracePeriodMinutes())
                );

        return changed;
    }

    
    private boolean hasHolidayChanges() {

        Holiday selected =
                holidayTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            return false;
        }

        String name = holidayNameField.getText() == null
                ? ""
                : holidayNameField.getText().trim();

        String description =
                holidayDescriptionField.getText() == null
                ? ""
                : holidayDescriptionField.getText().trim();

        String originalDescription =
                selected.getDescription() == null
                ? ""
                : selected.getDescription().trim();

        return !name.equals(selected.getHolidayName())
                || !Objects.equals(
                        holidayStartDatePicker.getValue(),
                        selected.getStartDate()
                )
                || !Objects.equals(
                        holidayEndDatePicker.getValue(),
                        selected.getEndDate()
                )
                || !description.equals(originalDescription)
                || holidayNonWorkingCheck.isSelected()
                        != selected.isNonWorkingDay();
    }

    private void updateHolidayButtonState() {

        if (updateHolidayButton != null) {
            updateHolidayButton.setDisable(
                    !hasHolidayChanges()
            );
        }
    }

    
    private void updateSaveButtonState() {

        if (loading) {
            return;
        }

        WorkingHoursConfig config = getSelectedConfig();

        if (config == null) {
            saveButton.setDisable(true);
            return;
        }

        boolean working = workingDayCheck.isSelected();

        boolean changed =
                working != config.isWorkingDay();

        if (working) {
            changed = changed
                    || !normalizeTime(startTimeField.getText())
                            .equals(normalizeTime(config.getStartTime()))
                    || !normalizeTime(endTimeField.getText())
                            .equals(normalizeTime(config.getEndTime()))
                    || !normalizeTime(breakStartField.getText())
                            .equals(normalizeTime(config.getBreakStartTime()))
                    || !normalizeTime(breakEndField.getText())
                            .equals(normalizeTime(config.getBreakEndTime()));
        }

        changed = changed
                || !gracePeriodField.getText().trim().equals(
                        String.valueOf(config.getGracePeriodMinutes())
                );

        saveButton.setDisable(!changed);
    }


    private String normalizeTime(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }

        try {
            return java.time.LocalTime.parse(time.trim()).toString();
        } catch (Exception e) {
            return time.trim();
        }
    }
    
    private void loadConfiguration() {

        try {
            // 1. Load records from Supabase
            configurations = service.loadWorkingHours();

            // 2. Debug output
            System.out.println("===== WORKING HOURS DEBUG =====");

            System.out.println(
                "Records loaded: " +
                (configurations == null ? "null" : configurations.size())
            );

            if (configurations != null) {
                for (WorkingHoursConfig config : configurations) {
                    System.out.println(
                        "ID: " + config.getId()
                        + " | Day: " + config.getDayOfWeek()
                        + " | Start: " + config.getStartTime()
                        + " | End: " + config.getEndTime()
                    );
                }
            }

            // 3. Check whether records exist
            if (configurations == null || configurations.isEmpty()) {
                saveButton.setDisable(true);
                statusLabel.setText(
                    "No working hours found in Supabase."
                );
                return;
            }

            // 4. Select Monday and display configuration
            restoringDaySelection = true;

            try {
                String selectedDay = dayCombo.getValue();

                if (selectedDay == null || selectedDay.isBlank()) {
                    dayCombo.getSelectionModel().selectFirst();
                } else {
                    dayCombo.getSelectionModel().select(selectedDay);
                }
            } finally {
                restoringDaySelection = false;
            }

            displaySelectedDay();

        } catch (Exception e) {

            saveButton.setDisable(true);
            statusLabel.setText("Failed to load configuration.");

            showError("Load Error", e.getMessage());
        }
    }


    private WorkingHoursConfig getSelectedConfig() {

        String day = dayCombo.getValue();

        if (day == null || configurations == null) {
            return null;
        }

        return configurations.stream()
                .filter(c -> day.equalsIgnoreCase(
                        c.getDayOfWeek()))
                .findFirst()
                .orElse(null);
    }

    private void displaySelectedDay() {

        WorkingHoursConfig config = getSelectedConfig();

        loading = true;

        try {
            if (config == null) {
                clearFields();
                saveButton.setDisable(true);
                statusLabel.setText(
                        "No configuration found for selected day."
                );
                return;
            }

            workingDayCheck.setSelected(
                    config.isWorkingDay()
            );

            startTimeField.setText(
                    safe(config.getStartTime())
            );

            endTimeField.setText(
                    safe(config.getEndTime())
            );

            breakStartField.setText(
                    safe(config.getBreakStartTime())
            );

            breakEndField.setText(
                    safe(config.getBreakEndTime())
            );

            gracePeriodField.setText(
                    String.valueOf(
                            config.getGracePeriodMinutes()
                    )
            );

            statusLabel.setText(
                    "Configuration loaded for " +
                    config.getDayOfWeek()
            );

        } finally {
            loading = false;
            previousSelectedDay = dayCombo.getValue();
            updateFieldState();
            updateSaveButtonState();
        }
    }

    private void updateFieldState() {

        if (loading) {
            return;
        }

        boolean disabled =
                !workingDayCheck.isSelected();

        startTimeField.setDisable(disabled);
        endTimeField.setDisable(disabled);
        breakStartField.setDisable(disabled);
        breakEndField.setDisable(disabled);
    }

    private void clearFields() {

        workingDayCheck.setSelected(false);
        startTimeField.clear();
        endTimeField.clear();
        breakStartField.clear();
        breakEndField.clear();
        gracePeriodField.clear();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @FXML
    private void handleSave() {

        WorkingHoursConfig oldConfig =
                getSelectedConfig();

        if (oldConfig == null) {
            showError(
                    "Save Error",
                    "No working day record selected."
            );
            return;
        }

        try {
            int grace = Integer.parseInt(
                    gracePeriodField.getText().trim()
            );

            boolean working =
                    workingDayCheck.isSelected();

            WorkingHoursConfig updated =
                    new WorkingHoursConfig(
                            oldConfig.getId(),
                            oldConfig.getDayOfWeek(),
                            working
                                    ? startTimeField.getText().trim()
                                    : null,
                            working
                                    ? endTimeField.getText().trim()
                                    : null,
                            working
                                    ? emptyToNull(breakStartField.getText())
                                    : null,
                            working
                                    ? emptyToNull(breakEndField.getText())
                                    : null,
                            grace,
                            working
                    );

            service.saveWorkingHours(updated);

            statusLabel.setText(
                    "Saved successfully for " +
                    updated.getDayOfWeek()
            );

            // Reload actual database values.
            String selectedDay = dayCombo.getValue();

            configurations = service.loadWorkingHours();

            dayCombo.getSelectionModel().select(
                    selectedDay
            );

            displaySelectedDay();

            showInfo(
                    "Success",
                    "Working hours saved successfully."
            );

        } catch (NumberFormatException e) {
            showError(
                    "Invalid Grace Period",
                    "Please enter a valid number."
            );

        } catch (Exception e) {
            showError(
                    "Save Failed",
                    e.getMessage()
            );
        }
    }

    private String emptyToNull(String text) {
        return text == null || text.isBlank()
                ? null
                : text.trim();
    }

    @FXML
    private void handleRefresh() {

        if (hasUnsavedChanges()) {

            ButtonType discardButton = new ButtonType(
                "Discard Changes",
                ButtonBar.ButtonData.NO
            );

            ButtonType keepButton = new ButtonType(
                "Keep Editing",
                ButtonBar.ButtonData.CANCEL_CLOSE
            );

            Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "You have unsaved changes. Refreshing will discard them.",
                discardButton,
                keepButton
            );

            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Discard unsaved changes?");

            if (alert.showAndWait().orElse(keepButton) != discardButton) {
                return;
            }
        }

        loadConfiguration();
    }

    @FXML private Button updateHolidayButton;

    @FXML
    private void handleHome(ActionEvent event) {
        navigate(event, "/fxml/admin/adminHome.fxml");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        navigate(event, "/fxml/admin/adminProfile.fxml");
    }

    @FXML
    private void handleUserManagement(ActionEvent event) {
        navigate(
                event,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }

    @FXML
    private void handleTaskManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminTask.fxml");
    }

    @FXML
    private void handleKpiManagement(ActionEvent event) {
        navigate(event, "/fxml/admin/adminKpi.fxml");
    }

    @FXML
    private void handleReports(ActionEvent event) {
        System.out.println("Reports page not implemented yet.");
    }

    @FXML
    private void handleAnonymousReports(ActionEvent event) {
        System.out.println("Anonymous Reports page not implemented yet.");
    }

    @FXML
    private void handleLogout(ActionEvent event) {

        SessionManager.clearSession();

        navigate(event, "/fxml/auth/login.fxml");
    }

    private void navigate(
            ActionEvent event,
            String path) {

        Stage stage = (Stage) ((Node)
                event.getSource())
                .getScene()
                .getWindow();

        NavigationManager.navigate(stage, path);
    }

    
    private void showInfo(String title, String message) {

        AEPMDialog.showSuccess(
            welcomeLabel.getScene().getWindow(),
            title,
            message
        );
    }

    private void showError(String title, String message) {

        System.err.println(
            "[AEPM ERROR] " + title + ": " + message
        );

        AEPMDialog.showError(
            welcomeLabel.getScene().getWindow(),
            title,
            message
        );
    }


    
    private void initializeHolidays() {

        holidayNameColumn.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getHolidayName()
            )
        );

        holidayStartDateColumn.setCellValueFactory(
            data -> new javafx.beans.property.SimpleObjectProperty<>(
                data.getValue().getStartDate()
            )
        );

        holidayEndDateColumn.setCellValueFactory(
            data -> new javafx.beans.property.SimpleObjectProperty<>(
                data.getValue().getEndDate()
            )
        );

        holidayTypeColumn.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().isNonWorkingDay() ? "Yes" : "No"
            )
        );

        holidayDescriptionColumn.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getDescription()
            )
        );

        
        holidayNameField.textProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateHolidayButtonState()
        );

        holidayStartDatePicker.valueProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateHolidayButtonState()
        );

        holidayEndDatePicker.valueProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateHolidayButtonState()
        );

        holidayDescriptionField.textProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateHolidayButtonState()
        );

        holidayNonWorkingCheck.selectedProperty().addListener(
                (obs, oldValue, newValue) ->
                        updateHolidayButtonState()
        );

        
        holidayTable.getSelectionModel().selectedItemProperty().addListener((obs, oldHoliday, selected) -> {
            if (selected != null) {

                holidayNameField.setText(
                    selected.getHolidayName()
                );

                holidayStartDatePicker.setValue(
                    selected.getStartDate()
                );

                holidayEndDatePicker.setValue(
                    selected.getEndDate()
                );

                holidayDescriptionField.setText(
                    selected.getDescription()
                );

                holidayNonWorkingCheck.setSelected(
                    selected.isNonWorkingDay()
                );
            }

            updateHolidayButtonState();
        });

        loadHolidays();
    }

    private void loadHolidays() {
        try {
            holidayTable.setItems(
                FXCollections.observableArrayList(
                    holidayRepository.getAllHolidays()
                )
            );

            holidayStatusLabel.setText(
                "Loaded " + holidayTable.getItems().size() + " holidays."
            );

        } catch (Exception e) {
            holidayStatusLabel.setText("Failed to load holidays.");
            showError("Holiday Load Error", e.getMessage());
        }
    }

    private Holiday readHolidayForm(long id) throws Exception {

        String name = holidayNameField.getText().trim();
        LocalDate startDate = holidayStartDatePicker.getValue();
        LocalDate endDate = holidayEndDatePicker.getValue();

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                "Please enter Holiday Name."
            );
        }

        if (startDate == null) {
            throw new IllegalArgumentException(
                "Please select Start Date."
            );
        }

        if (endDate == null) {
            throw new IllegalArgumentException(
                "Please select End Date."
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                "End Date cannot be earlier than Start Date."
            );
        }

        // Check overlapping holiday dates
        for (Holiday existing : holidayRepository.getAllHolidays()) {

            // Ignore the same record when updating
            if (existing.getHolidayId() == id && id != 0) {
                continue;
            }

            boolean overlaps =
                    !endDate.isBefore(existing.getStartDate())
                    && !startDate.isAfter(existing.getEndDate());

            if (overlaps) {
                throw new IllegalArgumentException(
                    "Selected dates overlap with existing holiday: "
                    + existing.getHolidayName()
                    + " ("
                    + existing.getStartDate()
                    + " to "
                    + existing.getEndDate()
                    + ")."
                );
            }
        }

        return new Holiday(
            id,
            name,
            startDate,
            endDate,
            holidayDescriptionField.getText().trim(),
            holidayNonWorkingCheck.isSelected()
        );
    }

    @FXML
    private void handleAddHoliday() {
        try {
            Holiday holiday = readHolidayForm(0);

            holidayRepository.addHoliday(holiday);

            loadHolidays();
            clearHolidayForm();

            showInfo("Success", "Holiday added successfully.");

        } catch (Exception e) {
            showError("Add Holiday Failed", e.getMessage());
        }
    }

    @FXML
    private void handleUpdateHoliday() {

        Holiday selected =
            holidayTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("No Selection", "Please select a holiday first.");
            return;
        }

        if (!hasHolidayChanges()) {
            updateHolidayButtonState();
            return;
        }

        try {
            Holiday updated = readHolidayForm(
                selected.getHolidayId()
            );

            holidayRepository.updateHoliday(updated);

            loadHolidays();
            clearHolidayForm();

            showInfo("Success", "Holiday updated successfully.");

        } catch (Exception e) {
            showError("Update Holiday Failed", e.getMessage());
        }
    }

    @FXML
    private void handleDeleteHoliday() {

        Holiday selected =
            holidayTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("No Selection", "Please select a holiday first.");
            return;
        }

        boolean confirmed = AEPMDialog.showConfirmation(
            welcomeLabel.getScene().getWindow(),
            "Delete Holiday",
            "Are you sure you want to delete \""
                    + selected.getHolidayName()
                    + "\"?\n\n"
                    + "This action cannot be undone."
        );

        if (!confirmed) {
            return;
        }

        try {
            holidayRepository.deleteHoliday(
                selected.getHolidayId()
            );

            loadHolidays();
            clearHolidayForm();

            showInfo("Success", "Holiday deleted successfully.");

        } catch (Exception e) {
            showError("Delete Holiday Failed", e.getMessage());
        }
    }

    @FXML
        private void handleAIConfiguration(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene().getWindow();        
        NavigationManager.navigate(
                stage,
                "/fxml/admin/adminAIConfiguration.fxml"
        );
    }
    
    @FXML
    private void handleAnnouncements(ActionEvent event) {
        AdminAnnouncementsDialog.show(
            ((Node) event.getSource()).getScene().getWindow()
        );
    }

    @FXML
    private void handleRefreshHolidays() {

        if (hasHolidayChanges()) {

            ButtonType discard = new ButtonType(
                    "Discard Changes",
                    ButtonBar.ButtonData.OK_DONE
            );

            ButtonType keep = new ButtonType(
                    "Keep Editing",
                    ButtonBar.ButtonData.CANCEL_CLOSE
            );

            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Refreshing will discard your unsaved holiday changes.",
                    discard,
                    keep
            );

            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Discard unsaved changes?");

            if (alert.showAndWait().orElse(keep) != discard) {
                return;
            }
        }

        loadHolidays();
        clearHolidayForm();
    }


    
    @FXML
    private void handleClearHoliday() {

        if (hasHolidayChanges()) {

            ButtonType discard = new ButtonType(
                    "Discard Changes",
                    ButtonBar.ButtonData.OK_DONE
            );

            ButtonType keep = new ButtonType(
                    "Keep Editing",
                    ButtonBar.ButtonData.CANCEL_CLOSE
            );

            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "You have unsaved holiday changes.",
                    discard,
                    keep
            );

            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Discard your changes?");

            if (alert.showAndWait().orElse(keep) != discard) {
                return;
            }
        }

        clearHolidayForm();
    }

    
    private void clearHolidayForm() {

        holidayTable.getSelectionModel().clearSelection();

        holidayNameField.clear();

        holidayStartDatePicker.setValue(null);
        holidayEndDatePicker.setValue(null);

        holidayDescriptionField.clear();

        holidayNonWorkingCheck.setSelected(true);

        updateHolidayButtonState();
    }


}
