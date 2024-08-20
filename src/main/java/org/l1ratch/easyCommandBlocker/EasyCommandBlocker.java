package org.l1ratch.easyCommandBlocker;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class EasyCommandBlocker extends JavaPlugin implements Listener, TabCompleter {

    private class BlockedCommand {
        String command;
        String message;
        boolean hideFromTab;

        BlockedCommand(String command, String message, boolean hideFromTab) {
            this.command = command;
            this.message = translateColors(message);
            this.hideFromTab = hideFromTab;
        }
    }

    private Map<String, BlockedCommand> blockedCommands;
    private String defaultText;
    private String blockColonCommands;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Загрузка текста по умолчанию из конфигурации и преобразование цветов
        defaultText = translateColors(getConfig().getString("deftxt"));

        // Загрузка настройки блокировки/скрытия команд с двоеточием
        blockColonCommands = getConfig().getString("blockColonCommands", "false").toUpperCase();

        // Загрузка заблокированных команд и их сообщений
        blockedCommands = new HashMap<>();
        List<String> configCommands = getConfig().getStringList("BlockCMD");
        for (String command : configCommands) {
            String[] parts = command.split(",", 3);
            String cmd = parts[0].trim().toLowerCase();
            String message = parts.length > 1 ? parts[1].trim() : "";
            boolean hideFromTab = parts.length > 2 && parts[2].trim().equalsIgnoreCase("tabCompleter: true");
            blockedCommands.put(cmd, new BlockedCommand(cmd, message, hideFromTab));
        }

        getServer().getPluginManager().registerEvents(this, this);

        // Регистрация TabCompleter для всех команд
        for (String command : blockedCommands.keySet()) {
            getCommand(command).setTabCompleter(this);
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();

        // Обработка команд с двоеточием в зависимости от настройки blockColonCommands
        if (message.contains(":")) {
            switch (blockColonCommands) {
                case "BLOCK":
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(translateColors("&cЭта команда заблокирована."));
                    return;
                case "HIDE":
                    // Ничего не делаем, только скрываем команду из таб-комплитера
                    break;
                case "TRUE":
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(translateColors("&cЭта команда заблокирована."));
                    return;
                case "FALSE":
                    // Ничего не делаем, команда разрешена
                    break;
                default:
                    break;
            }
        }

        // Проверяем, начинается ли команда с любой из заблокированных команд
        for (BlockedCommand blockedCommand : blockedCommands.values()) {
            if (message.equalsIgnoreCase("/" + blockedCommand.command)) {
                event.setCancelled(true);
                if (!blockedCommand.message.isEmpty()) {
                    // Если текст сообщения "deftxt", подставляем текст по умолчанию
                    String blockMessage = blockedCommand.message.equalsIgnoreCase("deftxt")
                            ? defaultText
                            : blockedCommand.message;
                    event.getPlayer().sendMessage(blockMessage);
                }
                return;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Проверка на наличие двоеточия в команде в зависимости от настройки blockColonCommands
        if (command.getName().contains(":")) {
            switch (blockColonCommands) {
                case "HIDE":
                case "TRUE":
                    return Collections.emptyList(); // Скрываем команду с двоеточием
                case "BLOCK":
                case "FALSE":
                    return null; // Оставляем команду доступной в таб-комплитере
                default:
                    return null; // Оставляем команду доступной в таб-комплитере
            }
        }

        BlockedCommand blockedCommand = blockedCommands.get(command.getName().toLowerCase());
        if (blockedCommand != null && blockedCommand.hideFromTab) {
            return Collections.emptyList(); // Скрываем заблокированную команду
        }
        return null; // Позволяем серверу предоставить обычные подсказки
    }

    // Метод для преобразования цветовых кодов
    private String translateColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
