package myau.vendor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * Restored from the native WebSocket client and command dispatcher ({@code 0x1805EE310},
 * {@code 0x18063AAF0}, {@code 0x18063C360}).
 * Connects to {@code wss://foodbyte.cc:443/MYAU/<encryptedHandshakeToken>}, exchanges AES-encrypted
 * JSON frames, responds to {@code getInfo} / {@code getHwid} / {@code hwAuth} / key rotation commands,
 * and sends {@code .pong} keepalives.
 * Restored for inspection only; never invoked by the mod.
 */
public class AuthWebSocketClient {
    private byte[] clientKey = VendorConfig.getInitialKeyBytes();
    private byte[] serverKey = VendorConfig.getInitialKeyBytes();
    private final Map<String, byte[]> downloadedBlobs = new LinkedHashMap<>();
    private volatile boolean authenticated = false;
    private volatile boolean closed = false;
    private final LoginDialogUi ui;

    public AuthWebSocketClient(LoginDialogUi ui) {
        this.ui = ui;
    }

    public String buildHandshakePath(String username, String password, String nonce) {
        String rawToken = username + "$" + password + "$" + nonce + VendorConfig.PROTOCOL_VERSION_SUFFIX;
        String encryptedHex = PacketCipher.encryptToHex(rawToken, VendorConfig.getInitialKeyBytes());
        return "/" + VendorConfig.CLIENT_NAME + "/" + encryptedHex;
    }

    public Map<String, String> buildUpgradeHeaders(String hostHeader) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Host", hostHeader);
        headers.put("Upgrade", "websocket");
        headers.put("Connection", "Upgrade");
        headers.put("Sec-WebSocket-Key", VendorConfig.WS_SEC_KEY);
        headers.put("Sec-WebSocket-Version", "13");
        headers.put("User-Agent", VendorConfig.USER_AGENT);
        headers.put("Authorization", VendorConfig.WS_AUTHORIZATION);
        headers.put("Cookies", VendorConfig.WS_COOKIE);
        return headers;
    }

    public boolean connectAndAuthenticate(String username, String password) throws IOException {
        String nonce = Long.toUnsignedString(new SecureRandom().nextLong());
        String path = buildHandshakePath(username, password, nonce);

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(VendorConfig.HOST, VendorConfig.PORT)) {
            socket.setSoTimeout(15000);
            socket.startHandshake();

            OutputStream rawOut = new BufferedOutputStream(socket.getOutputStream());
            InputStream rawIn = new BufferedInputStream(socket.getInputStream());

            StringBuilder req = new StringBuilder();
            req.append("GET ").append(path).append(" HTTP/1.1\r\n");
            for (Map.Entry<String, String> entry : buildUpgradeHeaders(VendorConfig.HOST + ":" + VendorConfig.PORT).entrySet()) {
                req.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
            }
            req.append("\r\n");
            rawOut.write(req.toString().getBytes(StandardCharsets.UTF_8));
            rawOut.flush();

            String statusLine = readHttpHeaderBlock(rawIn);
            if (!statusLine.contains("101")) {
                throw new IOException("Bad status code: " + statusLine);
            }

            while (!this.closed && !this.authenticated) {
                String frameText = readTextFrame(rawIn, rawOut);
                if (frameText == null) {
                    break;
                }
                handleIncomingMessage(frameText, rawOut);
            }
            return this.authenticated;
        }
    }

    public void handleIncomingMessage(String rawFrame, OutputStream out) throws IOException {
        if (rawFrame == null || rawFrame.isEmpty()) {
            return;
        }
        if (".ping".equals(rawFrame)) {
            sendEncryptedText(VendorConfig.HEARTBEAT_PONG, out);
            return;
        }

        String decrypted = PacketCipher.decryptFromHex(rawFrame.trim(), this.serverKey);
        JsonElement parsed = new JsonParser().parse(decrypted);
        if (parsed == null || !parsed.isJsonObject()) {
            return;
        }
        JsonObject msg = parsed.getAsJsonObject();
        String cmd = getStringOrEmpty(msg, "cmd");
        if (cmd.isEmpty()) {
            cmd = getStringOrEmpty(msg, "type");
        }

        switch (cmd) {
            case "setClientKey": {
                String key = getStringOrEmpty(msg, "key");
                this.clientKey = PacketCipher.normalizeKey16(key);
                break;
            }
            case "setServerKey": {
                String key = getStringOrEmpty(msg, "key");
                this.serverKey = PacketCipher.normalizeKey16(key);
                break;
            }
            case "getInfo": {
                JsonObject info = JvmProcessInspector.collectCurrentJvmTelemetry();
                if (info.entrySet().isEmpty()) {
                    throw new IllegalStateException("Minecraft info empty!");
                }
                JsonObject reply = new JsonObject();
                reply.addProperty("cmd", "InfoTag");
                reply.add("data", info);
                sendEncryptedText(reply.toString(), out);
                break;
            }
            case "getHwid":
            case "hwAuth": {
                JsonObject reply = new JsonObject();
                reply.addProperty("cmd", cmd);
                reply.addProperty("hwid", HardwareIdGenerator.computeHardwareId());
                reply.addProperty("version", VendorConfig.CLIENT_VERSION);
                sendEncryptedText(reply.toString(), out);
                break;
            }
            case "notify": {
                String text = getStringOrEmpty(msg, "message");
                if (this.ui != null) {
                    this.ui.pushToast(LoginDialogUi.ToastLevel.INFO, 3000, text);
                }
                break;
            }
            case "download": {
                String name = getStringOrEmpty(msg, "name");
                String hexBytes = getStringOrEmpty(msg, "data");
                if (!hexBytes.isEmpty()) {
                    this.downloadedBlobs.put(name, PacketCipher.fromHex(hexBytes));
                }
                break;
            }
            case "printString": {
                System.out.println(getStringOrEmpty(msg, "value"));
                break;
            }
            case "setEnvString": {
                String propKey = getStringOrEmpty(msg, "key");
                String propVal = getStringOrEmpty(msg, "value");
                if (!propKey.isEmpty()) {
                    System.setProperty(propKey, propVal);
                }
                break;
            }
            case "begin": {
                this.authenticated = true;
                break;
            }
            case "fail": {
                String reason = getStringOrEmpty(msg, "message");
                if (this.ui != null) {
                    this.ui.pushToast(LoginDialogUi.ToastLevel.ERROR, 3000, reason);
                }
                this.closed = true;
                break;
            }
            case "exit": {
                this.closed = true;
                break;
            }
            default:
                break;
        }
    }

    public void sendEncryptedText(String plaintext, OutputStream out) throws IOException {
        String hex = PacketCipher.encryptToHex(plaintext, this.clientKey);
        writeMaskedTextFrame(hex.getBytes(StandardCharsets.UTF_8), out);
    }

    public Map<String, byte[]> getDownloadedBlobs() {
        return Collections.unmodifiableMap(this.downloadedBlobs);
    }

    private static String getStringOrEmpty(JsonObject obj, String member) {
        if (obj.has(member) && !obj.get(member).isJsonNull()) {
            return obj.get(member).getAsString();
        }
        return "";
    }

    private static String readHttpHeaderBlock(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int state = 0;
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            if ((state == 0 || state == 2) && b == '\r') {
                state++;
            } else if ((state == 1 || state == 3) && b == '\n') {
                state++;
                if (state == 4) {
                    break;
                }
            } else {
                state = 0;
            }
        }
        String headers = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        int crlf = headers.indexOf("\r\n");
        return crlf >= 0 ? headers.substring(0, crlf) : headers;
    }

    private static String readTextFrame(InputStream in, OutputStream out) throws IOException {
        int b0 = in.read();
        if (b0 == -1) {
            return null;
        }
        int b1 = in.read();
        if (b1 == -1) {
            return null;
        }
        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        long len = b1 & 0x7F;
        if (len == 126) {
            len = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (len == 127) {
            len = 0;
            for (int i = 0; i < 8; i++) {
                len = (len << 8) | (in.read() & 0xFF);
            }
        }
        byte[] mask = new byte[4];
        if (masked) {
            readFully(in, mask);
        }
        byte[] payload = new byte[(int) len];
        readFully(in, payload);
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= mask[i & 3];
            }
        }
        if (opcode == 0x8) {
            return null;
        }
        if (opcode == 0x9) {
            writeFrame(0xA, payload, out);
            return "";
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static void readFully(InputStream in, byte[] dst) throws IOException {
        int off = 0;
        while (off < dst.length) {
            int r = in.read(dst, off, dst.length - off);
            if (r < 0) {
                throw new IOException("Connection lost during read");
            }
            off += r;
        }
    }

    private static void writeMaskedTextFrame(byte[] payload, OutputStream out) throws IOException {
        writeFrame(0x1, payload, out);
    }

    private static void writeFrame(int opcode, byte[] payload, OutputStream out) throws IOException {
        byte[] mask = new byte[4];
        new SecureRandom().nextBytes(mask);
        out.write(0x80 | (opcode & 0x0F));
        if (payload.length < 126) {
            out.write(0x80 | payload.length);
        } else if (payload.length <= 65535) {
            out.write(0x80 | 126);
            out.write((payload.length >>> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        } else {
            out.write(0x80 | 127);
            long l = payload.length;
            for (int i = 7; i >= 0; i--) {
                out.write((int) ((l >>> (i * 8)) & 0xFF));
            }
        }
        out.write(mask);
        for (int i = 0; i < payload.length; i++) {
            out.write(payload[i] ^ mask[i & 3]);
        }
        out.flush();
    }
}
