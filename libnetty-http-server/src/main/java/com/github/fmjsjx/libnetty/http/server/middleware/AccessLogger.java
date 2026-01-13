package com.github.fmjsjx.libnetty.http.server.middleware;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.CharsetUtil;

/**
 * A {@link Middleware} logging HTTP access logs.
 *
 * @since 1.1
 *
 * @author MJ Fang
 */
public class AccessLogger implements Middleware {

    private static final DateTimeFormatter DEFAULT_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final BigDecimal T6 = BigDecimal.valueOf(1_000_000L);

    /**
     * A logger wrapper.
     */
    @FunctionalInterface
    public interface LoggerWrapper {

        /**
         * Log content.
         *
         * @param content the content
         */
        void log(String content);

        /**
         * Returns whether this logger wrapper is enabled or not.
         *
         * @return {@code true} if this logger wrapper is enabled, {@code false} otherwise
         */
        default boolean isEnabled() {
            return true;
        }

    }

    /**
     * The stdout implementation of {@link LoggerWrapper}.
     */
    public static final class StdoutLoggerWrapper extends FunctionalLoggerWrapper {

        private static final StdoutLoggerWrapper INSTANCE = new StdoutLoggerWrapper();

        /**
         * Returns the global singleton {@link StdoutLoggerWrapper} instance.
         *
         * @return the global singleton {@code StdoutLoggerWrapper} instance
         */
        public static final StdoutLoggerWrapper getInstance() {
            return INSTANCE;
        }

        /**
         * Constructs a new {@link StdoutLoggerWrapper} instance.
         */
        public StdoutLoggerWrapper() {
            super(System.out::println);
        }

    }

    /**
     * The stderr implementation of {@link LoggerWrapper}.
     */
    public static final class StderrLoggerWrapper extends FunctionalLoggerWrapper {

        private static final StderrLoggerWrapper INSTANCE = new StderrLoggerWrapper();

        /**
         * Returns the global singleton {@link StderrLoggerWrapper} instance.
         *
         * @return the global singleton {@code StderrLoggerWrapper} instance
         */
        public static final StderrLoggerWrapper getInstance() {
            return INSTANCE;
        }

        /**
         * Constructs a new {@link StderrLoggerWrapper} instance.
         */
        public StderrLoggerWrapper() {
            super(System.err::println);
        }

    }

    /**
     * Functional implementation of {@link LoggerWrapper}.
     */
    public static class FunctionalLoggerWrapper implements LoggerWrapper {

        private final Consumer<String> logAction;
        private final BooleanSupplier enabledChecker;

        /**
         * Constructs a new {@link FunctionalLoggerWrapper} instance with the specified {@code logAction} and the
         * specified {@code enabledChecker} given.
         *
         * @param logAction the log action
         * @param enabledChecker the enabled state checker
         */
        public FunctionalLoggerWrapper(Consumer<String> logAction, BooleanSupplier enabledChecker) {
            this.logAction = Objects.requireNonNull(logAction, "logAction must not be null");
            this.enabledChecker = Objects.requireNonNull(enabledChecker, "enabledChecker must not be null");
        }

        /**
         * Constructs a new {@link FunctionalLoggerWrapper} instance with the specified {@code logAction} given.
         *
         * @param logAction the log action
         */
        public FunctionalLoggerWrapper(Consumer<String> logAction) {
            this(logAction, Boolean.TRUE::booleanValue);
        }

        @Override
        public void log(String content) {
            logAction.accept(content);
        }

        @Override
        public boolean isEnabled() {
            return enabledChecker.getAsBoolean();
        }

    }

    /**
     * The {@code slf4j} implementation of {@link LoggerWrapper}.
     */
    public static final class Slf4jLoggerWrapper extends FunctionalLoggerWrapper {

        private static final Consumer<String> logAction(Logger logger, Level level) {
            Objects.requireNonNull(logger, "logger must not be null");
            Objects.requireNonNull(level, "level must not be null");
            return switch (level) {
                case DEBUG -> logger::debug;
                case ERROR -> logger::error;
                case INFO -> logger::info;
                case TRACE -> logger::trace;
                case WARN -> logger::warn;
            };
        }

        private static final BooleanSupplier enabledChecker(Logger logger, Level level) {
            Objects.requireNonNull(logger, "logger must not be null");
            Objects.requireNonNull(level, "level must not be null");
            return switch (level) {
                case DEBUG -> logger::isDebugEnabled;
                case ERROR -> logger::isErrorEnabled;
                case INFO -> logger::isInfoEnabled;
                case TRACE -> logger::isTraceEnabled;
                case WARN -> logger::isWarnEnabled;
            };
        }

        /**
         * Constructs a new {@link Slf4jLoggerWrapper} instance with the specified {@code logger} and the specified
         * {@code level} given.
         *
         * @param logger the logger
         * @param level the level
         */
        public Slf4jLoggerWrapper(Logger logger, Level level) {
            super(logAction(logger, level), enabledChecker(logger, level));
        }

        /**
         * Constructs a new {@link Slf4jLoggerWrapper} instance with the specified {@code logger} given.
         *
         * @param logger the logger
         */
        public Slf4jLoggerWrapper(Logger logger) {
            this(logger, Level.INFO);
        }

        /**
         * Constructs a new {@link Slf4jLoggerWrapper} instance with the specified {@code name} and the specified
         * {@code level} given.
         *
         * @param name the logger name
         * @param level the level
         */
        public Slf4jLoggerWrapper(String name, Level level) {
            this(LoggerFactory.getLogger(name), level);
        }

        /**
         * Constructs a new {@link Slf4jLoggerWrapper} instance with the specified {@code name} given.
         *
         * @param name the logger name
         */
        public Slf4jLoggerWrapper(String name) {
            this(name, Level.INFO);
        }

    }

    /**
     * Some pre-defined log formats.
     *
     * @since 1.1
     *
     * @author MJ Fang
     */
    public enum LogFormat {

        /**
         * The minimal output.
         *
         * <pre>{@code
         * :method :path :status :result-length - :response-time ms
         * }</pre>
         */
        TINY(":method :path :status :result-length - :response-time ms"),

        /**
         * Shorter than default, also including response time.
         *
         * <pre>{@code
         * :remote-addr :remote-user :method :path :http-version :status :result-length - :response-time ms
         * }</pre>
         */
        SHORT(":remote-addr :remote-user :method :path :http-version :status :result-length - :response-time ms"),

        /**
         * Concise output colored by response status for development use. The :status
         * token will be colored green for success codes, red for server error codes,
         * yellow for client error codes, cyan for redirection codes, and uncolored for
         * information codes.
         *
         * <pre>{@code
         * :method :path :status :response-time ms - :result-length
         * }</pre>
         */
        DEV(":method :path :status :response-time ms - :result-length"),

        /**
         * Standard Apache common log output.
         *
         * <pre>{@code
         * :remote-addr - :remote-user [:datetime] \":method :path :http-version\" :status :result-length
         * }</pre>
         */
        COMMON(":remote-addr - :remote-user [:datetime] \":method :path :http-version\" :status :result-length"),

        /**
         * Standard Apache combined log output.
         *
         * <pre>{@code
         * :remote-addr - :remote-user [:datetime] ":method :path :http-version" :status :result-length ":referrer" ":user-agent"
         * }</pre>
         */
        COMBINED(
                ":remote-addr - :remote-user [:datetime] \":method :path :http-version\" :status :result-length \":referrer\" \":user-agent\""),

        /**
         * Basic log output.
         *
         * <pre>{@code
         * :datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length
         * }</pre>
         */
        BASIC(":datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length"),

        /**
         * Another basic log output, make result length human-readable.
         *
         * <pre>{@code
         * :datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length-humanreadable
         * }</pre>
         */
        BASIC2(":datetime :method :path :http-version :remote-addr - :status :response-time ms :result-length-humanreadable");

        private final String pattern;

        LogFormat(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return name().toLowerCase() + "(" + pattern + ")";
        }

    }

    private static final Pattern SYMBOL_PATTERN = Pattern.compile(":[0-9a-z\\-]+");

    private static final Function<HttpResult, String> generateMapperFromPattern(String pattern) {
        Matcher m = SYMBOL_PATTERN.matcher(pattern);
        List<String> txts = new ArrayList<>();
        List<String> symbols = new ArrayList<>();
        int start = 0;
        for (; m.find(start); start = m.end()) {
            symbols.add(m.group());
            if (m.start() > start) {
                txts.add(pattern.substring(start, m.start()));
            } else {
                txts.add("");
            }
        }
        if (symbols.isEmpty()) {
            return r -> pattern;
        }
        var atxt = txts.toArray(String[]::new);
        @SuppressWarnings("unchecked")
        Function<HttpResult, Object>[] symbolMappers = symbols.stream().map(AccessLogger::symbolMapper)
                .toArray(Function[]::new);
        if (pattern.length() > start) {
            String lastTxt = pattern.substring(start);
            return r -> {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < symbolMappers.length; i++) {
                    builder.append(atxt[i]).append(symbolMappers[i].apply(r));
                }
                return builder.append(lastTxt).toString();
            };
        } else {
            return r -> {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < symbolMappers.length; i++) {
                    builder.append(atxt[i]).append(symbolMappers[i].apply(r));
                }
                return builder.toString();
            };
        }
    }

    private static final Function<HttpResult, Object> symbolMapper(String symbol) {
        return switch (symbol) {
            case ":version", ":http-version" -> result -> result.requestContext().protocolVersion();
            case ":method", ":http-method" -> result -> result.requestContext().method();
            case ":uri" -> result -> result.requestContext().uri();
            case ":url", ":path" -> result -> result.requestContext().path();
            case ":raw-path" -> result -> result.requestContext().rawPath();
            case ":query" -> result -> result.requestContext().rawQuery();
            case ":host" -> result -> result.requestContext().headers().get(HttpHeaderNames.HOST, "-");
            case ":content-length" -> result -> result.requestContext().contentLength();
            case ":content" -> result -> {
                try {
                    return result.requestContext().body().toString(CharsetUtil.UTF_8);
                } catch (Exception e) {
                    // ByteBuf may be released before
                    return "-";
                }
            };
            case ":content-type" -> result -> result.requestContext().contentType().orElse("-");
            case ":remote-addr", ":remote-address" -> result -> result.requestContext().remoteAddress();
            case ":remote-user" -> result -> {
                String auth = result.requestContext().headers().get(HttpHeaderNames.AUTHORIZATION);
                if (auth == null || !auth.startsWith("Basic ")) {
                    return "-";
                }
                String base64 = auth.substring(6);
                String basic = new String(Base64.getDecoder().decode(base64.getBytes(CharsetUtil.UTF_8)),
                        CharsetUtil.UTF_8);
                return basic.split(":")[0];
            };
            case ":user-agent" -> result -> result.requestContext().headers().get(HttpHeaderNames.USER_AGENT, "-");
            case ":referrer" -> result -> result.requestContext().headers().get(HttpHeaderNames.REFERER, "-");
            case ":accept" -> result -> result.requestContext().headers().get(HttpHeaderNames.ACCEPT, "-");
            case ":status" -> HttpResult::responseStatus;
            case ":status-code" -> result -> result.responseStatus().codeAsText();
            case ":status-reason" -> result -> result.responseStatus().reasonPhrase();
            case ":result-length" -> result -> result.resultLength() < 0 ? "-" : String.valueOf(result.resultLength());
            case ":result-length-humanreadable" ->
                    result -> result.resultLength() < 0 ? "-" : toHumanReadableSize(result.resultLength());
            case ":iso-local-datetime" ->
                    result -> result.respondedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            case ":datetime" -> result -> result.respondedTime().format(DEFAULT_DATE_TIME);
            case ":iso-local-date" ->
                    result -> result.respondedTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            case ":basic-iso-date" ->
                    result -> result.respondedTime().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
            case ":iso-local-time" ->
                    result -> result.respondedTime().toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
            case ":response-time" ->
                    result -> BigDecimal.valueOf(result.nanoUsed()).divide(T6, 3, RoundingMode.HALF_EVEN);
            default -> result -> symbol;
        };
    }

    private static final String toHumanReadableSize(long length) {
        if (length < 1024) {
            return Long.toString(length);
        } else if (length < 1024 * 10) {
            return String.format("%.2fK", length / 1024.0);
        } else if (length < 1024 * 100) {
            return String.format("%.1fK", length / 1024.0);
        } else if (length < 1024 * 1024) {
            return (length / 1024) + "K";
        } else if (length < 1024 * 1024 * 10) {
            return String.format("%.2fM", length / (1024 * 1024.0));
        } else if (length < 1024 * 1024 * 100) {
            return String.format("%.1fM", length / (1024 * 1024.0));
        } else if (length < 1024 * 1024 * 1024) {
            return (length / (1024 * 1024)) + "M";
        } else if (length < 1024L * 1024 * 1024 * 10) {
            return String.format("%.2fG", length / (1024 * 1024 * 1024.0));
        } else if (length < 1024L * 1024 * 1024 * 100) {
            return String.format("%.1fG", length / (1024 * 1024 * 1024.0));
        } else {
            return (length / (1024 * 1024 * 1024)) + "G";
        }
    }

    private final LoggerWrapper loggerWrapper;
    private final Function<HttpResult, String> logMapper;

    /**
     * Constructs a new {@link AccessLogger} instance with the {@link StdoutLoggerWrapper} and the {@code BASIC} log
     * format.
     */
    public AccessLogger() {
        this(LogFormat.BASIC);
    }

    /**
     * Constructs a new {@link AccessLogger} instance with the {@link StdoutLoggerWrapper} and the specified
     * {@code format} given.
     *
     * @param format the log format
     */
    public AccessLogger(LogFormat format) {
        this(StdoutLoggerWrapper.INSTANCE, format);
    }

    /**
     * Constructs a new {@link AccessLogger} instance with the specified {@link LoggerWrapper} and the specified
     * {@code format} given.
     *
     * @param loggerWrapper the logger wrapper
     * @param format        the log format
     */
    public AccessLogger(LoggerWrapper loggerWrapper, LogFormat format) {
        this(loggerWrapper, Objects.requireNonNull(format, "format must not be null").pattern);
    }

    /**
     * Constructs a new {@link AccessLogger} instance with the {@link StdoutLoggerWrapper} and the specified
     * {@code pattern} given.
     *
     * @param pattern the log format pattern
     */
    public AccessLogger(String pattern) {
        this(StdoutLoggerWrapper.INSTANCE, pattern);
    }

    /**
     * Constructs a new {@link AccessLogger} instance with the specified {@link LoggerWrapper} and the specified
     * {@code pattern} given.
     *
     * @param loggerWrapper the logger wrapper
     * @param pattern       the log format pattern
     */
    public AccessLogger(LoggerWrapper loggerWrapper, String pattern) {
        this(loggerWrapper, generateMapperFromPattern(pattern));
    }

    /**
     * Constructs a new {@link AccessLogger} instance with the specified {@link LoggerWrapper} and the specified
     * {@code logMapper} given.
     *
     * @param loggerWrapper the logger wrapper
     * @param logMapper     the log mapper
     */
    private AccessLogger(LoggerWrapper loggerWrapper, Function<HttpResult, String> logMapper) {
        this.loggerWrapper = loggerWrapper;
        this.logMapper = logMapper;
    }

    String mapLog(HttpResult result) {
        return logMapper.apply(result);
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        return next.doNext(ctx).whenComplete((r, e) -> {
            if (e == null && loggerWrapper.isEnabled()) {
                loggerWrapper.log(mapLog(r));
            }
        });
    }

}
