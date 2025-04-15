package net.noscape.project.supremetags.guis;

import de.tr7zw.nbtapi.NBTItem;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import net.noscape.project.supremetags.SupremeTags;
import net.noscape.project.supremetags.api.events.TagAssignEvent;
import net.noscape.project.supremetags.api.events.TagBuyEvent;
import net.noscape.project.supremetags.api.events.TagResetEvent;
import net.noscape.project.supremetags.handlers.Tag;
import net.noscape.project.supremetags.handlers.menu.MenuUtil;
import net.noscape.project.supremetags.handlers.menu.Paged;
import net.noscape.project.supremetags.storage.UserData;
import net.noscape.project.supremetags.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.noscape.project.supremetags.utils.Utils.*;

public class TagMenu extends Paged {

    private final Map<String, Tag> tags;

    public TagMenu(MenuUtil menuUtil) {
        super(menuUtil);
        tags = SupremeTags.getInstance().getTagManager().getTags();
    }

    @Override
    public String getMenuName() {
        return format(Objects.requireNonNull(SupremeTags.getInstance().getConfig().getString("gui.tag-menu-none-categories.title")).replaceAll("%page%", String.valueOf(this.getPage())));
    }

    @Override
    public int getSlots() {
        return 27;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {

        Player player = (Player) e.getWhoClicked();

        String back = SupremeTags.getInstance().getConfig().getString("gui.strings.back-item");
        String close = SupremeTags.getInstance().getConfig().getString("gui.strings.close-item");
        String next = SupremeTags.getInstance().getConfig().getString("gui.strings.next-item");
        String reset = SupremeTags.getInstance().getConfig().getString("gui.strings.reset-item");

        String insufficient = SupremeTags.getInstance().getConfig().getString("messages.insufficient-funds");
        String unlocked = SupremeTags.getInstance().getConfig().getString("messages.tag-unlocked");

        if (e.getCurrentItem().getType().equals(Material.valueOf(Objects.requireNonNull(SupremeTags.getInstance().getConfig().getString("gui.layout.glass-material")).toUpperCase()))) {
            e.setCancelled(true);
        }

        if (e.getCurrentItem() == null) {
            e.setCancelled(true);
        }

        ArrayList<String> tag = new ArrayList<>(tags.keySet());

        NBTItem nbt = null;

        if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR) {
            nbt = new NBTItem(e.getCurrentItem());
        }

        if (nbt != null && nbt.hasCustomNbtData() && nbt.hasTag("identifier")) {
            String identifier = nbt.getString("identifier");

            Tag t = SupremeTags.getInstance().getTagManager().getTag(identifier);

            if (t != null) {

                if (!SupremeTags.getInstance().getTagManager().isCost()) {
                    if (!UserData.getActive(player.getUniqueId()).equalsIgnoreCase(identifier) && identifier != null) {
                        if (player.hasPermission(t.getPermission())) {

                            TagAssignEvent tagevent = new TagAssignEvent(player, identifier, false);
                            Bukkit.getPluginManager().callEvent(tagevent);

                            if (tagevent.isCancelled()) return;

                            UserData.setActive(player, tagevent.getTag());

                            super.open();
                            menuUtil.setIdentifier(tagevent.getTag());

                            if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                                msgPlayer(player, SupremeTags.getInstance().getConfig().getString("messages.tag-select-message").replaceAll("%identifier%", identifier).replaceAll("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag()));
                            }
                        } else {
                            msgPlayer(player, SupremeTags.getInstance().getConfig().getString("messages.locked-tag"));
                        }
                    }
                } else {
                    if (player.hasPermission(t.getPermission())) {
                        if (!UserData.getActive(player.getUniqueId()).equalsIgnoreCase(identifier) && identifier != null) {
                            TagAssignEvent tagevent = new TagAssignEvent(player, identifier, false);
                            Bukkit.getPluginManager().callEvent(tagevent);

                            if (tagevent.isCancelled()) return;

                            UserData.setActive(player, tagevent.getTag());
                            super.open();
                            menuUtil.setIdentifier(tagevent.getTag());

                            if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                                msgPlayer(player, SupremeTags.getInstance().getConfig().getString("messages.tag-select-message").replace("%identifier%", identifier).replaceAll("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag()));
                            }
                        }
                    } else {
                        double cost = t.getCost();

                        // check if they have the right amount of money to buy etc....
                        if (hasAmount(player, cost)) {
                            // give them the tag

                            TagBuyEvent tagevent = new TagBuyEvent(player, identifier, cost, false);
                            Bukkit.getPluginManager().callEvent(tagevent);

                            if (tagevent.isCancelled()) return;

                            take(player, cost);
                            addPerm(player, t.getPermission());
                            msgPlayer(player, unlocked.replaceAll("%identifier%", t.getIdentifier()).replaceAll("%tag%", SupremeTags.getInstance().getTagManager().getTag(identifier).getTag()));
                            super.open();
                        } else {
                            msgPlayer(player, insufficient.replaceAll("%cost%", String.valueOf(t.getCost())));
                        }
                    }
                }
            }
        }

        if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(close))) {
            player.closeInventory();
        }

        if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(reset))) {
            if (!SupremeTags.getInstance().getConfig().getBoolean("settings.forced-tag")) {
                TagResetEvent tagEvent = new TagResetEvent(player, false);
                Bukkit.getPluginManager().callEvent(tagEvent);

                if (tagEvent.isCancelled()) return;

                UserData.setActive(player, "None");
                super.open();
                menuUtil.setIdentifier("None");

                if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                    msgPlayer(player, SupremeTags.getInstance().getConfig().getString("messages.reset-message"));
                }
            } else {
                TagResetEvent tagEvent = new TagResetEvent(player, false);
                Bukkit.getPluginManager().callEvent(tagEvent);

                if (tagEvent.isCancelled()) return;

                String defaultTag = SupremeTags.getInstance().getConfig().getString("settings.default-tag");

                UserData.setActive(player, defaultTag);
                super.open();
                menuUtil.setIdentifier(defaultTag);

                if (SupremeTags.getInstance().getConfig().getBoolean("settings.gui-messages")) {
                    msgPlayer(player, SupremeTags.getInstance().getConfig().getString("messages.reset-message"));
                }
            }
        }

        if (e.getCurrentItem().getType().equals(Material.valueOf(Objects.requireNonNull(SupremeTags.getInstance().getConfig().getString("gui.layout.back-next-material")).toUpperCase()))) {
            if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(back))) {
                if (page != 0) {
                    page = page - 1;
                    super.open();
                }
            } else if (e.getCurrentItem().getItemMeta().getDisplayName().equalsIgnoreCase(format(next))) {
                if (!((index + 1) >= tag.size())) {
                    page = page + 1;
                    super.open();
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        getTagItems();
        applyLayout();
    }

    public void getTagItems() {
        Map<String, Tag> tags = SupremeTags.getInstance().getTagManager().getTags();

        ArrayList<Tag> tag = new ArrayList<>();

        if (SupremeTags.getInstance().getConfig().getBoolean("settings.only-show-player-access-tags")) {
            for (Tag t : tags.values()) {
                if (menuUtil.getOwner().hasPermission(t.getPermission())) {
                    tag.add(t);
                }
            }
        } else {
            tag = new ArrayList<>(tags.values());
        }

        if (!tag.isEmpty()) {
            int maxItemsPerPage = 9;

            int startIndex = page * maxItemsPerPage;
            int endIndex = Math.min(startIndex + maxItemsPerPage, tag.size());

            tag.sort((tag1, tag2) -> {
                boolean hasPermission1 = menuUtil.getOwner().hasPermission(tag1.getPermission());
                boolean hasPermission2 = menuUtil.getOwner().hasPermission(tag2.getPermission());

                if (hasPermission1 && !hasPermission2) {
                    return -1; // tag1 comes before tag2
                } else if (!hasPermission1 && hasPermission2) {
                    return 1; // tag2 comes before tag1
                } else {
                    // Sort alphabetically if both tags have permission or both don't
                    return tag1.getIdentifier().compareTo(tag2.getIdentifier());
                }
            });

            currentItemsOnPage = 0;

            for (int i = startIndex; i <= endIndex; i++) {
                if (i > tag.size() - 1) {
                    break;
                }

                Tag t = tag.get(i);
                if (t == null) break;

                if (i == endIndex) {
                    continue;
                }


                String permission = t.getPermission();

                if (!menuUtil.getOwner().hasPermission(permission) &&
                        (!SupremeTags.getInstance().getConfig().getBoolean("settings.locked-view") &&
                                !SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system"))) {
                    continue;
                }

                String displayname;

                if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname") != null) {
                    displayname = Objects.requireNonNull(SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".displayname")).replace("%tag%", t.getTag());
                } else {
                    displayname = format("&7Tag: " + t.getTag());
                }

                if (SupremeTags.getInstance().isPlaceholderAPI()) {
                    displayname = replacePlaceholders(menuUtil.getOwner(), displayname);
                }

                String material;

                if (SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item") != null) {
                    material = SupremeTags.getInstance().getTagManager().getTagConfig().getString("tags." + t.getIdentifier() + ".display-item");
                } else {
                    material = "NAME_TAG";
                }

                assert permission != null;

                ItemStack tagItem;
                ItemMeta tagMeta;
                NBTItem nbt;

                if (material.contains("hdb-")) {
                    int id = Integer.parseInt(material.replace("hdb-", ""));
                    HeadDatabaseAPI api = new HeadDatabaseAPI();
                    tagItem = api.getItemHead(String.valueOf(id));
                    tagMeta = tagItem.getItemMeta();
                } else if (material.contains("basehead-")) {
                    String id = material.replace("basehead-", "");
                    tagItem = createSkull(id);
                    tagMeta = tagItem.getItemMeta();
                } else if (material.contains("itemsadder-")) {
                    String id = material.replace("itemsadder-", "");
                    tagItem = getItemWithIA(id);
                    tagMeta = tagItem.getItemMeta();
                } else {
                    tagItem = new ItemStack(Material.valueOf(material.toUpperCase()), 1);
                    tagMeta = tagItem.getItemMeta();
                }

                nbt = new NBTItem(tagItem);
                nbt.setString("identifier", t.getIdentifier());

                //if (menuUtil.getOwner().hasPermission(t.getPermission()) || permission.equalsIgnoreCase("none")) {
                //    if (SupremeTagsPremium.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".custom-model-data") > 0) {
                //        int modelData = SupremeTagsPremium.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".custom-model-data");
                //        if (tagMeta != null)
                //            tagMeta.setCustomModelData(modelData);
                //    }
                //} else {
                //    if (SupremeTagsPremium.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".locked-tag.custom-model-data") > 0) {
                //        int modelData = SupremeTagsPremium.getInstance().getTagManager().getTagConfig().getInt("tags." + t.getIdentifier() + ".locked-tag.custom-model-data");
                //        if (tagMeta != null)
                //            tagMeta.setCustomModelData(modelData);
                //    }
                //}

                assert tagMeta != null;

                if (UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier()) && SupremeTags.getInstance().getConfig().getBoolean("settings.active-tag-glow")) {
                    tagMeta.addEnchant(Enchantment.KNOCKBACK, 1, true);
                }

                tagMeta.setDisplayName(format(displayname));
                tagMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                if (!isVersionLessThan("1.16")) {
                    tagMeta.addItemFlags(ItemFlag.HIDE_DYE);
                }
                tagMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                tagMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                tagMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

                List<String> lore = getFormattedLore(t, permission);

                String descriptionPlaceholder = "%description%";
                String identifierPlaceholder = "%identifier%";
                String tagPlaceholder = "%tag%";
                String costPlaceholder = "%cost%";

                for (int l = 0; l < lore.size(); l++) {
                    String line = lore.get(l);

                    line = line.replace(identifierPlaceholder, t.getIdentifier());
                    line = line.replace(descriptionPlaceholder, t.getDescription());
                    line = line.replace(tagPlaceholder, t.getTag());
                    line = line.replace(costPlaceholder, String.valueOf(t.getCost()));
                    line = Utils.replacePlaceholders(menuUtil.getOwner(), line);
                    lore.set(l, line);

                }

                tagMeta.setLore(color(lore));

                nbt.getItem().setItemMeta(tagMeta);
                nbt.setString("identifier", t.getIdentifier());

                inventory.addItem(nbt.getItem());

                currentItemsOnPage++;
            }
        }
    }

    private List<String> getFormattedLore(Tag t, String permission) {
        List<String> lore;
        boolean isCostEnabled = SupremeTags.getInstance().getConfig().getBoolean("settings.cost-system");
        boolean hasPermission = menuUtil.getOwner().hasPermission(permission) || permission.equalsIgnoreCase("none");
        boolean isSelected = UserData.getActive(menuUtil.getOwner().getUniqueId()).equalsIgnoreCase(t.getIdentifier());

        String lorePath;

        if (isCostEnabled) {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "locked-lore") : "locked-lore";
        } else {
            lorePath = hasPermission ? (isSelected ? "selected-lore" : "unlocked-lore") : "locked-permission";
        }

        lore = SupremeTags.getInstance().getConfig().getStringList("gui.tag-menu-none-categories.tag-item." + lorePath);

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);

            line = line.replace("%identifier%", t.getIdentifier())
                    .replace("%description%", t.getDescription())
                    .replace("%tag%", t.getTag())
                    .replace("%cost%", String.valueOf(t.getCost()));
            line = Utils.replacePlaceholders(menuUtil.getOwner(), line);
            lore.set(i, line);
        }

        return lore;
    }
}
