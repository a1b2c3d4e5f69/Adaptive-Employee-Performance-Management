package com.aepm.ui.admin;

import com.aepm.logic.kpi.KpiCalculationService;
import com.aepm.logic.kpi.KpiComponentService;
import com.aepm.logic.kpi.KpiConfigService;

import com.aepm.logic.user.UserService;

import com.aepm.model.KpiComponent;
import com.aepm.model.KpiConfig;
import com.aepm.model.KpiResult;
import com.aepm.model.User;

import com.aepm.ui.components.UIHelper;
import com.aepm.ui.navigation.NavigationManager;

import com.aepm.util.SessionManager;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.Node;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import javafx.stage.Stage;

import javafx.util.StringConverter;

import java.time.LocalDate;

import java.util.List;
import java.util.Optional;

public class AdminKpiController {

    // =========================================================
    // TOP BAR
    // =========================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label topNameLabel;


    // =========================================================
    // CONFIG SEARCH
    // =========================================================

    @FXML
    private TextField configSearchField;


    // =========================================================
    // CONFIG TABLE
    // =========================================================

    @FXML
    private TableView<KpiConfig> configTable;

    @FXML
    private TableColumn<KpiConfig, Number> configIdColumn;

    @FXML
    private TableColumn<KpiConfig, String> configNameColumn;

    @FXML
    private TableColumn<KpiConfig, Number> riskThresholdColumn;

    @FXML
    private TableColumn<KpiConfig, String> effectiveDateColumn;

    @FXML
    private TableColumn<KpiConfig, String> configStatusColumn;


    // =========================================================
    // CONFIG FORM
    // =========================================================

    @FXML
    private TextField configNameField;

    @FXML
    private TextField riskThresholdField;

    @FXML
    private DatePicker effectiveDatePicker;

    @FXML
    private Label configMessageLabel;


    // =========================================================
    // CONFIG BUTTONS
    // =========================================================

    @FXML
    private Button createConfigButton;

    @FXML
    private Button saveConfigButton;

    @FXML
    private Button configStatusButton;

    @FXML
    private Button deleteConfigButton;


    // =========================================================
    // COMPONENT TABLE
    // =========================================================

    @FXML
    private TableView<KpiComponent> componentTable;

    @FXML
    private TableColumn<KpiComponent, Number> componentIdColumn;

    @FXML
    private TableColumn<KpiComponent, String> componentNameColumn;

    @FXML
    private TableColumn<KpiComponent, Number> weightingColumn;

    @FXML
    private TableColumn<KpiComponent, String> componentStatusColumn;


    // =========================================================
    // COMPONENT FORM
    // =========================================================

    @FXML
    private TextField componentNameField;

    @FXML
    private TextField weightingField;

    @FXML
    private Label totalWeightingLabel;

    @FXML
    private Label componentMessageLabel;


    // =========================================================
    // COMPONENT BUTTONS
    // =========================================================

    @FXML
    private Button createComponentButton;

    @FXML
    private Button saveComponentButton;

    @FXML
    private Button componentStatusButton;

    @FXML
    private Button deleteComponentButton;


    // =========================================================
    // KPI CALCULATION
    // =========================================================

    @FXML
    private ComboBox<User> employeeComboBox;

    @FXML
    private DatePicker evaluationStartDatePicker;

    @FXML
    private DatePicker evaluationEndDatePicker;

    @FXML
    private Button calculateKpiButton;

    @FXML
    private Label productivityResultLabel;

    @FXML
    private Label timelinessResultLabel;

    @FXML
    private Label completionResultLabel;

    @FXML
    private Label attendanceResultLabel;

    @FXML
    private Label overallKpiResultLabel;

    @FXML
    private Label performanceStatusLabel;

    @FXML
    private Label calculationMessageLabel;


    // =========================================================
    // SERVICES
    // =========================================================

    private final KpiConfigService configService =
            new KpiConfigService();

    private final KpiComponentService componentService =
            new KpiComponentService();

    private final KpiCalculationService calculationService =
            new KpiCalculationService();

    private final UserService userService =
            new UserService();


    // =========================================================
    // DATA
    // =========================================================

    private final ObservableList<KpiConfig> configs =
            FXCollections.observableArrayList();

    private final ObservableList<KpiComponent> components =
            FXCollections.observableArrayList();

    private final ObservableList<User> employees =
            FXCollections.observableArrayList();


    private FilteredList<KpiConfig> filteredConfigs;


    private KpiConfig selectedConfig;

    private KpiComponent selectedComponent;


    // =========================================================
    // INITIALIZE
    // =========================================================

    @FXML
    public void initialize() {

        UIHelper.hideMessage(
                configMessageLabel
        );

        UIHelper.hideMessage(
                componentMessageLabel
        );

        UIHelper.hideMessage(
                calculationMessageLabel
        );


        loadCurrentUser();

        setupConfigTable();

        setupComponentTable();

        setupConfigSearch();

        setupSelections();

        setupEmployeeComboBox();


        clearConfigForm();

        clearComponentForm();

        clearCalculationResult();


        loadConfigs();

        loadEmployees();
    }


    // =========================================================
    // CURRENT USER
    // =========================================================

    private void loadCurrentUser() {

        User user =
                SessionManager.getCurrentUser();


        if (user == null) {
            return;
        }


        String name =
                user.getFullName();


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    user.getUsername();
        }


        if (
                name == null
                        || name.isBlank()
        ) {

            name =
                    "Administrator";
        }


        welcomeLabel.setText(
                "Welcome, " + name
        );


        topNameLabel.setText(
                name
        );
    }


    // =========================================================
    // CONFIG TABLE
    // =========================================================

    private void setupConfigTable() {

        configIdColumn.setCellValueFactory(
                cellData ->
                        new SimpleLongProperty(
                                cellData
                                        .getValue()
                                        .getConfigId()
                        )
        );


        configNameColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getConfigName()
                                )
                        )
        );


        riskThresholdColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getRiskThreshold()
                        )
        );


        effectiveDateColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getEffectiveDate()
                                )
                        )
        );


        configStatusColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getConfigStatus()
                                )
                        )
        );


        configTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // COMPONENT TABLE
    // =========================================================

    private void setupComponentTable() {

        componentIdColumn.setCellValueFactory(
                cellData ->
                        new SimpleLongProperty(
                                cellData
                                        .getValue()
                                        .getComponentId()
                        )
        );


        componentNameColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getComponentName()
                                )
                        )
        );


        weightingColumn.setCellValueFactory(
                cellData ->
                        new SimpleDoubleProperty(
                                cellData
                                        .getValue()
                                        .getWeighting()
                        )
        );


        componentStatusColumn.setCellValueFactory(
                cellData ->
                        new SimpleStringProperty(
                                safe(
                                        cellData
                                                .getValue()
                                                .getComponentStatus()
                                )
                        )
        );


        componentTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }


    // =========================================================
    // CONFIG SEARCH
    // =========================================================

    private void setupConfigSearch() {

        filteredConfigs =
                new FilteredList<>(
                        configs,
                        config -> true
                );


        configTable.setItems(
                filteredConfigs
        );


        configSearchField
                .textProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> applyConfigSearch()
                );
    }


    private void applyConfigSearch() {

        String keyword =
                configSearchField.getText() == null
                        ? ""
                        : configSearchField
                                .getText()
                                .trim()
                                .toLowerCase();


        filteredConfigs.setPredicate(
                config -> {

                    if (config == null) {
                        return false;
                    }


                    if (keyword.isBlank()) {
                        return true;
                    }


                    return contains(
                            config.getConfigName(),
                            keyword
                    )
                            || contains(
                            config.getConfigStatus(),
                            keyword
                    )
                            || contains(
                            config.getEffectiveDate(),
                            keyword
                    );
                }
        );
    }


    // =========================================================
    // SELECTION
    // =========================================================

    private void setupSelections() {

        configTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedConfig =
                                        newValue;


                                displayConfig(
                                        newValue
                                );


                                loadComponents(
                                        newValue.getConfigId()
                                );
                            }
                        }
                );


        componentTable
                .getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (
                                observable,
                                oldValue,
                                newValue
                        ) -> {

                            if (newValue != null) {

                                selectedComponent =
                                        newValue;


                                displayComponent(
                                        newValue
                                );
                            }
                        }
                );
    }


    // =========================================================
    // EMPLOYEE COMBO BOX
    // =========================================================

    private void setupEmployeeComboBox() {

        employeeComboBox.setItems(
                employees
        );


        employeeComboBox.setConverter(
                new StringConverter<>() {

                    @Override
                    public String toString(
                            User user
                    ) {

                        if (user == null) {
                            return "";
                        }


                        String staffId =
                                user.getEmployeeId() == null
                                        || user.getEmployeeId().isBlank()
                                        ? "-"
                                        : user.getEmployeeId();


                        String name =
                                user.getFullName() == null
                                        || user.getFullName().isBlank()
                                        ? user.getUsername()
                                        : user.getFullName();


                        if (
                                name == null
                                        || name.isBlank()
                        ) {

                            name =
                                    "Employee";
                        }


                        return staffId
                                + " - "
                                + name;
                    }


                    @Override
                    public User fromString(
                            String string
                    ) {

                        return null;
                    }
                }
        );
    }


    // =========================================================
    // LOAD CONFIGS
    // =========================================================

    @FXML
    private void loadConfigs() {

        UIHelper.hideMessage(
                configMessageLabel
        );


        javafx.concurrent.Task<List<KpiConfig>> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<KpiConfig> call()
                            throws Exception {

                        return configService
                                .getAllConfigs();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<KpiConfig> result =
                            task.getValue();


                    configs.clear();


                    if (result != null) {

                        configs.addAll(
                                result
                        );
                    }


                    configTable.refresh();


                    clearConfigForm();

                    components.clear();

                    clearComponentForm();

                    updateTotalWeightingLabel();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            configMessageLabel,
                            error == null
                                    ? "Unable to load KPI configurations."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD COMPONENTS
    // =========================================================

    private void loadComponents(
            Long configId
    ) {

        UIHelper.hideMessage(
                componentMessageLabel
        );


        javafx.concurrent.Task<List<KpiComponent>> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<KpiComponent> call()
                            throws Exception {

                        return componentService
                                .getComponentsByConfig(
                                        configId
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<KpiComponent> result =
                            task.getValue();


                    components.clear();


                    if (result != null) {

                        components.addAll(
                                result
                        );
                    }


                    componentTable.setItems(
                            components
                    );


                    componentTable.refresh();


                    clearComponentForm();

                    updateTotalWeightingLabel();
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            componentMessageLabel,
                            error == null
                                    ? "Unable to load KPI components."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // LOAD EMPLOYEES
    // =========================================================

    private void loadEmployees() {

        javafx.concurrent.Task<List<User>> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected List<User> call()
                            throws Exception {

                        return userService
                                .getAllUsers();
                    }
                };


        task.setOnSucceeded(
                event -> {

                    List<User> users =
                            task.getValue();


                    employees.clear();


                    if (users != null) {

                        for (User user : users) {

                            if (
                                    user != null
                                            && user.getRoleId() == 3
                                            && "Active".equalsIgnoreCase(
                                            user.getAccountStatus()
                                    )
                            ) {

                                employees.add(
                                        user
                                );
                            }
                        }
                    }
                }
        );


        task.setOnFailed(
                event -> {

                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            calculationMessageLabel,
                            error == null
                                    ? "Unable to load employees."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DISPLAY CONFIG
    // =========================================================

    private void displayConfig(
            KpiConfig config
    ) {

        configNameField.setText(
                safe(
                        config.getConfigName()
                )
        );


        riskThresholdField.setText(
                String.valueOf(
                        config.getRiskThreshold()
                )
        );


        try {

            effectiveDatePicker.setValue(
                    LocalDate.parse(
                            config.getEffectiveDate()
                    )
            );

        } catch (Exception e) {

            effectiveDatePicker.setValue(
                    null
            );
        }


        createConfigButton.setDisable(
                true
        );

        saveConfigButton.setDisable(
                false
        );

        configStatusButton.setDisable(
                false
        );

        deleteConfigButton.setDisable(
                false
        );


        updateConfigStatusButtonText(
                config.getConfigStatus()
        );
    }


    // =========================================================
    // DISPLAY COMPONENT
    // =========================================================

    private void displayComponent(
            KpiComponent component
    ) {

        componentNameField.setText(
                safe(
                        component.getComponentName()
                )
        );


        weightingField.setText(
                String.valueOf(
                        component.getWeighting()
                )
        );


        createComponentButton.setDisable(
                true
        );

        saveComponentButton.setDisable(
                false
        );

        componentStatusButton.setDisable(
                false
        );

        deleteComponentButton.setDisable(
                false
        );


        updateComponentStatusButtonText(
                component.getComponentStatus()
        );
    }


    // =========================================================
    // NEW CONFIG
    // =========================================================

    @FXML
    private void handleNewConfig() {

        configTable
                .getSelectionModel()
                .clearSelection();


        selectedConfig =
                null;


        clearConfigForm();


        components.clear();

        clearComponentForm();

        updateTotalWeightingLabel();


        UIHelper.hideMessage(
                configMessageLabel
        );


        configNameField
                .requestFocus();
    }


    // =========================================================
    // CREATE CONFIG
    // =========================================================

    @FXML
    private void handleCreateConfig() {

        String name =
                configNameField
                        .getText()
                        .trim();


        String thresholdText =
                riskThresholdField
                        .getText()
                        .trim();


        LocalDate effectiveDate =
                effectiveDatePicker
                        .getValue();


        if (name.isBlank()) {

            UIHelper.showError(
                    configMessageLabel,
                    "Configuration name is required."
            );

            return;
        }


        if (thresholdText.isBlank()) {

            UIHelper.showError(
                    configMessageLabel,
                    "Risk threshold is required."
            );

            return;
        }


        if (effectiveDate == null) {

            UIHelper.showError(
                    configMessageLabel,
                    "Effective date is required."
            );

            return;
        }


        double threshold;


        try {

            threshold =
                    Double.parseDouble(
                            thresholdText
                    );

        } catch (NumberFormatException e) {

            UIHelper.showError(
                    configMessageLabel,
                    "Risk threshold must be a number."
            );

            return;
        }


        double finalThreshold =
                threshold;


        createConfigButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        configService
                                .createConfig(
                                        name,
                                        finalThreshold,
                                        effectiveDate.toString()
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            configMessageLabel,
                            "KPI configuration created successfully."
                    );


                    loadConfigs();
                }
        );


        task.setOnFailed(
                event -> {

                    createConfigButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            configMessageLabel,
                            error == null
                                    ? "Unable to create KPI configuration."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // SAVE CONFIG
    // =========================================================

    @FXML
    private void handleSaveConfig() {

        if (selectedConfig == null) {

            UIHelper.showError(
                    configMessageLabel,
                    "Please select a KPI configuration."
            );

            return;
        }


        String name =
                configNameField
                        .getText()
                        .trim();


        String thresholdText =
                riskThresholdField
                        .getText()
                        .trim();


        LocalDate effectiveDate =
                effectiveDatePicker
                        .getValue();


        if (
                name.isBlank()
                        || thresholdText.isBlank()
                        || effectiveDate == null
        ) {

            UIHelper.showError(
                    configMessageLabel,
                    "Please complete all configuration fields."
            );

            return;
        }


        double threshold;


        try {

            threshold =
                    Double.parseDouble(
                            thresholdText
                    );

        } catch (NumberFormatException e) {

            UIHelper.showError(
                    configMessageLabel,
                    "Risk threshold must be a number."
            );

            return;
        }


        selectedConfig.setConfigName(
                name
        );

        selectedConfig.setRiskThreshold(
                threshold
        );

        selectedConfig.setEffectiveDate(
                effectiveDate.toString()
        );


        saveConfigButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        configService
                                .updateConfig(
                                        selectedConfig
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    saveConfigButton.setDisable(
                            false
                    );


                    configTable.refresh();


                    UIHelper.showSuccess(
                            configMessageLabel,
                            "KPI configuration updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    saveConfigButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            configMessageLabel,
                            error == null
                                    ? "Unable to update KPI configuration."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // CONFIG STATUS
    // =========================================================

    @FXML
    private void handleToggleConfigStatus() {

        if (selectedConfig == null) {

            UIHelper.showError(
                    configMessageLabel,
                    "Please select a KPI configuration."
            );

            return;
        }


        String newStatus =
                "Active".equalsIgnoreCase(
                        selectedConfig.getConfigStatus()
                )
                        ? "Inactive"
                        : "Active";


        configStatusButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        configService
                                .updateConfigStatus(
                                        selectedConfig,
                                        newStatus
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    selectedConfig.setConfigStatus(
                            newStatus
                    );


                    updateConfigStatusButtonText(
                            newStatus
                    );


                    configStatusButton.setDisable(
                            false
                    );


                    configTable.refresh();


                    UIHelper.showSuccess(
                            configMessageLabel,
                            "KPI configuration status updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    configStatusButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            configMessageLabel,
                            error == null
                                    ? "Unable to update configuration status."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DELETE CONFIG
    // =========================================================

    @FXML
        private void handleSystemConfiguration(ActionEvent event) {
        navigate(event, "/fxml/admin/systemConfiguration.fxml");
    }

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
    private void handleDeleteConfig() {

        if (selectedConfig == null) {

            UIHelper.showError(
                    configMessageLabel,
                    "Please select a KPI configuration."
            );

            return;
        }


        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        alert.setTitle(
                "Delete KPI Configuration"
        );


        alert.setHeaderText(
                "Delete "
                        + selectedConfig
                        .getConfigName()
                        + "?"
        );


        alert.setContentText(
                "This action permanently removes the configuration."
        );


        Optional<ButtonType> result =
                alert.showAndWait();


        if (
                result.isEmpty()
                        || result.get() != ButtonType.OK
        ) {

            return;
        }


        Long configId =
                selectedConfig
                        .getConfigId();


        deleteConfigButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        configService
                                .deleteConfig(
                                        configId
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event ->
                        loadConfigs()
        );


        task.setOnFailed(
                event -> {

                    deleteConfigButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            configMessageLabel,
                            error == null
                                    ? "Unable to delete KPI configuration."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // NEW COMPONENT
    // =========================================================

    @FXML
    private void handleNewComponent() {

        if (selectedConfig == null) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Please select a KPI configuration first."
            );

            return;
        }


        componentTable
                .getSelectionModel()
                .clearSelection();


        selectedComponent =
                null;


        clearComponentForm();


        UIHelper.hideMessage(
                componentMessageLabel
        );


        componentNameField
                .requestFocus();
    }


    // =========================================================
    // CREATE COMPONENT
    // =========================================================

    @FXML
    private void handleCreateComponent() {

        if (selectedConfig == null) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Please select a KPI configuration first."
            );

            return;
        }


        String name =
                componentNameField
                        .getText()
                        .trim();


        String weightingText =
                weightingField
                        .getText()
                        .trim();


        if (name.isBlank()) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Component name is required."
            );

            return;
        }


        if (weightingText.isBlank()) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Weighting is required."
            );

            return;
        }


        double weighting;


        try {

            weighting =
                    Double.parseDouble(
                            weightingText
                    );

        } catch (NumberFormatException e) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Weighting must be a number."
            );

            return;
        }


        double finalWeighting =
                weighting;


        Long configId =
                selectedConfig
                        .getConfigId();


        createComponentButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        componentService
                                .createComponent(
                                        configId,
                                        name,
                                        finalWeighting
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    UIHelper.showSuccess(
                            componentMessageLabel,
                            "KPI component created successfully."
                    );


                    loadComponents(
                            configId
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    createComponentButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            componentMessageLabel,
                            error == null
                                    ? "Unable to create KPI component."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // SAVE COMPONENT
    // =========================================================

    @FXML
    private void handleSaveComponent() {

        if (selectedComponent == null) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Please select a KPI component."
            );

            return;
        }


        String name =
                componentNameField
                        .getText()
                        .trim();


        String weightingText =
                weightingField
                        .getText()
                        .trim();


        if (name.isBlank()) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Component name is required."
            );

            return;
        }


        if (weightingText.isBlank()) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Weighting is required."
            );

            return;
        }


        double weighting;


        try {

            weighting =
                    Double.parseDouble(
                            weightingText
                    );

        } catch (NumberFormatException e) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Weighting must be a number."
            );

            return;
        }


        selectedComponent.setComponentName(
                name
        );

        selectedComponent.setWeighting(
                weighting
        );


        saveComponentButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        componentService
                                .updateComponent(
                                        selectedComponent
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    saveComponentButton.setDisable(
                            false
                    );


                    componentTable.refresh();

                    updateTotalWeightingLabel();


                    UIHelper.showSuccess(
                            componentMessageLabel,
                            "KPI component updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    saveComponentButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            componentMessageLabel,
                            error == null
                                    ? "Unable to update KPI component."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // COMPONENT STATUS
    // =========================================================

    @FXML
    private void handleToggleComponentStatus() {

        if (selectedComponent == null) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Please select a KPI component."
            );

            return;
        }


        String newStatus =
                "Active".equalsIgnoreCase(
                        selectedComponent
                                .getComponentStatus()
                )
                        ? "Inactive"
                        : "Active";


        componentStatusButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        componentService
                                .updateComponentStatus(
                                        selectedComponent,
                                        newStatus
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event -> {

                    selectedComponent
                            .setComponentStatus(
                                    newStatus
                            );


                    componentStatusButton.setDisable(
                            false
                    );


                    updateComponentStatusButtonText(
                            newStatus
                    );


                    componentTable.refresh();

                    updateTotalWeightingLabel();


                    UIHelper.showSuccess(
                            componentMessageLabel,
                            "KPI component status updated successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    componentStatusButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            componentMessageLabel,
                            error == null
                                    ? "Unable to update component status."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DELETE COMPONENT
    // =========================================================

    @FXML
    private void handleDeleteComponent() {

        if (selectedComponent == null) {

            UIHelper.showError(
                    componentMessageLabel,
                    "Please select a KPI component."
            );

            return;
        }


        Alert alert =
                new Alert(
                        Alert.AlertType.CONFIRMATION
                );


        alert.setTitle(
                "Delete KPI Component"
        );


        alert.setHeaderText(
                "Delete "
                        + selectedComponent
                        .getComponentName()
                        + "?"
        );


        alert.setContentText(
                "This KPI component will be permanently removed."
        );


        Optional<ButtonType> result =
                alert.showAndWait();


        if (
                result.isEmpty()
                        || result.get() != ButtonType.OK
        ) {

            return;
        }


        Long componentId =
                selectedComponent
                        .getComponentId();


        Long configId =
                selectedConfig
                        .getConfigId();


        deleteComponentButton.setDisable(
                true
        );


        javafx.concurrent.Task<Void> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected Void call()
                            throws Exception {

                        componentService
                                .deleteComponent(
                                        componentId
                                );


                        return null;
                    }
                };


        task.setOnSucceeded(
                event ->
                        loadComponents(
                                configId
                        )
        );


        task.setOnFailed(
                event -> {

                    deleteComponentButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            componentMessageLabel,
                            error == null
                                    ? "Unable to delete KPI component."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // CALCULATE KPI
    // =========================================================

    @FXML
    private void handleCalculateKpi() {

        UIHelper.hideMessage(
                calculationMessageLabel
        );


        User employee =
                employeeComboBox
                        .getValue();


        LocalDate startDate =
                evaluationStartDatePicker
                        .getValue();


        LocalDate endDate =
                evaluationEndDatePicker
                        .getValue();


        if (employee == null) {

            UIHelper.showError(
                    calculationMessageLabel,
                    "Please select an employee."
            );

            return;
        }


        if (
                startDate == null
                        || endDate == null
        ) {

            UIHelper.showError(
                    calculationMessageLabel,
                    "Please select the evaluation start and end date."
            );

            return;
        }


        if (
                endDate.isBefore(
                        startDate
                )
        ) {

            UIHelper.showError(
                    calculationMessageLabel,
                    "Evaluation end date cannot be before start date."
            );

            return;
        }


        String employeeUserId =
                employee.getUserId();


        if (
                employeeUserId == null
                        || employeeUserId.isBlank()
        ) {

            UIHelper.showError(
                    calculationMessageLabel,
                    "Selected employee has an invalid user ID."
            );

            return;
        }


        calculateKpiButton.setDisable(
                true
        );


        clearCalculationResult();


        javafx.concurrent.Task<KpiResult> task =
                new javafx.concurrent.Task<>() {

                    @Override
                    protected KpiResult call()
                            throws Exception {

                        return calculationService
                                .calculateKpi(
                                        employeeUserId,
                                        startDate,
                                        endDate
                                );
                    }
                };


        task.setOnSucceeded(
                event -> {

                    calculateKpiButton.setDisable(
                            false
                    );


                    KpiResult result =
                            task.getValue();


                    displayKpiResult(
                            result
                    );


                    UIHelper.showSuccess(
                            calculationMessageLabel,
                            "KPI calculated and saved successfully."
                    );
                }
        );


        task.setOnFailed(
                event -> {

                    calculateKpiButton.setDisable(
                            false
                    );


                    Throwable error =
                            task.getException();


                    if (error != null) {
                        error.printStackTrace();
                    }


                    UIHelper.showError(
                            calculationMessageLabel,
                            error == null
                                    ? "Unable to calculate KPI."
                                    : error.getMessage()
                    );
                }
        );


        Thread thread =
                new Thread(task);


        thread.setDaemon(true);

        thread.start();
    }


    // =========================================================
    // DISPLAY KPI RESULT
    // =========================================================

    private void displayKpiResult(
            KpiResult result
    ) {

        if (result == null) {

            clearCalculationResult();

            return;
        }


        productivityResultLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getProductivityScore()
                )
        );


        timelinessResultLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getTimelinessScore()
                )
        );


        completionResultLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getCompletionRate()
                )
        );


        attendanceResultLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getAttendanceScore()
                )
        );


        overallKpiResultLabel.setText(
                String.format(
                        "%.2f%%",
                        result.getOverallKpiScore()
                )
        );


        performanceStatusLabel.setText(
                safe(
                        result.getPerformanceStatus()
                )
        );
    }


    // =========================================================
    // CLEAR KPI RESULT
    // =========================================================

    private void clearCalculationResult() {

        productivityResultLabel.setText(
                "-"
        );

        timelinessResultLabel.setText(
                "-"
        );

        completionResultLabel.setText(
                "-"
        );

        attendanceResultLabel.setText(
                "-"
        );

        overallKpiResultLabel.setText(
                "-"
        );

        performanceStatusLabel.setText(
                "-"
        );
    }


    // =========================================================
    // TOTAL WEIGHTING
    // =========================================================

    private void updateTotalWeightingLabel() {

        double total =
                0;


        for (
                KpiComponent component
                : components
        ) {

            if (
                    "Active".equalsIgnoreCase(
                            component.getComponentStatus()
                    )
            ) {

                total +=
                        component.getWeighting();
            }
        }


        totalWeightingLabel.setText(
                String.format(
                        "Total Active Weighting: %.2f%% / 100%%",
                        total
                )
        );
    }


    // =========================================================
    // CLEAR CONFIG
    // =========================================================

    private void clearConfigForm() {

        selectedConfig =
                null;


        configTable
                .getSelectionModel()
                .clearSelection();


        configNameField.clear();

        riskThresholdField.clear();

        effectiveDatePicker.setValue(
                null
        );


        createConfigButton.setDisable(
                false
        );

        saveConfigButton.setDisable(
                true
        );

        configStatusButton.setDisable(
                true
        );

        deleteConfigButton.setDisable(
                true
        );


        configStatusButton.setText(
                "Activate"
        );
    }


    // =========================================================
    // CLEAR COMPONENT
    // =========================================================

    private void clearComponentForm() {

        selectedComponent =
                null;


        componentTable
                .getSelectionModel()
                .clearSelection();


        componentNameField.clear();

        weightingField.clear();


        createComponentButton.setDisable(
                selectedConfig == null
        );

        saveComponentButton.setDisable(
                true
        );

        componentStatusButton.setDisable(
                true
        );

        deleteComponentButton.setDisable(
                true
        );


        componentStatusButton.setText(
                "Deactivate"
        );
    }


    // =========================================================
    // STATUS BUTTON TEXT
    // =========================================================

    private void updateConfigStatusButtonText(
            String status
    ) {

        configStatusButton.setText(
                "Active".equalsIgnoreCase(
                        status
                )
                        ? "Deactivate"
                        : "Activate"
        );
    }


    private void updateComponentStatusButtonText(
            String status
    ) {

        componentStatusButton.setText(
                "Active".equalsIgnoreCase(
                        status
                )
                        ? "Deactivate"
                        : "Activate"
        );
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    @FXML
    private void handleHome(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminHome.fxml"
        );
    }


    @FXML
    private void handleProfile(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminProfile.fxml"
        );
    }


    @FXML
    private void handleUserManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminUserManagement.fxml"
        );
    }


    @FXML
    private void handleTaskManagement(
            ActionEvent event
    ) {

        navigate(
                event,
                "/fxml/admin/adminTask.fxml"
        );
    }


    @FXML
    private void handleKpiManagement() {
        // Current page
    }


    @FXML
    private void handleReports() {

        System.out.println(
                "Reports not implemented yet."
        );
    }


    @FXML
    private void handleAnonymousReports() {

        System.out.println(
                "Anonymous Reports not implemented yet."
        );
    }


    @FXML
    private void handleLogout(
            ActionEvent event
    ) {

        SessionManager.clearSession();


        navigate(
                event,
                "/fxml/auth/login.fxml"
        );
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private boolean contains(
            String value,
            String keyword
    ) {

        return value != null
                && value
                .toLowerCase()
                .contains(
                        keyword
                );
    }


    private String safe(
            String value
    ) {

        return value == null
                || value.isBlank()
                ? "-"
                : value;
    }


    private void navigate(
            ActionEvent event,
            String path
    ) {

        Stage stage =
                (Stage) ((Node) event.getSource())
                        .getScene()
                        .getWindow();


        NavigationManager.navigate(
                stage,
                path
        );
    }
}