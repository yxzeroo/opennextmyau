package myau.management;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.stream.Collectors;
import myau.mixin.IAccessorMinecraft;
import net.minecraft.client.Minecraft;

public abstract class PlayerFileManager {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ArrayList<String> players = new ArrayList<>();
    private final File file;
    private final Color color;

    public PlayerFileManager(File file, Color color) {
        this.file = file;
        this.color = color;
    }

    public void load() {
        if (!this.file.exists()) {
            try {
                Files.createDirectories(this.file.toPath().getParent());
                if (this.file.createNewFile()) {
                    ((IAccessorMinecraft) mc).getLogger().info("Created: {}", this.file.getName());
                }
            } catch (Exception exception) {
                ((IAccessorMinecraft) mc).getLogger().error(exception.getMessage());
            }
        }
        try (FileReader fileReader = new FileReader(this.file);
             BufferedReader bufferedReader = new BufferedReader(fileReader)) {
            this.players.addAll(bufferedReader.lines().map(String::trim).collect(Collectors.toList()));
        } catch (Exception exception) {
            ((IAccessorMinecraft) mc).getLogger().error(exception.getMessage());
        }
    }

    public void save() {
        try (FileWriter fileWriter = new FileWriter(this.file);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.print(String.join("\n", this.players));
        } catch (Exception exception) {
            ((IAccessorMinecraft) mc).getLogger().error(exception.getMessage());
        }
    }

    public String add(String name) {
        if (this.contains(name)) {
            return null;
        }
        this.players.add(name);
        this.save();
        return name;
    }

    public String remove(String name) {
        for (String existing : this.players) {
            if (existing.equalsIgnoreCase(name)) {
                this.players.remove(existing);
                this.save();
                return existing;
            }
        }
        return null;
    }

    public void clear() {
        this.players.clear();
        this.save();
    }

    public boolean contains(String name) {
        return this.players.stream().anyMatch(existing -> existing.equalsIgnoreCase(name));
    }

    public ArrayList<String> getPlayers() {
        return this.players;
    }

    public Color getColor() {
        return this.color;
    }
}
