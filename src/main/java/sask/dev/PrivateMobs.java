package sask.dev;

import org.bukkit.plugin.java.JavaPlugin;
import sask.dev.command.PrivateMobCommand;
import sask.dev.command.PrivateMobTab;
import sask.dev.manager.PrivateMobManager;

public class PrivateMobs extends JavaPlugin {

    private PrivateMobManager privateMobManager;

    @Override
    public void onEnable() {
        this.privateMobManager = new PrivateMobManager(this);
        getCommand("privatemobs").setExecutor(new PrivateMobCommand(this));
        getCommand("privatemobs").setTabCompleter(new PrivateMobTab(this));


        getLogger().info("PrivateMobs enabled | SaskDev");
    }

    @Override
    public void onDisable() {
        if (privateMobManager != null) {
            privateMobManager.despawnAll();
        }
        getLogger().info("PrivateMobs disabled");
    }

    public PrivateMobManager getPrivateMobManager() {
        return privateMobManager;
    }
}
