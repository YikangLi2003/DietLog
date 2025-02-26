package com.github.yikangli2003.database;

import com.github.yikangli2003.database.entity.*;
import com.github.yikangli2003.database.exception.*;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.RollbackException;
import jakarta.persistence.EntityExistsException;

import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.List;

public class DataAccessor {
    private static final EntityManagerFactory entityManagerFactory =
            Persistence.createEntityManagerFactory("objectdb:ApplicationDatabase.odb");

    public static void closeEntityManagerFactory() { entityManagerFactory.close(); }

    private static void executeInTransaction(Consumer<EntityManager> operation) {
        executeInTransaction(entityManager -> {
            operation.accept(entityManager);
            return null;
        });
    }

    private static <T> T executeInTransaction(Function<EntityManager, T> operation) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        try (entityManager) {
            transaction.begin();
            T result = operation.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    // User entity related methods performing CURD operations.

    public static void signUpNewUser (
            String email,
            String hashedPassword,
            String name,
            LocalDateTime localRegistrationTime
    ) {
        executeInTransaction(entityManager -> {
            User newUser = new User(email, hashedPassword, name, localRegistrationTime);
            entityManager.persist(newUser);
        });
    }

    public static void changeUserPassword(String targetAccount, String newHashedPassword) throws UserNotFoundException {
        executeInTransaction(entityManager -> {
            User user = entityManager.find(User.class, targetAccount);
            if (user != null) {
                user.setHashedPassword(newHashedPassword);
            } else {
                throw new UserNotFoundException(targetAccount);
            }
        });
    }

    public static void changeUserName(String targetAccount, String newUsername) throws UserNotFoundException {
        executeInTransaction(entityManager -> {
            User user = entityManager.find(User.class, targetAccount);
            if (user != null) {
                user.setName(newUsername);
            } else {
                throw new UserNotFoundException(targetAccount);
            }
        });
    }

    public static User getUserByAccount(String account) throws UserNotFoundException {
        return executeInTransaction(entityManager -> {
            User user = entityManager.find(User.class, account);
            if (user != null) {
                return user;
            } else {
                throw new UserNotFoundException(account);
            }
        });
    }
}
