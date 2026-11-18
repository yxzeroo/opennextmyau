package myau.util;

import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class ServerUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static ArrayList<String> getScoreboardLines() {
        if (mc.theWorld == null) {
            return new ArrayList<>();
        }
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return new ArrayList<>();
        }
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return new ArrayList<>();
        }
        return scoreboard.getSortedScores(objective).stream()
            .map(score -> ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(score.getPlayerName()), score.getPlayerName()))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public static boolean isHypixel() {
        ArrayList<String> lines = getScoreboardLines();
        if (lines.isEmpty()) {
            return false;
        }
        String first = lines.get(0);
        return first.equals("\u00a7ewww.hypixel.ne\uf382\u00a7et") || first.equals("\u00a7ewww.hypixel.ne\u00a7g\u00a7et");
    }

    public static boolean hasPlayersLine() {
        for (String line : getScoreboardLines()) {
            if (line.matches(".*Players: \u00a7a\\d+/\\d+.*")) {
                return true;
            }
        }
        return false;
    }
}
