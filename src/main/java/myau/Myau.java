package myau;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import myau.command.CommandManager;
import myau.command.commands.BindCommand;
import myau.command.commands.ConfigCommand;
import myau.command.commands.DenickCommand;
import myau.command.commands.EnemyCommand;
import myau.command.commands.FriendCommand;
import myau.command.commands.HelpCommand;
import myau.command.commands.HideCommand;
import myau.command.commands.ItemCommand;
import myau.command.commands.ListCommand;
import myau.command.commands.ModuleCommand;
import myau.command.commands.PlayerListCommand;
import myau.command.commands.ShowCommand;
import myau.command.commands.ToggleCommand;
import myau.command.commands.UsernameCommand;
import myau.command.commands.VclipCommand;
import myau.config.Config;
import myau.event.EventManager;
import myau.management.BlinkManager;
import myau.management.EnemyManager;
import myau.management.FloatManager;
import myau.management.FriendManager;
import myau.management.HurtTimeTracker;
import myau.management.LagManager;
import myau.management.LagRangeManager;
import myau.management.PacketStateTracker;
import myau.management.RotationManager;
import myau.module.Module;
import myau.module.ModuleManager;
import myau.module.modules.AimAssist;
import myau.module.modules.AntiDebuff;
import myau.module.modules.AntiFireball;
import myau.module.modules.AntiObbyTrap;
import myau.module.modules.AntiObfuscate;
import myau.module.modules.AntiVoid;
import myau.module.modules.AutoClicker;
import myau.module.modules.AutoHeal;
import myau.module.modules.AutoTool;
import myau.module.modules.BedESP;
import myau.module.modules.BedNuker;
import myau.module.modules.BedTracker;
import myau.module.modules.Blink;
import myau.module.modules.Chams;
import myau.module.modules.ChestESP;
import myau.module.modules.ChestStealer;
import myau.module.modules.ESP;
import myau.module.modules.Eagle;
import myau.module.modules.FastPlace;
import myau.module.modules.Fly;
import myau.module.modules.Fullbright;
import myau.module.modules.GhostHand;
import myau.module.modules.HUD;
import myau.module.modules.HitSelect;
import myau.module.modules.Indicators;
import myau.module.modules.InvManager;
import myau.module.modules.InvWalk;
import myau.module.modules.ItemESP;
import myau.module.modules.Jesus;
import myau.module.modules.KeepSprint;
import myau.module.modules.KillAura;
import myau.module.modules.LagRange;
import myau.module.modules.LightningTracker;
import myau.module.modules.LongJump;
import myau.module.modules.MCF;
import myau.module.modules.NameTags;
import myau.module.modules.NickHider;
import myau.module.modules.NoFall;
import myau.module.modules.NoHitDelay;
import myau.module.modules.NoHurtCam;
import myau.module.modules.NoJumpDelay;
import myau.module.modules.NoRotate;
import myau.module.modules.NoSlow;
import myau.module.modules.Reach;
import myau.module.modules.SafeWalk;
import myau.module.modules.Scaffold;
import myau.module.modules.Spammer;
import myau.module.modules.Speed;
import myau.module.modules.SpeedMine;
import myau.module.modules.Sprint;
import myau.module.modules.TargetHUD;
import myau.module.modules.TargetStrafe;
import myau.module.modules.Timer;
import myau.module.modules.Tracers;
import myau.module.modules.Trajectories;
import myau.module.modules.Velocity;
import myau.module.modules.ViewClip;
import myau.module.modules.WTap;
import myau.module.modules.Xray;
import myau.property.Property;
import myau.property.PropertyManager;

public class Myau {
    public static String clientName;
    public static EventManager eventManager;
    public static RotationManager rotationManager;
    public static FloatManager floatManager;
    public static HurtTimeTracker hurtTimeTracker;
    public static BlinkManager blinkManager;
    public static LagManager lagManager;
    public static LagRangeManager lagRangeManager;
    public static PacketStateTracker packetStateTracker;
    public static FriendManager friendManager;
    public static EnemyManager enemyManager;
    public static PropertyManager propertyManager;
    public static ModuleManager moduleManager;
    public static CommandManager commandManager;

    public static void init() {
        clientName = "&7[&cM&6y&ea&au&7]&r ";
        eventManager = new EventManager();
        rotationManager = new RotationManager();
        floatManager = new FloatManager();
        hurtTimeTracker = new HurtTimeTracker();
        blinkManager = new BlinkManager();
        lagManager = new LagManager();
        lagRangeManager = new LagRangeManager();
        packetStateTracker = new PacketStateTracker();
        friendManager = new FriendManager();
        enemyManager = new EnemyManager();
        propertyManager = new PropertyManager();
        moduleManager = new ModuleManager();
        commandManager = new CommandManager();

        eventManager.register(rotationManager);
        eventManager.register(floatManager);
        eventManager.register(hurtTimeTracker);
        eventManager.register(blinkManager);
        eventManager.register(lagManager);
        eventManager.register(lagRangeManager);
        eventManager.register(packetStateTracker);
        eventManager.register(friendManager);
        eventManager.register(enemyManager);
        eventManager.register(propertyManager);
        eventManager.register(moduleManager);
        eventManager.register(commandManager);

        Module[] modules = new Module[]{
            new AimAssist(), new AntiDebuff(), new AntiFireball(), new AntiObbyTrap(), new AntiObfuscate(),
            new AntiVoid(), new AutoClicker(), new AutoHeal(), new AutoTool(), new BedESP(),
            new BedNuker(), new BedTracker(), new Blink(), new Chams(), new ChestESP(),
            new ChestStealer(), new Eagle(), new ESP(), new FastPlace(), new Fly(),
            new Fullbright(), new GhostHand(), new HitSelect(), new HUD(), new Indicators(),
            new InvManager(), new InvWalk(), new ItemESP(), new Jesus(), new KeepSprint(),
            new KillAura(), new LagRange(), new LightningTracker(), new LongJump(), new MCF(),
            new NameTags(), new NickHider(), new NoFall(), new NoHitDelay(), new NoHurtCam(),
            new NoJumpDelay(), new NoRotate(), new NoSlow(), new Reach(), new SafeWalk(),
            new Scaffold(), new Spammer(), new Speed(), new SpeedMine(), new Sprint(),
            new TargetHUD(), new TargetStrafe(), new Timer(), new Tracers(), new Trajectories(),
            new Velocity(), new ViewClip(), new WTap(), new Xray()
        };

        for (Module module : modules) {
            moduleManager.modules.put(module.getClass(), module);
            ArrayList<Property<?>> moduleProperties = new ArrayList<>();
            for (Field field : module.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object value = field.get(module);
                    if (value instanceof Property) {
                        Property<?> property = (Property<?>) value;
                        moduleProperties.add(property);
                        property.registerTo(module);
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            propertyManager.properties.put(module.getClass(), moduleProperties);
            if (module.isEnabled()) {
                module.onEnabled();
            }
            eventManager.register(module);
        }

        commandManager.commands.addAll(Arrays.asList(
            new BindCommand(), new ConfigCommand(), new DenickCommand(), new EnemyCommand(), new FriendCommand(),
            new HelpCommand(), new HideCommand(), new ItemCommand(), new ListCommand(), new PlayerListCommand(),
            new ModuleCommand(), new ShowCommand(), new ToggleCommand(), new UsernameCommand(), new VclipCommand()
        ));

        friendManager.load();
        enemyManager.load();

        Config defaultConfig = new Config("default");
        if (!defaultConfig.load()) {
            defaultConfig.save();
        }
    }
}
