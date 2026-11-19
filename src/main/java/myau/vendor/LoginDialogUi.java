package myau.vendor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Pure-Java Swing reconstruction of the native Dear ImGui {@code 1.88 WIP} +
 * DirectX 11 login/registration window ({@code .rdata} {@code 0x1809E0D80..0x1809E0EE0})
 * and toast notification overlay ({@code 0x1805ADF80}, {@code 0x1809E0258..0x1809E0288}).
 *
 * <p>Preserved strictly for structural reference; never instantiated by the client.
 */
public final class LoginDialogUi extends JDialog implements AuthWebSocketClient.StatusListener {
    public static final int TOAST_NONE = 0;
    public static final int TOAST_SUCCESS = 1;
    public static final int TOAST_WARNING = 2;
    public static final int TOAST_ERROR = 3;
    public static final int TOAST_INFO = 4;

    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_REGISTER = "REGISTER";

    private final ClientIniStore iniStore = new ClientIniStore();
    private final RegisterApiClient registerClient = new RegisterApiClient();
    private final AuthWebSocketClient wsClient = new AuthWebSocketClient();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardContainer = new JPanel(cardLayout);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    private final JTextField loginUsernameField = new JTextField();
    private final JPasswordField loginPasswordField = new JPasswordField();
    private final JCheckBox rememberCheckBox = new JCheckBox("Remember Me");
    private final JButton signInButton = new JButton("Sign In");

    private final JTextField regUsernameField = new JTextField();
    private final JPasswordField regPasswordField = new JPasswordField();
    private final JTextField regCdKeyField = new JTextField();
    private final JButton registerButton = new JButton("Register");

    public LoginDialogUi() {
        setTitle(VendorConfig.CLIENT_NAME + " #" + VendorConfig.CLIENT_VERSION);
        setModal(true);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        wsClient.setStatusListener(this);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        root.setBackground(new Color(24, 24, 28));

        JLabel header = new JLabel(VendorConfig.CLIENT_NAME + " #" + VendorConfig.CLIENT_VERSION, SwingConstants.CENTER);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
        root.add(header, BorderLayout.NORTH);

        cardContainer.setOpaque(false);
        cardContainer.add(buildLoginPanel(), CARD_LOGIN);
        cardContainer.add(buildRegisterPanel(), CARD_REGISTER);
        root.add(cardContainer, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        footerPanel.setOpaque(false);
        statusLabel.setForeground(new Color(200, 200, 210));
        JLabel poweredBy = new JLabel("Powered by 苏辰", SwingConstants.CENTER);
        poweredBy.setForeground(new Color(140, 140, 155));
        footerPanel.add(statusLabel);
        footerPanel.add(poweredBy);
        root.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(root);
        setPreferredSize(new Dimension(370, 320));
        pack();
        setLocationRelativeTo(null);

        ClientIniStore.Credentials creds = iniStore.load();
        rememberCheckBox.setSelected(creds.remember);
        if (creds.remember) {
            loginUsernameField.setText(creds.username);
            loginPasswordField.setText(creds.password);
        }
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setOpaque(false);

        panel.add(styledLabel("用户名 / Username"));
        panel.add(loginUsernameField);
        panel.add(styledLabel("密码 / Password"));
        panel.add(loginPasswordField);

        rememberCheckBox.setOpaque(false);
        rememberCheckBox.setForeground(Color.LIGHT_GRAY);
        panel.add(rememberCheckBox);

        JButton switchRegisterButton = new JButton("没有账号？点我注册账号");
        JButton exitButton = new JButton("Exit");

        signInButton.addActionListener(e -> onSignInClicked());
        switchRegisterButton.addActionListener(e -> cardLayout.show(cardContainer, CARD_REGISTER));
        exitButton.addActionListener(e -> dispose());

        panel.add(signInButton);
        panel.add(switchRegisterButton);
        panel.add(exitButton);
        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        panel.setOpaque(false);

        panel.add(styledLabel("用户名 / Username"));
        panel.add(regUsernameField);
        panel.add(styledLabel("密码 / Password"));
        panel.add(regPasswordField);
        panel.add(styledLabel("注册码 / CDKEY"));
        panel.add(regCdKeyField);

        JButton backButton = new JButton("Back Login");
        registerButton.addActionListener(e -> onRegisterClicked());
        backButton.addActionListener(e -> cardLayout.show(cardContainer, CARD_LOGIN));

        panel.add(registerButton);
        panel.add(backButton);
        return panel;
    }

    private void onSignInClicked() {
        String user = loginUsernameField.getText().trim();
        String pass = new String(loginPasswordField.getPassword());
        if (user.isEmpty() || pass.isEmpty()) {
            onNotify(TOAST_WARNING, 3000L, "请先输入账号密码");
            return;
        }

        ClientIniStore.Credentials creds = new ClientIniStore.Credentials();
        creds.remember = rememberCheckBox.isSelected();
        creds.username = user;
        creds.password = pass;
        iniStore.save(creds);

        signInButton.setEnabled(false);
        statusLabel.setForeground(new Color(200, 200, 210));
        statusLabel.setText("loading...");

        Thread loginThread = new Thread(() -> {
            try {
                wsClient.connectAndAuthenticate(user, pass);
            } catch (Exception ex) {
                onAuthFailed(ex.getMessage());
            }
        }, "Myau-Vendor-Login");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void onRegisterClicked() {
        String user = regUsernameField.getText().trim();
        String pass = new String(regPasswordField.getPassword());
        String cdKey = regCdKeyField.getText().trim();
        if (user.isEmpty() || pass.isEmpty() || cdKey.isEmpty()) {
            onNotify(TOAST_WARNING, 3000L, "请先输入账号密码");
            return;
        }

        registerButton.setEnabled(false);
        registerButton.setText("Registering");
        Thread regThread = new Thread(() -> {
            RegisterApiClient.RegisterResponse resp = registerClient.register(user, pass, cdKey);
            SwingUtilities.invokeLater(() -> {
                registerButton.setEnabled(true);
                registerButton.setText("Register");
                int level = resp.httpStatus == 200 && resp.code == 0 ? TOAST_SUCCESS : TOAST_ERROR;
                onNotify(level, 4000L, resp.message);
            });
        }, "Myau-Vendor-Register");
        regThread.setDaemon(true);
        regThread.start();
    }

    @Override
    public void onNotify(int toastLevel, long durationMs, String message) {
        SwingUtilities.invokeLater(() -> {
            String prefix;
            Color color;
            switch (toastLevel) {
                case TOAST_SUCCESS:
                    prefix = "[Success] ";
                    color = new Color(110, 220, 130);
                    break;
                case TOAST_WARNING:
                    prefix = "[Warning] ";
                    color = new Color(240, 200, 90);
                    break;
                case TOAST_ERROR:
                    prefix = "[Error] ";
                    color = new Color(240, 105, 105);
                    break;
                case TOAST_INFO:
                    prefix = "[Info] ";
                    color = new Color(130, 190, 245);
                    break;
                default:
                    prefix = "";
                    color = new Color(200, 200, 210);
                    break;
            }
            statusLabel.setForeground(color);
            statusLabel.setText(prefix + (message != null ? message : ""));
        });
    }

    @Override
    public void onAuthSucceeded() {
        SwingUtilities.invokeLater(() -> {
            signInButton.setEnabled(true);
            dispose();
        });
    }

    @Override
    public void onAuthFailed(String reason) {
        SwingUtilities.invokeLater(() -> {
            signInButton.setEnabled(true);
            if (reason != null && !reason.isEmpty()) {
                onNotify(TOAST_ERROR, 4000L, reason);
            }
        });
    }

    private static JLabel styledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(220, 220, 230));
        return label;
    }
}
