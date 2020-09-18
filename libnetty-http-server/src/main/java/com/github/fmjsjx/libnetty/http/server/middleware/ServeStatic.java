package com.github.fmjsjx.libnetty.http.server.middleware;

import static com.github.fmjsjx.libnetty.http.server.HttpResponseUtil.*;
import static com.github.fmjsjx.libnetty.http.server.HttpServerUtil.*;
import static io.netty.channel.ChannelFutureListener.*;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.file.StandardOpenOption.*;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fmjsjx.libnetty.http.server.DefaultHttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerHandler;
import com.github.fmjsjx.libnetty.http.server.MimeTypeUtil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.AsciiString;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * A {@link Middleware} serves static resources.
 * 
 * @since 1.1
 *
 * @author MJ Fang
 * 
 * @see Middleware
 * @see MiddlewareChain
 */
public class ServeStatic implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(ServeStatic.class);

    private static final int chunkSize;

    static {
        chunkSize = SystemPropertyUtil.getInt("libnetty.http.server.middleware.static.chunkSize", 8192);
        logger.debug("-Dlibnetty.http.server.middleware.static.chunkSize: {}", chunkSize);
    }

    private static final LinkedHashMap<String, String> toLinkedHashN(String... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("number of arguments must be even");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(kvs.length);
        for (int i = 0; i < kvs.length / 2; i++) {
            int index = i * 2;
            map.put(kvs[index], kvs[index + 1]);
        }
        return map;
    }

    private final List<StaticLocationMapping> mappings;
    private final List<String> indexes;
    private final boolean showHidden;
    private final boolean redirectDirectory;
    private final AsciiString cacheControl;
    private final boolean etagEnabled;
    private final boolean lastModifiedEnabled;
    private final long maxAge;
    private final EtagGenerator etagGenerator;
    private final Consumer<HttpHeaders> addHeaders;

    /**
     * Constructs a new {@link ServeStatic} with the specified {@code path} and
     * {@code locationMapping}.
     * 
     * @param path            the URI prefix path
     * @param locationMapping the location mapping of the resources
     */
    public ServeStatic(String path, String locationMapping) {
        this(path, locationMapping, null);
    }

    /**
     * Constructs a new {@link ServeStatic} with the specified {@code path},
     * {@code locationMapping} and {@link Options}.
     * 
     * @param path            the URI prefix path
     * @param locationMapping the location mapping of the resources
     * @param options         the options
     */
    public ServeStatic(String path, String locationMapping, Options options) {
        this(Collections.singletonMap(path, locationMapping), Optional.ofNullable(options));
    }

    /**
     * Constructs a new {@link ServeStatic} with the multiply {@code path}s,
     * {@code locationMapping}s and the specified {@link Options}.
     * 
     * @param path1            the first URI prefix path
     * @param locationMapping1 the first location mapping of the resources
     * @param path2            the second URI prefix path
     * @param locationMapping2 the second location mapping of the resources
     * @param options          the options
     */
    public ServeStatic(String path1, String locationMapping1, String path2, String locationMapping2, Options options) {
        this(toLinkedHashN(path1, locationMapping1, path2, locationMapping2), options);
    }

    /**
     * Constructs a new {@link ServeStatic} with the multiply
     * {@code locationMapping}s and the specified {@link Options}.
     * 
     * @param options              the options
     * @param pathLocationMappings the array contains the URI prefix paths and the
     *                             location mappings, the length of array must be
     *                             even
     */
    public ServeStatic(Options options, String... pathLocationMappings) {
        this(toLinkedHashN(pathLocationMappings), options);
    }

    /**
     * Constructs a new {@link ServeStatic} with the multiply
     * {@code locationMapping}s and the specified {@link Options}.
     * 
     * @param locationMappings the map contains the URI prefix paths and the
     *                         location mappings
     * @param options          the options
     */
    public ServeStatic(LinkedHashMap<String, String> locationMappings, Options options) {
        this(locationMappings, Optional.ofNullable(options));
    }

    private ServeStatic(Map<String, String> locationMappings, Optional<Options> options) {
        Options opt = options.orElseGet(Options::new);
        mappings = locationMappings.entrySet().stream().map(StaticLocationMapping::new).collect(Collectors.toList());
        logger.debug("ServeStatic: locationMappings={}, options={}", mappings, opt);
        this.indexes = opt.indexes;
        this.showHidden = opt.showHidden;
        this.redirectDirectory = opt.redirectDirectory;
        this.cacheControl = opt.cacheControl();
        this.etagEnabled = opt.etag;
        this.lastModifiedEnabled = opt.lastModified;
        this.maxAge = Math.max(0, // must >= 0
                Arrays.stream(cacheControl.split(',')).map(s -> s.trim()).filter(s -> s.startsWith("max-age="))
                        .mapToInt(s -> s.parseInt(8, s.length())).findFirst().orElse(0));
        this.etagGenerator = opt.etagGenerator;
        this.addHeaders = opt.addHeaders;
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        String path = ctx.path();
        boolean isGet = HttpMethod.GET.equals(ctx.method());
        List<StaticLocationMapping> mappings = this.mappings;
        L1: for (StaticLocationMapping mapping : mappings) {
            String uri = mapping.uri;
            if (!path.startsWith(uri)) {
                continue L1;
            }
            if (!isGet) {
                return sendMethodNotAllowed(ctx);
            }
            Path p = Paths.get(mapping.location, path.substring(uri.length()));
            if (!Files.exists(p)) {
                continue L1;
            }
            logger.debug("Converted location path {} => {}", path, p);
            if (Files.isDirectory(p)) {
                boolean hitRedirect = !path.endsWith("/");
                if (hitRedirect && redirectDirectory) {
                    String rawQuery = ctx.rawQuery();
                    if (StringUtil.isNullOrEmpty(rawQuery)) {
                        return sendRedirect(ctx, path + "/");
                    } else {
                        return sendRedirect(ctx, path + "/?" + rawQuery);
                    }
                } else {
                    boolean exists = false;
                    for (String index : indexes) {
                        Path fp = p.resolve(index);
                        if (Files.isRegularFile(fp)) {
                            p = fp;
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        continue L1;
                    }
                }
            } else if (!Files.isRegularFile(p)) {
                continue L1;
            }
            FullHttpRequest request = ctx.request();
            HttpVersion version = request.protocolVersion();
            HttpHeaders headers = request.headers();
            try {
                if (Files.isHidden(p) && !showHidden) {
                    return next.doNext(ctx);
                }
                BasicFileAttributes fileAttrs = Files.readAttributes(p, BasicFileAttributes.class);
                Instant now = Instant.now();
                String etag = etagEnabled ? etagGenerator.generate(p, fileAttrs) : null;
                Instant lastModified = lastModifiedEnabled ? fileAttrs.lastModifiedTime().toInstant() : null;
                long maxAge = this.maxAge;
                Instant expires = maxAge > 0 ? now.plusSeconds(maxAge) : now;
                boolean keepAlive = HttpUtil.isKeepAlive(request);
                if (etagEnabled) {
                    List<String> ifNoneMatches = headers.getAll(IF_NONE_MATCH);
                    if (!ifNoneMatches.isEmpty()) {
                        headers.remove(IF_MODIFIED_SINCE); // skip header if-modified-since
                        if (ifNoneMatches.stream().anyMatch(etag::equals)) { // not modified
                            FullHttpResponse response = create(version, NOT_MODIFIED, keepAlive);
                            setDateAndCacheHeaders(now, etag, lastModified, expires, response);
                            return sendResponse(ctx, response, 0, keepAlive);
                        }
                    }
                }
                if (lastModifiedEnabled) {
                    Long ims = headers.getTimeMillis(IF_MODIFIED_SINCE);
                    if (ims != null && ims.longValue() >= lastModified.toEpochMilli()) { // not modified
                        FullHttpResponse response = create(version, NOT_MODIFIED, keepAlive);
                        setDateAndCacheHeaders(now, etag, lastModified, expires, response);
                        return sendResponse(ctx, response, 0, keepAlive);
                    }
                }
                long contentLength = fileAttrs.size();
                HttpResponse response = new DefaultHttpResponse(version, OK);
                HttpUtil.setKeepAlive(response, keepAlive);
                HttpUtil.setContentLength(response, contentLength);
                response.headers().set(CONTENT_TYPE, MimeTypeUtil.probeContentType(p));
                setDateAndCacheHeaders(now, etag, lastModified, expires, response);
                CompletableFuture<HttpResult> future = new CompletableFuture<>();
                ChannelFutureListener[] cbs = new ChannelFutureListener[] { cf -> {
                    if (cf.isSuccess()) {
                        future.complete(new DefaultHttpResult(ctx, contentLength, OK));
                    } else if (cf.cause() != null) {
                        future.completeExceptionally(cf.cause());
                    }
                }, keepAlive ? HttpServerHandler.READ_NEXT : CLOSE };
                Channel channel = ctx.channel();
                channel.write(response);
                FileChannel file = FileChannel.open(p, READ);
                if (channel.pipeline().get(SslHandler.class) == null) {
                    // Use zero-copy file transfer
                    channel.write(new DefaultFileRegion(file, 0, contentLength));
                    // Write the end marker.
                    channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListeners(cbs);
                } else {
                    ChunkedNioFile chunkedFile = new ChunkedNioFile(file, 0, contentLength, chunkSize);
                    // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                    channel.writeAndFlush(new HttpChunkedInput(chunkedFile)).addListeners(cbs);
                }
                return future;
            } catch (IOException e) {
                // skip any IO exception
                return next.doNext(ctx);
            }
        }
        // End of loop
        return next.doNext(ctx);
    }

    private void setDateAndCacheHeaders(Instant date, String etag, Instant lastModified, Instant expires,
            HttpResponse response) {
        HttpHeaders headers = response.headers();
        if (!cacheControl.isEmpty()) {
            headers.add(CACHE_CONTROL, cacheControl);
        }
        headers.set(DATE, Date.from(date));
        if (etagEnabled) {
            headers.set(ETAG, etag);
        }
        headers.set(EXPIRES, Date.from(expires));
        if (lastModifiedEnabled) {
            headers.set(LAST_MODIFIED, Date.from(lastModified));
        }
        addCustomHeaders(headers);
    }

    private void addCustomHeaders(HttpHeaders headers) {
        Consumer<HttpHeaders> addHeaders = this.addHeaders;
        if (addHeaders != null) {
            addHeaders.accept(headers);
        }
    }

    private static final class StaticLocationMapping {

        private final String uri;
        private final String location;

        private StaticLocationMapping(Entry<String, String> entry) {
            this(entry.getKey(), entry.getValue());
        }

        private StaticLocationMapping(String uri, String location) {
            this.uri = validateUri(uri);
            this.location = validateLocation(location);
        }

        private static final String validateLocation(String location) {
            return Objects.requireNonNull(location, "location must not be null");
        }

        private static final String validateUri(String uri) {
            Objects.requireNonNull(uri, "uri must not be null");
            if (!uri.endsWith("/")) {
                uri = uri + "/";
            }
            return uri;
        }

        @Override
        public String toString() {
            return "(" + uri + " => " + location + ")";
        }

    }

    /**
     * Function to generates {@code E-TAG}s.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     */
    @FunctionalInterface
    public interface EtagGenerator {

        /**
         * The basic generator.
         * <p>
         * Algorithm:
         * 
         * <pre>
         * "${hex(file.lastModifiedInSeconds)}-${hex(file.size)}"
         * </pre>
         */
        static EtagGenerator BASIC = (p, a) -> {
            long size = a.size();
            long modifiedTime = a.lastModifiedTime().to(TimeUnit.SECONDS);
            return "\"" + Long.toHexString(modifiedTime) + "-" + Long.toHexString(size) + "\"";
        };

        /**
         * Returns {@code true} if is weak validator, otherwise {@code false}
         * 
         * @return {@code true} if is weak validator, otherwise {@code false}
         */
        default boolean week() {
            return false;
        }

        /**
         * Generate the {@code E-TAG}.
         * 
         * @param path           the path of the file
         * @param fileAttributes the basic attributes associated with the file
         * 
         * @return the {@code E-TAG} string
         * 
         * @throws IOException if any IO error occurs
         */
        String generate(Path path, BasicFileAttributes fileAttributes) throws IOException;

    }

    /**
     * The options of {@link ServeStatic}.
     * 
     * @since 1.1
     *
     * @author MJ Fang
     */
    public static class Options {

        private List<String> indexes = Arrays.asList("index.html");
        private boolean showHidden = false;
        private boolean redirectDirectory = true;
        private String cacheControl = "no-cache";
        private boolean etag = true;
        private EtagGenerator etagGenerator = EtagGenerator.BASIC;
        private boolean lastModified = true;
        private Consumer<HttpHeaders> addHeaders;

        /**
         * Set index.
         * 
         * @param index the filename of the index
         * @return this {@code Options}
         */
        public Options index(String index) {
            return indexes(index);
        }

        /**
         * Set multiply indexes.
         * 
         * @param indexes the array contains the filenames of the indexes
         * @return this {@code Options}
         */
        public Options indexes(String... indexes) {
            if (indexes.length == 0) {
                this.indexes = Collections.emptyList();
            } else {
                this.indexes = Arrays.stream(indexes).map(String::trim).filter(s -> s.length() > 0).map(String::intern)
                        .collect(Collectors.toList());
            }
            return this;
        }

        /**
         * Allow clients to access hidden files.
         * <p>
         * The default is {@code false}.
         * 
         * @return this {@code Options}
         */
        public Options showHidden() {
            return showHidden(true);
        }

        /**
         * Set if clients are allowed to access hidden files.
         * <p>
         * The default is {@code false}.
         * 
         * @param showHidden {@code true} to allow clients to access hidden files
         * @return this {@code Options}
         */
        public Options showHidden(boolean showHidden) {
            this.showHidden = showHidden;
            return this;
        }

        /**
         * Set if redirect to trailing "/" when the pathname is a directory or not.
         * <p>
         * The default is {@code true}.
         * 
         * @param redirectDirectory {@code false} to disable
         * @return this {@code Options}
         */
        public Options redirectDirectory(boolean redirectDirectory) {
            this.redirectDirectory = redirectDirectory;
            return this;
        }

        /**
         * Set cacheControl.
         * <p>
         * The default is {@code "no-cache"}.
         * 
         * @param cacheControl the value of the cache-control
         * @return this {@code Options}
         */
        public Options cacheControl(String cacheControl) {
            this.cacheControl = Optional.ofNullable(cacheControl).map(String::trim).orElse(null);
            return this;
        }

        private AsciiString cacheControl() {
            if (StringUtil.isNullOrEmpty(cacheControl)) {
                return AsciiString.EMPTY_STRING;
            } else {
                return AsciiString.cached(cacheControl.intern());
            }
        }

        /**
         * Set if {@code E-TAG} is enabled or not.
         * <p>
         * The default is {@code true}.
         * 
         * @param etag {@code false} to disable {@code E-TEG} feature
         * @return this {@code Options}
         */
        public Options etag(boolean etag) {
            this.etag = etag;
            return this;
        }

        /**
         * Set the function to generate {@code E-TAG}s.
         * <p>
         * The default is {@link EtagGenerator#BASIC}.
         * 
         * @param etagGenerator the function to generate {@code E-TAG}s
         * @return this {@code Options}
         */
        public Options etagGenerator(EtagGenerator etagGenerator) {
            this.etagGenerator = etagGenerator;
            return this;
        }

        /**
         * Set if header {@code last-modified} is enabled or not.
         * <p>
         * The default is {@code true}.
         * 
         * @param lastModified {@code false} to disable {@code last-modified} header
         * @return this {@code Options}
         */
        public Options lastModified(boolean lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * Set the function set custom headers on response.
         * 
         * @param addHeaders the function set custom headers on response
         * @return this {@code Options}
         */
        public Options addHeaders(Consumer<HttpHeaders> addHeaders) {
            this.addHeaders = addHeaders;
            return this;
        }

        @Override
        public String toString() {
            return "ServeStatic.Options[indexes=" + indexes + ", showHidden=" + showHidden + ", redirectDirectory="
                    + redirectDirectory + ", cacheControl=" + cacheControl + ", etag=" + etag + ", etagGenerator="
                    + etagGenerator + ", lastModified=" + lastModified + ", addHeaders=" + addHeaders + "]";
        }
    }

}
