/*
 * Copyright (c) 2008-2013, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.apache.logging.log4j.spi.LoggerProvider;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Logging to Log4j 2.x.
 */
public class Log4j2Factory extends LoggerFactorySupport {

    private static final String FQCN = Log4j2Logger.class.getName();

    protected ILogger createLogger(String name) {
        final LoggerProvider l = PrivateManager.getLogger(name);
        return new Log4j2Logger(l);
    }

    class Log4j2Logger extends AbstractLogger {
        private final LoggerProvider logger;

        public Log4j2Logger(LoggerProvider logger) {
            this.logger = logger;
        }

        public void log(LogEvent logEvent) {
            LogRecord logRecord = logEvent.getLogRecord();
            Level level = logEvent.getLogRecord().getLevel();
            String message = logRecord.getMessage();
            Throwable thrown = logRecord.getThrown();
            log(level, message, thrown);
        }

        public void log(Level level, String message) {
            logger.logIfEnabled(FQCN, getLevel(level), null, message, (Throwable) null);
        }

        public void log(Level level, String message, Throwable thrown) {
            logger.logIfEnabled(FQCN, getLevel(level), null, message, thrown);
        }

        public Level getLevel() {
            if (logger.isDebugEnabled()) {
                return Level.FINEST;
            } else if (logger.isInfoEnabled()) {
                return Level.INFO;
            } else if (logger.isWarnEnabled()) {
                return Level.WARNING;
            } else {
                return Level.SEVERE;
            }
        }

        public boolean isLoggable(Level level) {
            if (Level.OFF == level) {
                return false;
            } else {
                return logger.isEnabled(getLevel(level), null);
            }
        }

        org.apache.logging.log4j.Level getLevel(Level level) {
            if (Level.FINEST == level) {
                return org.apache.logging.log4j.Level.DEBUG;
            } else if (Level.INFO == level) {
                return org.apache.logging.log4j.Level.INFO;
            } else if (Level.WARNING == level) {
                return org.apache.logging.log4j.Level.WARN;
            } else if (Level.SEVERE == level) {
                return org.apache.logging.log4j.Level.FATAL;
            } else {
                return org.apache.logging.log4j.Level.INFO;
            }
        }
    }

    /**
     * The real bridge between commons logging and Log4j.
     */
    private static class PrivateManager extends LogManager {
        private static final String FQCN = Logger.class.getName();

        public static LoggerContext getContext() {
            return getContext(FQCN, false);
        }

        public static LoggerProvider getLogger(final String name) {
            return getContext().getLogger(name);
        }
    }
}
