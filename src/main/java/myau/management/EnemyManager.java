package myau.management;

import java.awt.Color;
import java.io.File;
import myau.enums.ChatColors;

public class EnemyManager extends PlayerFileManager {
    public EnemyManager() {
        super(new File("./config/Myau/", "enemies.txt"), new Color(ChatColors.DARK_RED.toRgb()));
    }
}
