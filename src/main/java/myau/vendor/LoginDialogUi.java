package myau.vendor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Restored Java representation of the native Dear ImGui authentication window ({@code 0x1805CEC90})
 * and toast notification overlay ({@code 0x1805ADF80}).
 */
public class LoginDialogUi {

    public enum ToastLevel {
        NONE(""),
        SUCCESS("Success"),
        WARNING("Warning"),
        ERROR("Error"),
        INFO("Info");

        public final String defaultTitle;

        ToastLevel(String defaultTitle) {
            this.defaultTitle = defaultTitle;
        }
    }

    public static final class ToastNotification {
        public final ToastLevel level;
        public final String title;
        public final String content;
        public final long createdAtMs;
        public final int durationMs;

        public ToastNotification(ToastLevel level, int durationMs, String content) {
            this(level, level.defaultTitle, content, System.currentTimeMillis(), durationMs);
        }

        public ToastNotification(ToastLevel level, String title, String content, long createdAtMs, int durationMs) {
            this.level = level;
            this.title = (title == null || title.isEmpty()) ? level.defaultTitle : title;
            this.content = content != null ? content : "";
            this.createdAtMs = createdAtMs;
            this.durationMs = durationMs;
        }

        public boolean isExpired(long nowMs) {
            return (nowMs - this.createdAtMs) > (this.durationMs + 300L);
        }
    }

    private final List<ToastNotification> toasts = new ArrayList<>();
    private boolean registerMode = false;
    private boolean loading = false;

    public void pushToast(ToastLevel level, int durationMs, String message) {
        synchronized (this.toasts) {
            this.toasts.add(new ToastNotification(level, durationMs, message));
        }
    }

    public List<ToastNotification> getActiveToasts() {
        long now = System.currentTimeMillis();
        synchronized (this.toasts) {
            Iterator<ToastNotification> it = this.toasts.iterator();
            while (it.hasNext()) {
                if (it.next().isExpired(now)) {
                    it.remove();
                }
            }
            return new ArrayList<>(this.toasts);
        }
    }

    public JDialog createDialog() {
        ClientIniStore.SavedCredentials saved = ClientIniStore.load();

        JDialog dialog = new JDialog((java.awt.Frame) null, VendorConfig.CLIENT_NAME, true);
        dialog.setSize(new Dimension(380, 280));
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout(8, 8));

        CardLayout cards = new CardLayout();
        JPanel centerPanel = new JPanel(cards);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JTextField loginUser = new JTextField(saved.username, 20);
        JPasswordField loginPass = new JPasswordField(saved.password, 20);
        JCheckBox rememberBox = new JCheckBox("Remember Me", saved.remember);
        JLabel statusLabel = new JLabel(VendorConfig.POWERED_BY);
        statusLabel.setForeground(Color.GRAY);

        JPanel loginCard = new JPanel(new GridLayout(0, 1, 4, 4));
        loginCard.add(new JLabel("Username"));
        loginCard.add(loginUser);
        loginCard.add(new JLabel("Password"));
        loginCard.add(loginPass);
        loginCard.add(rememberBox);

        JButton signInButton = new JButton("Sign In");
        JButton toRegisterButton = new JButton("\u6ca1\u6709\u8d26\u53f7\uff1f\u70b9\u6211\u6ce8\u518c\u8d26\u53f7");
        JButton exitButton = new JButton("Exit");

        JPanel loginActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        loginActions.add(toRegisterButton);
        loginActions.add(signInButton);
        loginActions.add(exitButton);
        loginCard.add(loginActions);

        JTextField regUser = new JTextField(20);
        JPasswordField regPass = new JPasswordField(20);
        JTextField regCdkey = new JTextField(20);
        JButton registerButton = new JButton("Register");
        JButton backLoginButton = new JButton("Back Login");

        JPanel registerCard = new JPanel(new GridLayout(0, 1, 4, 4));
        registerCard.add(new JLabel("Username"));
        registerCard.add(regUser);
        registerCard.add(new JLabel("Password"));
        registerCard.add(regPass);
        registerCard.add(new JLabel("CDKEY"));
        registerCard.add(regCdkey);

        JPanel regActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        regActions.add(backLoginButton);
        regActions.add(registerButton);
        registerCard.add(regActions);

        centerPanel.add(loginCard, "login");
        centerPanel.add(registerCard, "register");

        toRegisterButton.addActionListener(e -> {
            this.registerMode = true;
            cards.show(centerPanel, "register");
        });
        backLoginButton.addActionListener(e -> {
            this.registerMode = false;
            cards.show(centerPanel, "login");
        });
        exitButton.addActionListener(e -> dialog.dispose());

        signInButton.addActionListener(e -> {
            String u = loginUser.getText().trim();
            String p = new String(loginPass.getPassword());
            if (u.isEmpty() || p.isEmpty()) {
                pushToast(ToastLevel.WARNING, 3000, "\u8bf7\u5148\u8f93\u5165\u8d26\u53f7\u5bc6\u7801");
                statusLabel.setText("\u8bf7\u5148\u8f93\u5165\u8d26\u53f7\u5bc6\u7801");
                return;
            }
            try {
                ClientIniStore.save(rememberBox.isSelected(), u, p);
            } catch (Exception ignored) {
            }
            this.loading = true;
            statusLabel.setText("loading");
        });

        registerButton.addActionListener(e -> {
            registerButton.setText("Registering");
            RegisterApiClient.RegisterResult res = RegisterApiClient.registerAccount(
                    regUser.getText().trim(),
                    new String(regPass.getPassword()),
                    regCdkey.getText().trim()
            );
            registerButton.setText("Register");
            pushToast(res.success ? ToastLevel.SUCCESS : ToastLevel.ERROR, 3000, res.message);
            statusLabel.setText(res.message);
        });

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(statusLabel, BorderLayout.SOUTH);
        return dialog;
    }

    public boolean isRegisterMode() {
        return this.registerMode;
    }

    public boolean isLoading() {
        return this.loading;
    }
}
