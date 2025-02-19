/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package gov.nist.toolkit.fhir.simulators.proxy.service;

import org.apache.http.HttpHost;

import java.io.IOException;

/**
 * Elemental HTTP/1.1 reverse proxy.
 */
public class ElementalReverseProxy {

    static final String HTTP_IN_CONN = "http.proxy.in-conn";
    static final String HTTP_OUT_CONN = "http.proxy.out-conn";
    static final String HTTP_CONN_KEEPALIVE = "http.proxy.conn-keepalive";
    static final String HTTP_PROXY_BASE = "http.proxy.base";

    // localhost:8889 7777
    public static void main(final String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: <hostname[:port]> [listener port]");
            System.exit(1);
        }
        final HttpHost targetHost = HttpHost.create(args[0]);
        int port = 8080;
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }

        start(port);

    }

    static public RequestListenerThread start(int port) throws IOException {
        System.out.println("Proxy Operation: Starting Sim Proxy on port " + port);

        final RequestListenerThread t = new RequestListenerThread(port);
        t.setDaemon(false);
        t.start();
        return t;
    }

    static public RequestListenerThread start(String port) throws Exception {
        return start(Integer.parseInt(port));
    }
}