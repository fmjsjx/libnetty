package com.github.fmjsjx.libnetty.http.server.middleware;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
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

    @FunctionalInterface
    public interface LoggerWrapper {

        void log(String content);

        default boolean isEnabled() {
            return true;
        }

    }

    public static final class StdoutLoggerWrapper extends FunctionalLoggerWrapper {

        private static final StdoutLoggerWrapper INSTANCE = new StdoutLoggerWrapper();

        public static final StdoutLoggerWrapper getInstance() {
            return INSTANCE;
        }

        public StdoutLoggerWrapper() {
            super(System.out::println);
        }

    }

    public static final class StderrLoggerWrapper extends FunctionalLoggerWrapper {

        private static final StderrLoggerWrapper INSTANCE = new StderrLoggerWrapper();

        public static final StderrLoggerWrapper getInstance() {
            return INSTANCE;
        }

        public StderrLoggerWrapper() {
            super(System.err::println);
        }

    }

    public static class FunctionalLoggerWrapper implements LoggerWrapper {

        private final Consumer<String> logAction;
        private final BooleanSupplier enabledChecker;

        public FunctionalLoggerWrapper(Consumer<String> logAction, BooleanSupplier enabledChecker) {
            this.logAction = Objects.requireNonNull(logAction, "logAction must not be null");
            this.enabledChecker = Objects.requireNonNull(enabledChecker, "enabledChecker must not be null");
        }

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

    public static final class Slf4jLoggerWrapper extends FunctionalLoggerWrapper {

        private static final Consumer<String> logAction(Logger logger, Level level) {
            Objects.requireNonNull(logger, "logger must not be null");
            Objects.requireNonNull(level, "level must not be null");
            switch (level) {
            case DEBUG:
                return logger::debug;
            case ERROR:
                return logger::error;
            case INFO:
                return logger::info;
            case TRACE:
                return logger::trace;
            case WARN:
                return logger::warn;
            default:
                // can't reach this line
                return logger::info;
            }
        }

        private static final BooleanSupplier enabledChecker(Logger logger, Level level) {
            Objects.requireNonNull(logger, "logger must not be null");
            Objects.requireNonNull(level, "level must not be null");
            switch (level) {
            case DEBUG:
                return logger::isDebugEnabled;
            case ERROR:
                return logger::isErrorEnabled;
            case INFO:
                return logger::isInfoEnabled;
            case TRACE:
                return logger::isTraceEnabled;
            case WARN:
                return logger::isWarnEnabled;
            default:
                // can't reach this line
                return logger::isInfoEnabled;
            }
        }

        public Slf4jLoggerWrapper(Logger logger, Level level) {
            super(logAction(logger, level), enabledChecker(logger, level));
        }

        public Slf4jLoggerWrapper(Logger logger) {
            this(logger, Level.INFO);
        }

    }

    private static final Pattern SYMBOL_PATTERN = Pattern.compile(":[0-9a-z\\-]+");

    static final Function<HttpResult, String> generateMapperFromPattern(String pattern) {
        Matcher m = SYMBOL_PATTERN.matcher(pattern);
        int start = 0;
        if (m.find(start)) {
            BiConsumer<HttpResult, StringBuilder> appender;
            {
                Function<HttpResult, Object> symbolMapper = symbolMapperForTest(m.group());
                if (m.start() > start) {
                    String txt = pattern.substring(start, m.start());
                    appender = (r, b) -> b.append(txt).append(symbolMapper.apply(r));
                } else {
                    appender = (r, b) -> b.append(symbolMapper.apply(r));
                }
            }
            for (start = m.end(); m.find(start); start = m.end()) {
                Function<HttpResult, Object> symbolMapper = symbolMapperForTest(m.group());
                if (m.start() > start) {
                    String txt = pattern.substring(start, m.start());
                    appender = appender.andThen((r, b) -> b.append(txt).append(symbolMapper.apply(r)));
                } else {
                    appender = appender.andThen((r, b) -> b.append(symbolMapper.apply(r)));
                }
            }
            BiConsumer<HttpResult, StringBuilder> sa = appender;
            if (pattern.length() > start) {
                String txt = pattern.substring(start);
                return r -> {
                    StringBuilder builder = new StringBuilder();
                    sa.accept(r, builder);
                    return builder.append(txt).toString();
                };
            } else {
                return r -> {
                    StringBuilder builder = new StringBuilder();
                    sa.accept(r, builder);
                    return builder.toString();
                };
            }
        } else {
            return r -> pattern;
        }
    }

    static final Function<HttpResult, String> generateMapperFromPattern2(String pattern) {
        Matcher m = SYMBOL_PATTERN.matcher(pattern);
        List<String> txts = new ArrayList<String>();
        List<String> symbols = new ArrayList<String>();
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
        String[] atxt = txts.toArray(new String[txts.size()]);
        @SuppressWarnings("unchecked")
        Function<HttpResult, Object>[] symbolMappers = symbols.stream().map(AccessLogger::symbolMapperForTest)
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

    static final Function<HttpResult, String> generateMapperFromPattern3(String pattern) {
        Matcher m = SYMBOL_PATTERN.matcher(pattern);
        int start = 0;
        List<BiConsumer<HttpResult, StringBuilder>> appenders = new ArrayList<BiConsumer<HttpResult, StringBuilder>>();
        for (; m.find(start); start = m.end()) {
            Function<HttpResult, Object> symbolMapper = symbolMapperForTest(m.group());
            if (m.start() > start) {
                String txt = pattern.substring(start, m.start());
                appenders.add((r, b) -> b.append(txt).append(symbolMapper.apply(r)));
            } else {
                appenders.add((r, b) -> b.append(symbolMapper.apply(r)));
            }
        }
        if (appenders.isEmpty()) {
            return r -> pattern;
        }
        if (pattern.length() > start) {
            String lastTxt = pattern.substring(start);
            return r -> {
                StringBuilder builder = new StringBuilder();
                for (BiConsumer<HttpResult, StringBuilder> appender : appenders) {
                    appender.accept(r, builder);
                }
                return builder.append(lastTxt).toString();
            };
        }
        return r -> {
            StringBuilder builder = new StringBuilder();
            for (BiConsumer<HttpResult, StringBuilder> appender : appenders) {
                appender.accept(r, builder);
            }
            return builder.toString();
        };
    }

    public static void main(String[] args) {
        String p = "Hello :datetime :method :path :version :remote-address - :status :response-time ms :result-length World!";
        Function<HttpResult, String> m1 = generateMapperFromPattern(p);
        Function<HttpResult, String> m2 = generateMapperFromPattern2(p);
        Function<HttpResult, String> m3 = generateMapperFromPattern3(p);
        System.out.println(m1.apply(null));
        System.out.println(m2.apply(null));
        System.out.println(m3.apply(null));
        System.out.println("-- warm --");
        int count = 10_000_000;
        for (int i = 0; i < count; i++) {
            m1.apply(null);
            m2.apply(null);
            m3.apply(null);
        }
        System.out.println("-- start --");
        long[] ns = new long[10];
        long[] ns2 = new long[ns.length];
        long[] ns3 = new long[ns.length];
        long n;
        for (int i = 0; i < ns.length; i++) {
            n = System.nanoTime();
            for (int j = 0; j < count; j++) {
                m1.apply(null);
            }
            ns[i] = System.nanoTime() - n;

            n = System.nanoTime();
            for (int j = 0; j < count; j++) {
                m2.apply(null);
            }
            ns2[i] = System.nanoTime() - n;

            n = System.nanoTime();
            for (int j = 0; j < count; j++) {
                m3.apply(null);
            }
            ns3[i] = System.nanoTime() - n;
        }
        System.out.println("-- result --");
        Arrays.sort(ns);
        Arrays.sort(ns2);
        Arrays.sort(ns3);
        for (long n1 : ns) {
            System.out.println(n1);
        }
        System.out.println("-- method 1 --");

        for (long n2 : ns2) {
            System.out.println(n2);
        }
        System.out.println("-- method 2 --");

        for (long n3 : ns3) {
            System.out.println(n3);
        }
        System.out.println("-- method 3 --");
    }

    private static final Function<HttpResult, Object> symbolMapperForTest(String symbol) {
        return result -> symbol;
    }

    private static final Function<HttpResult, Object> symbolMapper(String symbol) {
        switch (symbol) {
        case ":version":
            return result -> result.requestContext().version();
        case ":method":
            return result -> result.requestContext().method();
        case ":path":
            return result -> result.requestContext().path();
        case ":raw-path":
            return result -> result.requestContext().rawPath();
        case ":query":
            return result -> result.requestContext().rawQuery();
        case ":host":
            return result -> result.requestContext().headers().get(HttpHeaderNames.HOST, "-");
        case ":content-length":
            return result -> result.requestContext().contentLength();
        case ":content":
            return result -> {
                try {
                    return result.requestContext().body().toString(CharsetUtil.UTF_8);
                } catch (Exception e) {
                    // ByteBuf may be released before
                    return "-";
                }
            };
        case ":content-type":
            return result -> result.requestContext().contentType().orElse("-");
        case ":remote-address":
            return result -> result.requestContext().remoteAddress();
        case ":user-agent":
            return result -> result.requestContext().headers().get(HttpHeaderNames.USER_AGENT, "-");
        case ":referrer":
            return result -> result.requestContext().headers().get(HttpHeaderNames.REFERER, "-");
        case ":accept":
            return result -> result.requestContext().headers().get(HttpHeaderNames.ACCEPT, "-");
        case ":status":
            return result -> result.responseStatus();
        case ":status-code":
            return result -> result.responseStatus().codeAsText();
        case ":status-reason":
            return result -> result.responseStatus().reasonPhrase();
        case ":result-length":
            return HttpResult::resultLength;
        case ":result-length-humanreadable":
            return result -> toHumanReadableSize(result.resultLength());
        case ":iso-local-datetime":
            return result -> result.respondedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        case ":datetime":
            return result -> result.respondedTime().format(DEFAULT_DATE_TIME);
        case ":iso-local-date":
            return result -> result.respondedTime().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        case ":basic-iso-date":
            return result -> result.respondedTime().toLocalDate().format(DateTimeFormatter.BASIC_ISO_DATE);
        case ":iso-local-time":
            return result -> result.respondedTime().toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
        case ":response-time":
            return result -> BigDecimal.valueOf(result.nanoUsed()).divide(T6, 3, RoundingMode.HALF_EVEN);
        default:
            return result -> symbol;
        }
    }

    private static final BigDecimal T6 = BigDecimal.valueOf(1_000_000L);

    private static final BigDecimal toMillis(long val) {
        return BigDecimal.valueOf(val).divide(T6, 3, RoundingMode.HALF_EVEN);
    }

    private static final String toHumanReadableSize(long length) {
        if (length < 1024) {
            return Long.toString(length);
        } else if (length < 1024 * 10) {
            return String.format("%.2fK", length / 1024.0);
        } else if (length < 1024 * 1024) {
            return (length / 1024) + "K";
        } else if (length < 1024 * 1024 * 10) {
            return String.format("%.2fM", length / (1024 * 1024.0));
        } else if (length < 1024 * 1024 * 1024) {
            return (length / (1024 * 1024)) + "M";
        } else if (length < 1024 * 1024 * 1024 * 10) {
            return String.format("%.2fG", length / (1024 * 1024 * 1024.0));
        } else {
            return (length / (1024 * 1024 * 1024)) + "G";
        }
    }

    private final LoggerWrapper loggerWrapper;
    private final Function<HttpResult, String> logMapper;

    // TODO other public constructors

    public AccessLogger(String pattern) {
        this(StdoutLoggerWrapper.INSTANCE, generateMapperFromPattern(pattern));
    }

    private AccessLogger(LoggerWrapper loggerWrapper, Function<HttpResult, String> logMapper) {
        this.loggerWrapper = loggerWrapper;
        this.logMapper = logMapper;
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        return next.doNext(ctx).whenComplete((r, e) -> {
            if (e != null && loggerWrapper.isEnabled()) {
                loggerWrapper.log(logMapper.apply(r));
            }
        });
    }

}
