package com.dinhkhang.code.config;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Listener to properly shutdown MySQL's AbandonedConnectionCleanupThread
 * to prevent memory leaks and illegal access warnings when the application
 * stops.
 */
@WebListener
public class MySQLCleanupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Nothing to do on startup
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception e) {
            // Log the exception if needed
            System.err.println("Error shutting down MySQL cleanup thread: " + e.getMessage());
        }
    }
}
