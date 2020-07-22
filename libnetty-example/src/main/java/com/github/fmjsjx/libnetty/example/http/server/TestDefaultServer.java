package com.github.fmjsjx.libnetty.example.http.server;

import com.github.fmjsjx.libnetty.http.server.DefaultHttpServer;
import com.github.fmjsjx.libnetty.http.server.SslContextProviders;

public class TestDefaultServer {

    public static void main(String[] args) throws Exception {
        DefaultHttpServer server = new DefaultHttpServer(SslContextProviders.selfSigned()).port(80);
        try {
            server.startup();
            System.in.read();
        } catch (Exception e) {
            System.err.println("Unexpected error occurs when startup " + server);
            e.printStackTrace();
        } finally {
            if (server.isRunning()) {
                server.shutdown();
            }
        }
    }

}
