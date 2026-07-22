package com.apiautomation.security;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Blocks requests to internal / link-local / metadata targets (SSRF guard).
 */
public final class SsrfGuard {

    private SsrfGuard() {
    }

    /**
     * @return error message if unsafe, otherwise null
     */
    public static String check(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "URL is missing — cannot execute request.";
        }

        final URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (Exception e) {
            return "Invalid URL: " + e.getMessage();
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            return "Only http/https URLs are allowed (got: " + scheme + ").";
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return "URL host is missing.";
        }

        String hostLower = host.toLowerCase(Locale.ROOT);
        if ("localhost".equals(hostLower)
                || hostLower.endsWith(".localhost")
                || hostLower.endsWith(".local")
                || "metadata.google.internal".equals(hostLower)
                || hostLower.endsWith(".internal")) {
            return "Blocked host (SSRF protection): " + host;
        }

        if (isBlockedLiteralIp(hostLower)) {
            return "Blocked IP literal (SSRF protection): " + host;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isBlockedAddress(addr)) {
                    return "Blocked resolved address (SSRF protection): " + addr.getHostAddress()
                            + " for host " + host;
                }
            }
        } catch (UnknownHostException e) {
            return "Cannot resolve host (SSRF protection): " + host;
        }

        return null;
    }

    public static void assertSafe(String rawUrl) {
        String err = check(rawUrl);
        if (err != null) {
            throw new SecurityException(err);
        }
    }

    private static boolean isBlockedLiteralIp(String host) {
        // IPv4 dotted or IPv6 in brackets already stripped by URI.getHost()
        if ("0.0.0.0".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:1".equals(host)) {
            return true;
        }
        try {
            InetAddress addr = InetAddress.getByName(host);
            return isBlockedAddress(addr);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isBlockedAddress(InetAddress addr) {
        if (addr.isAnyLocalAddress()
                || addr.isLoopbackAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] b = addr.getAddress();
        if (b.length == 4) {
            int a = b[0] & 0xff;
            int c = b[1] & 0xff;
            // 169.254.0.0/16 link-local (also covered) + AWS/GCP metadata commonly 169.254.169.254
            if (a == 169 && c == 254) {
                return true;
            }
            // 100.64.0.0/10 carrier-grade NAT
            if (a == 100 && c >= 64 && c <= 127) {
                return true;
            }
        }
        return false;
    }
}
