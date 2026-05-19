package com.github.fmjsjx.libnetty.http.server.middleware;

import com.github.fmjsjx.libnetty.http.server.DefaultHttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;
import com.github.fmjsjx.libnetty.http.server.HttpServerHandler;
import com.github.fmjsjx.libnetty.http.server.util.MimeTypesUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.DateFormatter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.AsciiString;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.file.StandardOpenOption.READ;

/**
 * A {@link Middleware} serves static resources.
 *
 * @author MJ Fang
 * @see Middleware
 * @see MiddlewareChain
 * @since 1.1
 */
public class ServeStatic implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(ServeStatic.class);

    private static final int DEFAULT_CHUNK_SIZE;

    static {
        DEFAULT_CHUNK_SIZE = SystemPropertyUtil.getInt("libnetty.http.server.middleware.static.chunkSize", 8192);
        logger.debug("-Dlibnetty.http.server.middleware.static.chunkSize: {}", DEFAULT_CHUNK_SIZE);
    }

    private static final LinkedHashMap<String, String> toLinkedHashN(String... kvs) {
        if (kvs.length % 2 != 0) {
            throw new IllegalArgumentException("number of arguments must be even");
        }
        var map = new LinkedHashMap<String, String>(kvs.length);
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
    private final int chunkSize;
    private final boolean allowPost;
    private final boolean rangeEnabled;

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
        this.chunkSize = opt.chunkSize;
        this.allowPost = opt.allowPost;
        this.rangeEnabled = opt.range;
    }

    @Override
    public CompletionStage<HttpResult> apply(HttpRequestContext ctx, MiddlewareChain next) {
        String path = ctx.path();
        boolean isGet = HttpMethod.GET.equals(ctx.method());
        boolean isHead = HttpMethod.HEAD.equals(ctx.method());
        var methodNotAllow = !isGet && !isHead && !allowPost;
        List<Range> ranges = null;
        var rangeHeader = ctx.headers().get(RANGE);
        logger.debug("rangeHeader: {}", rangeHeader);
        var isRange = rangeEnabled && rangeHeader != null;
        if (isRange) {
            ranges = parseRange(rangeHeader);
        }
        List<StaticLocationMapping> mappings = this.mappings;
        for (StaticLocationMapping mapping : mappings) {
            String uri = mapping.uri;
            if (!path.startsWith(uri)) {
                continue;
            }
            Path p = Paths.get(mapping.location, path.substring(uri.length()));
            if (!Files.exists(p)) {
                continue;
            }
            logger.debug("Converted location path {} => {}", path, p);
            if (Files.isDirectory(p)) {
                boolean hitRedirect = !path.endsWith("/");
                if (hitRedirect && redirectDirectory) {
                    if (methodNotAllow) {
                        return ctx.simpleRespond(METHOD_NOT_ALLOWED);
                    }
                    return ctx.sendRedirect(path + "/", addHeaders);
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
                        continue;
                    }
                }
            } else if (!Files.isRegularFile(p)) {
                continue;
            }
            FullHttpRequest request = ctx.request();
            HttpVersion version = request.protocolVersion();
            HttpHeaders headers = request.headers();
            try {
                if (Files.isHidden(p) && !showHidden) {
                    return next.doNext(ctx);
                }
                if (methodNotAllow) {
                    return ctx.simpleRespond(METHOD_NOT_ALLOWED);
                }
                BasicFileAttributes fileAttrs = Files.readAttributes(p, BasicFileAttributes.class);
                Instant now = Instant.now();
                String etag = etagEnabled ? etagGenerator.generate(p, fileAttrs) : null;
                Instant lastModified = lastModifiedEnabled ? fileAttrs.lastModifiedTime().toInstant() : null;
                long maxAge = this.maxAge;
                Instant expires = maxAge > 0 ? now.plusSeconds(maxAge) : now;
                boolean keepAlive = HttpUtil.isKeepAlive(request);
                var fileSize = fileAttrs.size();
                if (isRange) {
                    var response = checkRange(ranges, ctx, now, etag, lastModified, expires, fileSize);
                    if (response != null) {
                        return ctx.sendResponse(response, 0);
                    }
                    var ifRange = ctx.headers().get(IF_RANGE);
                    if (!StringUtil.isNullOrEmpty(ifRange)) {
                        if (ifRange.charAt(0) == '"' && etag != null) { // use etag
                            if (!etag.equals(ifRange)) {
                                isRange = false;
                            }
                        } else {
                            Date date = DateFormatter.parseHttpDate(ifRange);
                            if (date != null && lastModified != null) {
                                if (date.toInstant().isBefore(lastModified)) {
                                    isRange = false;
                                }
                            }
                        }
                    }
                }
                if (etag != null) {
                    List<String> ifNoneMatches = headers.getAll(IF_NONE_MATCH);
                    if (!ifNoneMatches.isEmpty()) {
                        headers.remove(IF_MODIFIED_SINCE); // skip header if-modified-since
                        if (ifNoneMatches.stream().anyMatch(etag::equals)) { // not modified
                            FullHttpResponse response = ctx.responseFactory().createFull(NOT_MODIFIED);
                            setDateAndCacheHeaders(now, etag, lastModified, expires, response.headers());
                            return ctx.sendResponse(response, 0);
                        }
                    }
                }
                if (lastModified != null) {
                    Long ims = headers.getTimeMillis(IF_MODIFIED_SINCE);
                    if (ims != null && ims >= lastModified.toEpochMilli()) { // not modified
                        FullHttpResponse response = ctx.responseFactory().createFull(NOT_MODIFIED);
                        setDateAndCacheHeaders(now, etag, lastModified, expires, response.headers());
                        return ctx.sendResponse(response, 0);
                    }
                }
                var contentType = MimeTypesUtil.probeContentType(p);
                if (isRange) {
                    var response = new DefaultHttpResponse(version, PARTIAL_CONTENT);
                    HttpUtil.setKeepAlive(response, keepAlive);
                    assert ranges != null;
                    if (ranges.size() == 1) {
                        var range = ranges.getFirst().normalize(fileSize);
                        response.headers().set(CONTENT_RANGE, "bytes " + range.start() + "-" + range.end() + "/" + fileSize);
                        response.headers().set(CONTENT_TYPE, contentType);
                        setDateAndCacheHeaders(now, etag, lastModified, expires, response.headers());
                        response.headers().set(ACCEPT_RANGES, BYTES);
                        var contentLength = range.length();
                        var offset = range.start();
                        return sendResponse(ctx, response, contentLength, isHead, fileSize, keepAlive, p, offset);
                    }
                    // multipart/byteranges
                    // TODO implement multipart/byteranges feature in next version
                }
                var response = new DefaultHttpResponse(version, OK);
                HttpUtil.setKeepAlive(response, keepAlive);
                HttpUtil.setContentLength(response, fileSize);
                if (rangeHeader != null) {
                    response.headers().set(ACCEPT_RANGES, NONE);
                }
                response.headers().set(CONTENT_TYPE, contentType);
                setDateAndCacheHeaders(now, etag, lastModified, expires, response.headers());
                return sendResponse(ctx, response, fileSize, isHead, fileSize, keepAlive, p, 0);
            } catch (IOException e) {
                // skip any IO exception
                return next.doNext(ctx);
            }
        }
        // End of loop
        return next.doNext(ctx);
    }

    private CompletableFuture<HttpResult> sendResponse(HttpRequestContext ctx, DefaultHttpResponse response,
                                                       long contentLength, boolean isHead, long fileSize,
                                                       boolean keepAlive, Path path, long offset) throws IOException {
        HttpUtil.setContentLength(response, contentLength);
        CompletableFuture<HttpResult> future = new CompletableFuture<>();
        var resultLength = isHead ? 0 : fileSize;
        ChannelFutureListener[] cbs = new ChannelFutureListener[]{cf -> {
            if (cf.isSuccess()) {
                future.complete(new DefaultHttpResult(ctx, resultLength, OK));
            } else if (cf.cause() != null) {
                future.completeExceptionally(cf.cause());
            }
        }, keepAlive ? HttpServerHandler.READ_NEXT : CLOSE};
        Channel channel = ctx.channel();
        var noSsl = channel.pipeline().get(SslHandler.class) == null;
        if (noSsl) {
            // disable compression feature
            response.headers().set(CONTENT_ENCODING, IDENTITY);
        }
        channel.write(response);
        if (isHead) {
            channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListeners(cbs);
        } else {
            FileChannel file = FileChannel.open(path, READ);
            if (noSsl && supportZeroCopyTransfer(channel)) {
                // Use zero-copy file transfer
                channel.write(new DefaultFileRegion(file, offset, contentLength));
                // Write the end marker.
                channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListeners(cbs);
            } else {
                ChunkedNioFile chunkedFile = new ChunkedNioFile(file, offset, contentLength, chunkSize);
                // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                channel.writeAndFlush(new HttpChunkedInput(chunkedFile)).addListeners(cbs);
            }
        }
        return future;
    }

    private FullHttpResponse checkRange(List<Range> ranges, HttpRequestContext ctx, Instant now, String etag,
                                        Instant lastModified, Instant expires, long fileSize) {
        if (ranges == null) {
            var response = ctx.responseFactory().createFull(REQUESTED_RANGE_NOT_SATISFIABLE);
            setDateAndCacheHeaders(now, etag, lastModified, expires, response.headers());
            return response;
        }
        var anyInvalid = false;
        for (var range : ranges) {
            if (range.invalid() || range.outOfRange(fileSize)) {
                anyInvalid = true;
                break;
            }
        }
        if (anyInvalid) {
            var response = ctx.responseFactory().createFull(REQUESTED_RANGE_NOT_SATISFIABLE);
            response.headers().add(CONTENT_RANGE, "bytes */" + fileSize);
            setDateAndCacheHeaders(now, etag, lastModified, expires, response.headers());
            return response;
        }
        return null;
    }

    private void setDateAndCacheHeaders(Instant date, String etag, Instant lastModified, Instant expires,
                                        HttpHeaders headers) {
        addCustomHeaders(headers);
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
    }

    private void addCustomHeaders(HttpHeaders headers) {
        Consumer<HttpHeaders> addHeaders = this.addHeaders;
        if (addHeaders != null) {
            addHeaders.accept(headers);
        }
    }

    private boolean supportZeroCopyTransfer(Channel channel) {
        // Http2StreamChannel doesn't support ByteBuf
        return !(channel instanceof Http2StreamChannel);
    }

    private record StaticLocationMapping(String uri, String location) {

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

    }

    /**
     * Function to generate {@code E-TAG}s.
     *
     * @author MJ Fang
     * @since 1.1
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
        EtagGenerator BASIC = (p, a) -> {
            long size = a.size();
            long modifiedTime = a.lastModifiedTime().to(TimeUnit.SECONDS);
            return "\"" + Long.toHexString(modifiedTime) + "-" + Long.toHexString(size) + "\"";
        };

        /**
         * Generate the {@code E-TAG}.
         *
         * @param path           the path of the file
         * @param fileAttributes the basic attributes associated with the file
         * @return the {@code E-TAG} string
         * @throws IOException if any IO error occurs
         */
        String generate(Path path, BasicFileAttributes fileAttributes) throws IOException;

    }

    /**
     * The options of {@link ServeStatic}.
     *
     * @author MJ Fang
     * @since 1.1
     */
    public static class Options {

        private List<String> indexes = List.of("index.html");
        private boolean showHidden = false;
        private boolean redirectDirectory = true;
        private String cacheControl = "no-cache";
        private boolean etag = true;
        private EtagGenerator etagGenerator = EtagGenerator.BASIC;
        private boolean lastModified = true;
        private Consumer<HttpHeaders> addHeaders;
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private boolean allowPost = false;
        private boolean range = false;

        /**
         * Constructs a new {@link Options} instance.
         */
        public Options() {
        }

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
                this.indexes = Arrays.stream(indexes).map(String::trim).filter(s -> !s.isEmpty()).map(String::intern)
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

        /**
         * Set the number of bytes to fetch for each chunk.
         *
         * @param chunkSize the number of bytes to fetch for each chunk
         * @return this {@code Options}
         * @since 2.3
         */
        public Options chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        /**
         * Allow POST method to fetch static resources.
         *
         * @return this {@code Options}
         * @since 4.2
         */
        public Options allowPost() {
            return allowPost(true);
        }

        /**
         * Set whether allow POST method to fetch static resources.
         *
         * @param allowPost {@code true} if allow, {@code false} otherwise
         * @return this {@code Options}
         * @since 4.2
         */
        public Options allowPost(boolean allowPost) {
            this.allowPost = allowPost;
            return this;
        }

        /**
         * Enable Range feature.
         *
         * @return this {@code Options}
         * @since 4.2
         */
        public Options enableRange() {
            return range(true);
        }

        /**
         * Set whether enable Range feature.
         *
         * @param range {@code true} if enable, {@code false} otherwise
         * @return this {@code Options}
         * @since 4.2
         */
        public Options range(boolean range) {
            this.range = range;
            return this;
        }

        @Override
        public String toString() {
            return "ServeStatic.Options[indexes=" + indexes + ", showHidden=" + showHidden + ", redirectDirectory="
                    + redirectDirectory + ", cacheControl=" + cacheControl + ", etag=" + etag + ", etagGenerator="
                    + etagGenerator + ", lastModified=" + lastModified + ", addHeaders=" + addHeaders + ", chunkSize="
                    + chunkSize + ", allowPost=" + allowPost + "]";
        }
    }

    /**
     * Parses the HTTP {@code Range} request header value into a list of {@link Range}s
     * according to RFC 7233.
     * <p>
     * Supported syntax (the {@code bytes} unit only):
     * <ul>
     *     <li>{@code bytes=0-100} &rarr; {@code start=0, end=100}</li>
     *     <li>{@code bytes=100-} &rarr; {@code start=100, end=-1} (open-ended, until EOF)</li>
     *     <li>{@code bytes=-100} &rarr; {@code start=-100, end=-1} (suffix length, last N bytes)</li>
     *     <li>{@code bytes=0-100,300-400,500-} &rarr; multiple ranges separated by commas</li>
     * </ul>
     * <p>
     * This method only validates the syntactic format. The semantic validity
     * (e.g. {@code start} not exceeding {@code end}) is left to the caller via
     * {@link Range#valid()}.
     *
     * @param rangeHeader the raw value of the {@code Range} request header
     * @return the parsed list of ranges, or {@code null} if {@code rangeHeader}
     * is {@code null}, blank, uses an unsupported unit or contains any
     * malformed range specification
     * @since 4.2
     */
    static List<Range> parseRange(String rangeHeader) {
        // Range header must start with the unit (case-insensitive) followed by '='.
        var equalsIndex = rangeHeader.indexOf('=');
        if (equalsIndex <= 0) {
            return null;
        }
        var unit = rangeHeader.substring(0, equalsIndex).trim();
        // Only the "bytes" unit is supported, as required by RFC 7233.
        if (!"bytes".equalsIgnoreCase(unit)) {
            return null;
        }
        var rangeSet = rangeHeader.substring(equalsIndex + 1).trim();
        if (rangeSet.isEmpty()) {
            return null;
        }
        // Use limit = -1 to preserve trailing empty strings so that malformed
        // input such as "bytes=0-100," or "bytes=0-100,,200-300" can be detected
        // by the empty-segment check below instead of being silently accepted.
        var parts = rangeSet.split(",", -1);
        var ranges = new ArrayList<Range>(parts.length);
        for (var part : parts) {
            var spec = part.trim();
            if (spec.isEmpty()) {
                return null;
            }
            var dashIndex = spec.indexOf('-');
            if (dashIndex < 0) {
                return null;
            }
            // Reject specs containing more than one dash, e.g. "0-1-2" or "--100".
            if (spec.indexOf('-', dashIndex + 1) >= 0) {
                return null;
            }
            var startStr = spec.substring(0, dashIndex).trim();
            var endStr = spec.substring(dashIndex + 1).trim();
            long start;
            long end;
            try {
                if (startStr.isEmpty()) {
                    // suffix-byte-range-spec: "-" suffix-length, e.g. "bytes=-100"
                    if (isAnyNotDigit(endStr)) {
                        return null;
                    }
                    start = -Long.parseLong(endStr);
                    end = -1L;
                } else if (endStr.isEmpty()) {
                    // open-ended byte-range-spec, e.g. "bytes=100-"
                    if (isAnyNotDigit(startStr)) {
                        return null;
                    }
                    start = Long.parseLong(startStr);
                    end = -1L;
                } else {
                    // explicit byte-range-spec, e.g. "bytes=0-100"
                    if (isAnyNotDigit(startStr) || isAnyNotDigit(endStr)) {
                        return null;
                    }
                    start = Long.parseLong(startStr);
                    end = Long.parseLong(endStr);
                    System.err.println(endStr);
                    System.err.println(end);
                }
            } catch (NumberFormatException e) {
                // numeric overflow is treated as a malformed range
                return null;
            }
            ranges.add(new Range(start, end));
        }
        return ranges;
    }

    private static boolean isAnyNotDigit(String s) {
        if (s.isEmpty()) {
            return true;
        }
        for (int i = 0, len = s.length(); i < len; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private record Range(long start, long end, boolean valid) {
        private static boolean isValid(long start, long end) {
            return end < 0 || (start >= 0 && start <= end);
        }

        private Range(long start, long end) {
            this(start, end, isValid(start, end));
        }

        private boolean invalid() {
            return !valid;
        }

        private boolean outOfRange(long fileSize) {
            if (end < 0) {
                return start >= 0 ? start >= fileSize : Math.abs(start) > fileSize;
            }
            System.err.println(this);
            var r = end >= fileSize || start >= fileSize;
            System.err.println(r);
            return r;
        }

        private Range normalize(long fileSize) {
            if (end < 0) {
                if (start > 0) {
                    return new Range(start, fileSize - 1);
                } else {
                    return new Range(fileSize + start, fileSize - 1);
                }
            } else {
                return this;
            }
        }

        public long length() {
            return end - start + 1;
        }
    }

}
