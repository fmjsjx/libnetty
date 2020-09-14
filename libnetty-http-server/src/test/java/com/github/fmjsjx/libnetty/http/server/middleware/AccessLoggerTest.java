package com.github.fmjsjx.libnetty.http.server.middleware;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.fmjsjx.libnetty.http.HttpHeaderXNames;
import com.github.fmjsjx.libnetty.http.server.HttpRequestContext;
import com.github.fmjsjx.libnetty.http.server.HttpResult;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;

public class AccessLoggerTest {

    private static final LocalDateTime BASE_DATETIME = LocalDateTime.of(2020, 9, 14, 16, 52);

    @Test
    public void testMapLog() {
        String p = "Hello :datetime :iso-local-datetime :iso-local-date :basic-iso-date :iso-local-time :method :path :version :raw-path :remote-address :query :host :content-length :content-type :content :user-agent :referrer :accept - :status :status-code :status-reason :response-time ms :result-length World!";
        StringBuilder resultBuilder = new StringBuilder();
        try (AccessLogger accessLogger = new AccessLogger(resultBuilder::append, p)) {
            DefaultFullHttpRequest request = mockedRequest();
            HttpRequestContext requestContext = mocketRequestContext(request);
            HttpResult result = mockedResult(requestContext);
            String value = accessLogger.mapLog(result);
            assertNotNull(value);
            String expected = "Hello 2020-09-14 16:52:00.123 2020-09-14T16:52:00.123456789 2020-09-14 20200914 16:52:00.123456789 POST /test HTTP/1.1 /test 127.0.0.1 q1=1&q2=abc localhost 78 application/json {\"action\":\"test\",\"date\":\"2020-09-14\",\"time\":\"16:51:23\",\"timestamp\":1600073543} test http://otherdomain.com/home.html application/json - 200 OK 200 OK 123.457 ms 47 World!";
            assertEquals(expected, value);
        } catch (Exception e) {
            fail(e);
        }
    }

    private DefaultFullHttpRequest mockedRequest() {
        // {"action":"test","date":"2020-09-14","time":"16:51:23","timestamp":1600073543}
        ByteBuf content = Unpooled.copiedBuffer(
                "{\"action\":\"test\",\"date\":\"2020-09-14\",\"time\":\"16:51:23\",\"timestamp\":1600073543}",
                CharsetUtil.UTF_8);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                "/test?q1=1&q2=abc", content);
        HttpUtil.setContentLength(request, content.readableBytes());
        request.headers().set(HttpHeaderNames.HOST, "localhost");
        request.headers().set(HttpHeaderNames.USER_AGENT, "test");
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        request.headers().set(HttpHeaderNames.ACCEPT, "application/json");
        request.headers().set(HttpHeaderNames.REFERER, "http://otherdomain.com/home.html");
        request.headers().set(HttpHeaderXNames.X_FORWARDED_FOR, "127.0.0.1");
        return request;
    }

    private HttpRequestContext mocketRequestContext(FullHttpRequest request) {
        LocalDateTime datetime = BASE_DATETIME;
        int contentLength = request.content().readableBytes();
        return new HttpRequestContext() {

            @Override
            public FullHttpRequest request() {
                return request;
            }

            @Override
            public String remoteAddress() {
                return com.github.fmjsjx.libnetty.http.HttpUtil.remoteAddress(channel(), request.headers());
            }

            @Override
            public ZonedDateTime receivedTime() {
                return datetime.atZone(ZoneId.systemDefault());
            }

            @Override
            public long receivedNanoTime() {
                return 0;
            }

            @Override
            public QueryStringDecoder queryStringDecoder() {
                return new QueryStringDecoder(request.uri());
            }

            @Override
            public Stream<Object> propertyKeys() {
                return Stream.empty();
            }

            @Override
            public HttpRequestContext property(Object key, Object value) {
                return this;
            }

            @Override
            public <T> Optional<T> property(Object key, Class<T> type) throws ClassCastException {
                return Optional.empty();
            }

            @Override
            public <T> Optional<T> property(Object key) throws ClassCastException {
                return Optional.empty();
            }

            @Override
            public Optional<CharSequence> contentType() {
                return Optional.ofNullable(HttpUtil.getMimeType(request));
            }

            @Override
            public int contentLength() {
                return contentLength;
            }

            @Override
            public Channel channel() {
                return null;
            }
        };
    }

    private HttpResult mockedResult(HttpRequestContext ctx) {
        // {"code":0,"data":{"action":test,"result":"OK"}}
        String responseBody = "{\"code\":0,\"data\":{\"action\":test,\"result\":\"OK\"}}";
        byte[] b = responseBody.getBytes(CharsetUtil.UTF_8);
        Long resultLength = Long.valueOf(b.length);
        HttpResult result = mock(HttpResult.class);
        long nanosGone = 123456789L;
        ZonedDateTime time = BASE_DATETIME.plusNanos(nanosGone).atZone(ZoneId.systemDefault());
        when(result.requestContext()).thenReturn(ctx);
        when(result.resultLength()).thenReturn(resultLength);
        when(result.responseStatus()).thenReturn(HttpResponseStatus.OK);
        when(result.respondedNaonTime()).thenReturn(nanosGone);
        when(result.respondedTime()).thenReturn(time);
        when(result.respondedTime(any(ZoneId.class))).thenCallRealMethod();
        when(result.nanoUsed()).thenCallRealMethod();
        when(result.timeUsed(any(TimeUnit.class))).thenCallRealMethod();
        return result;
    }

}
