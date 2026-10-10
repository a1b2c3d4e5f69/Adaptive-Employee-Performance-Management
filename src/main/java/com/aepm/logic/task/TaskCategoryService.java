package com.aepm.logic.task;

import com.aepm.data.task.TaskCategoryRepository;
import com.aepm.model.Role;
import com.aepm.model.TaskCategory;
import com.aepm.model.User;
import com.aepm.util.SessionManager;

import java.util.List;

public class TaskCategoryService {

    private final TaskCategoryRepository repository =
            new TaskCategoryRepository();


    // =========================================================
    // ACTIVE CATEGORIES
    // Manager / Employee
    // =========================================================

    public List<TaskCategory> getActiveCategories()
            throws Exception {

        return repository
                .getActiveCategories(
                        requireAccessToken()
                );
    }


    // =========================================================
    // ALL CATEGORIES
    // Admin only
    // =========================================================

    public List<TaskCategory> getAllCategories()
            throws Exception {

        requireAdmin();


        return repository
                .getAllCategories(
                        requireAccessToken()
                );
    }


    // =========================================================
    // CREATE
    // =========================================================

    public void createCategory(
            String categoryName,
            String description
    ) throws Exception {

        User admin =
                requireAdmin();


        String cleanName =
                validateName(
                        categoryName
                );


        String accessToken =
                requireAccessToken();


        if (
                repository.categoryNameExists(
                        cleanName,
                        accessToken
                )
        ) {

            throw new IllegalArgumentException(
                    "Category name already exists."
            );
        }


        TaskCategory category =
                new TaskCategory();


        category.setCategoryName(
                cleanName
        );


        category.setDescription(
                cleanDescription(
                        description
                )
        );


        category.setCategoryStatus(
                "Active"
        );


        category.setCreatedBy(
                admin.getUserId()
        );


        repository.createCategory(
                category,
                accessToken
        );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public void updateCategory(
            TaskCategory category
    ) throws Exception {

        requireAdmin();


        if (
                category == null
                        || category.getCategoryId() == null
        ) {

            throw new IllegalArgumentException(
                    "Invalid task category."
            );
        }


        category.setCategoryName(
                validateName(
                        category.getCategoryName()
                )
        );


        category.setDescription(
                cleanDescription(
                        category.getDescription()
                )
        );


        if (
                category.getCategoryStatus() == null
                        || category.getCategoryStatus().isBlank()
        ) {

            category.setCategoryStatus(
                    "Active"
            );
        }


        repository.updateCategory(
                category,
                requireAccessToken()
        );
    }


    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================

    public void updateCategoryStatus(
            Long categoryId,
            String newStatus
    ) throws Exception {

        requireAdmin();


        if (categoryId == null) {

            throw new IllegalArgumentException(
                    "Invalid category."
            );
        }


        if (
                !"Active".equals(
                        newStatus
                )
                        && !"Inactive".equals(
                        newStatus
                )
        ) {

            throw new IllegalArgumentException(
                    "Invalid category status."
            );
        }


        repository.updateCategoryStatus(
                categoryId,
                newStatus,
                requireAccessToken()
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deleteCategory(
            Long categoryId
    ) throws Exception {

        requireAdmin();


        if (categoryId == null) {

            throw new IllegalArgumentException(
                    "Invalid category."
            );
        }


        String accessToken =
                requireAccessToken();


        boolean used =
                repository.isCategoryUsed(
                        categoryId,
                        accessToken
                );


        if (used) {

            throw new IllegalStateException(
                    "This category is already used by one or more tasks. "
                            + "Deactivate it instead of deleting it."
            );
        }


        repository.deleteCategory(
                categoryId,
                accessToken
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private String validateName(
            String categoryName
    ) {

        if (
                categoryName == null
                        || categoryName.trim().isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Category name is required."
            );
        }


        String cleanName =
                categoryName.trim();


        if (
                cleanName.length() > 30
        ) {

            throw new IllegalArgumentException(
                    "Category name cannot exceed 30 characters."
            );
        }


        return cleanName;
    }


    private String cleanDescription(
            String description
    ) {

        if (
                description == null
                        || description.trim().isBlank()
        ) {

            return null;
        }


        String clean =
                description.trim();


        if (
                clean.length() > 255
        ) {

            throw new IllegalArgumentException(
                    "Description cannot exceed 255 characters."
            );
        }


        return clean;
    }


    // =========================================================
    // SESSION
    // =========================================================

    private String requireAccessToken() {

        String accessToken =
                SessionManager.getAccessToken();


        if (
                accessToken == null
                        || accessToken.isBlank()
        ) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        return accessToken;
    }


    private User requireAdmin() {

        User currentUser =
                SessionManager.getCurrentUser();


        if (currentUser == null) {

            throw new IllegalStateException(
                    "User session has expired."
            );
        }


        boolean admin =
                currentUser.getRole() == Role.ADMINISTRATOR
                        || currentUser.getRoleId() == 1;


        if (!admin) {

            throw new SecurityException(
                    "Only administrators can manage task categories."
            );
        }


        return currentUser;
    }
}