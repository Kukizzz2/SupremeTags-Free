package net.noscape.project.supremetags.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.noscape.project.supremetags.SupremeTags;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.awt.Color;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utils {

    private static Pattern p1 = Pattern.compile("\\{#([0-9A-Fa-f]{6})\\}");
    private static Pattern p2 = Pattern.compile("&#([A-Fa-f0-9]){6}");
    private static Pattern p3 = Pattern.compile("#([A-Fa-f0-9]){6}");

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###.#");

    public static String format(String message) {
        // Check if the server version is less than 1.16
        if (isVersionLessThan("1.16")) {
            // Now translate normal color codes
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
        } else {
            // Server is 1.16 or above, so process both hex and normal color codes
            if (SupremeTags.getInstance().isCmiHex()) {
                Matcher match = p1.matcher(message);
                while (match.find()) {
                    getRGB(message);
                }
                return ChatColor.translateAlternateColorCodes('&', message);
            } else if (SupremeTags.getInstance().isLegacyFormat()) {
                message = message.replace(">>", "").replace("<<", "");
                Matcher matcher = p2.matcher(message);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    ChatColor hexColor = ChatColor.of(matcher.group().substring(1));
                    matcher.appendReplacement(sb, hexColor.toString());
                }
                matcher.appendTail(sb);
                return ChatColor.translateAlternateColorCodes('&', sb.toString());
            } else {
                Pattern pattern = p3;
                for (Matcher matcher = pattern.matcher(message); matcher.find(); matcher = pattern.matcher(message)) {
                    String color = message.substring(matcher.start(), matcher.end());
                    message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color) + ""); // You're missing this replacing
                }
                message = ChatColor.translateAlternateColorCodes('&', message); // Translates any & codes too
                return message;
            }
        }
    }

    public static boolean isValidVersion(String version) {
        return version.matches("\\d+(\\.\\d+)*"); // Matches version strings like "1", "1.2", "1.2.3", etc.
    }

    public static boolean isVersionLessThan(String version) {
        String serverVersion = Bukkit.getVersion();
        String[] serverParts = serverVersion.split(" ")[2].split("\\.");
        String[] targetParts = version.split("\\.");

        for (int i = 0; i < Math.min(serverParts.length, targetParts.length); i++) {
            if (!isValidVersion(serverParts[i]) || !isValidVersion(targetParts[i])) {
                // Handle invalid version format
                return false;
            }

            int serverPart = Integer.parseInt(serverParts[i]);
            int targetPart = Integer.parseInt(targetParts[i]);

            if (serverPart < targetPart) {
                return true;
            } else if (serverPart > targetPart) {
                return false;
            }
        }
        return serverParts.length < targetParts.length;
    }

    public static void addPerm(OfflinePlayer player, String permission) {
        for (World world : Bukkit.getWorlds())
            SupremeTags.getPermissions().playerAdd(world.getName(), player, permission);
    }

    public static void removePerm(OfflinePlayer player, String permission) {
        for (World world : Bukkit.getWorlds())
            SupremeTags.getPermissions().playerRemove(world.getName(), player, permission);
    }

    public static boolean hasAmount(Player player, double cost) {
        return SupremeTags.getEconomy().has(player, cost);
    }

    public static void take(Player player, double cost) {
        SupremeTags.getEconomy().withdrawPlayer(player, cost);
    }

    public static String deformat(String str) {
        return ChatColor.stripColor(format(str));
    }

    public static void msgPlayer(Player player, String... str) {
        for (String msg : str) {
            player.sendMessage(format(msg));
        }
    }

    public static void msgPlayer(CommandSender player, String... str) {
        for (String msg : str) {
            player.sendMessage(format(msg));
        }
    }

    public static void titlePlayer(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
    }

    public static void soundPlayer(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static List<String> color(List<String> lore){
        return lore.stream().map(Utils::format).collect(Collectors.toList());
    }

    private static Pattern rgbPat = Pattern.compile("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)");
    public static String getRGB(String msg) {
        String temp = msg;
        try {

            String status = "none";
            String r = "";
            String g = "";
            String b = "";
            Matcher match = rgbPat.matcher(msg);
            while (match.find()) {
                String color = msg.substring(match.start(), match.end());
                for (char character : msg.substring(match.start(), match.end()).toCharArray()) {
                    switch (character) {
                        case '(':
                            status = "r";
                            continue;
                        case ',':
                            switch (status) {
                                case "r":
                                    status = "g";
                                    continue;
                                case "g":
                                    status = "b";
                                    continue;
                                default:
                                    break;
                            }
                        default:
                            switch (status) {
                                case "r":
                                    r = r + character;
                                    continue;
                                case "g":
                                    g = g + character;
                                    continue;
                                case "b":
                                    b = b + character;
                                    continue;
                            }
                            break;
                    }


                }
                b = b.replace(")", "");
                Color col = new Color(Integer.parseInt(r), Integer.parseInt(g), Integer.parseInt(b));
                temp = temp.replaceFirst("(?:#|0x)(?:[a-f0-9]{3}|[a-f0-9]{6})\\b|(?:rgb|hsl)a?\\([^\\)]*\\)", ChatColor.of(col) + "");
                r = "";
                g = "";
                b = "";
                status = "none";
            }
        } catch (Exception e) {
            return msg;
        }
        return temp;
    }

    public static String replacePlaceholders(Player user, String base) {
        if (SupremeTags.getInstance().getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return base;
        }

        return PlaceholderAPI.setPlaceholders(user, base);
    }

    public static ItemStack createSkull(String baseheadtexture64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);

        if (baseheadtexture64 == null || baseheadtexture64.isEmpty()) {
            return skull;
        }

        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        assert skullMeta != null;
        skullMeta.setOwningPlayer(null);

        GameProfile profile = new GameProfile(UUID.randomUUID(), "Dummy");
        profile.getProperties().put("textures", new Property("textures", baseheadtexture64));

        try {
            Field profileField = skullMeta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(skullMeta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        skull.setItemMeta(skullMeta);
        return skull;
    }

}