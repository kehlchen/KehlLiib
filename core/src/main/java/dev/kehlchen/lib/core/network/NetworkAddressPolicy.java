package dev.kehlchen.lib.core.network;

import java.net.Inet6Address;
import java.net.InetAddress;

final class NetworkAddressPolicy {
    private NetworkAddressPolicy() {}

    static boolean isNonPublic(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return true;
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address) {
            return (bytes[0] & 0xfe) == 0xfc
                    || (bytes[0] & 0xff) == 0x20 && (bytes[1] & 0xff) == 0x01
                    && (bytes[2] & 0xff) == 0x0d && (bytes[3] & 0xff) == 0xb8;
        }
        int first = bytes[0] & 0xff;
        int second = bytes[1] & 0xff;
        int third = bytes[2] & 0xff;
        return first == 0 || first == 10 || first == 127 || first >= 224
                || first == 100 && second >= 64 && second <= 127
                || first == 172 && second >= 16 && second <= 31
                || first == 192 && second == 168
                || first == 192 && second == 0 && (third == 0 || third == 2)
                || first == 192 && second == 88 && third == 99
                || first == 198 && (second == 18 || second == 19)
                || first == 198 && second == 51 && third == 100
                || first == 203 && second == 0 && third == 113;
    }
}
