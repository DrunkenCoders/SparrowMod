package net.canarymod.api.entity.living.humanoid;

import net.canarymod.Canary;
import net.canarymod.MathHelp;
import net.canarymod.ToolBox;
import net.canarymod.api.CanaryServer;
import net.canarymod.api.GameMode;
import net.canarymod.api.NetServerHandler;
import net.canarymod.api.PlayerListEntry;
import net.canarymod.api.chat.CanaryChatComponent;
import net.canarymod.api.chat.ChatComponent;
import net.canarymod.api.entity.EntityType;
import net.canarymod.api.entity.living.animal.CanaryHorse;
import net.canarymod.api.inventory.CanaryAnimalInventory;
import net.canarymod.api.inventory.CanaryBlockInventory;
import net.canarymod.api.inventory.CanaryEntityInventory;
import net.canarymod.api.inventory.EnderChestInventory;
import net.canarymod.api.inventory.Inventory;
import net.canarymod.api.packet.CanaryPacket;
import net.canarymod.api.packet.Packet;
import net.canarymod.api.world.CanaryWorld;
import net.canarymod.api.world.World;
import net.canarymod.api.world.blocks.*;
import net.canarymod.api.world.position.Direction;
import net.canarymod.api.world.position.Location;
import net.canarymod.chat.Colors;
import net.canarymod.chat.TextFormat;
import net.canarymod.config.Configuration;
import net.canarymod.hook.command.PlayerCommandHook;
import net.canarymod.hook.player.ChatHook;
import net.canarymod.hook.player.TeleportHook;
import net.canarymod.hook.player.TeleportHook.TeleportCause;
import net.canarymod.hook.system.PermissionCheckHook;
import net.canarymod.permissionsystem.PermissionProvider;
import net.canarymod.user.Group;
import net.canarymod.warp.Warp;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.*;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.WorldSettings;
import net.visualillusionsent.utils.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.canarymod.Canary.log;

/**
 * Canary Player wrapper.
 *
 * @author Chris (damagefilter)
 * @author Jason (darkdiplomat)
 */
public class CanaryPlayer extends CanaryHuman implements Player {
    private Pattern badChatPattern = Pattern.compile("[\u2302\u00D7\u00AA\u00BA\u00AE\u00AC\u00BD\u00BC\u00A1\u00AB\u00BB]");
    private List<Group> groups;
    private PermissionProvider permissions;
    private boolean muted;
    private String[] allowedIPs;
    private HashMap<String, String> defaultChatpattern = new HashMap<String, String>();
    //Global chat format setting
    private static String chatFormat = Configuration.getServerConfig().getChatFormat().replace("&", Colors.MARKER);

    public CanaryPlayer(EntityPlayerMP entity) {
        super(entity);
        initPlayerData();
    }

    @Override
    public boolean isPlayer() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public EntityType getEntityType() {
        return EntityType.PLAYER;
    }

    /** {@inheritDoc} */
    @Override
    public String getFqName() {
        return "Player";
    }

    /** {@inheritDoc} */
    @Override
    public void chat(final String message) {
        if (message.length() > 100) {
            kick("Message too long!");
        }
        String out = message.trim();
        Matcher m = badChatPattern.matcher(out);

        if (m.find() && !this.canIgnoreRestrictions()) {
            out = out.replaceAll(m.group(), "");
        }

        if (out.startsWith("/")) {
            executeCommand(out.split(" "));
        }
        else {
            if (isMuted()) {
                notice("You are currently muted!");
            }
            else {
                // This is a copy of the real player list already, no need to copy again (re: Collections.copy())
                List<Player> receivers = Canary.getServer().getPlayerList();
                defaultChatpattern.put("%name", getDisplayName()); // Safe to get name now
                defaultChatpattern.put("%message", out);
                ChatHook hook = (ChatHook) new ChatHook(this, chatFormat, receivers, defaultChatpattern).call();
                if (hook.isCanceled()) {
                    return;
                }
                receivers = hook.getReceiverList();
                String formattedMessage = hook.buildSendMessage();
                for (Player player : receivers) {
                    player.message(formattedMessage);
                }
                log.info(TextFormat.consoleFormat(formattedMessage));
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void message(String message) {
        getNetServerHandler().sendMessage(message);
        // Should cover all chat logging
    }

    /** {@inheritDoc} */
    @Override
    public void notice(String message) {
        message(Colors.LIGHT_RED + message);
    }

    /** {@inheritDoc} */
    @Override
    public Location getSpawnPosition() {
        Location spawn = Canary.getServer().getDefaultWorld().getSpawnLocation();
        ChunkCoordinates loc = getHandle().bL();

        if (loc != null) {
            spawn = new Location(Canary.getServer().getDefaultWorld(), loc.a, loc.b, loc.c, 0.0F, 0.0F);
        }
        return spawn;
    }

    /** {@inheritDoc} */
    @Override
    public Location getHome() {
        Warp home = Canary.warps().getHome(this);

        if (home != null) {
            return home.getLocation();
        }
        return getSpawnPosition();
    }

    /** {@inheritDoc} */
    @Override
    public void setHome(Location home) {
        Canary.warps().setHome(this, home);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasHome() {
        return Canary.warps().getHome(this) != null;
    }

    /** {@inheritDoc} */
    @Override
    public void setSpawnPosition(Location spawn) {
        ChunkCoordinates loc = new ChunkCoordinates((int) spawn.getX(), (int) spawn.getY(), (int) spawn.getZ());

        getHandle().a(loc, true);
    }

    /** {@inheritDoc} */
    @Override
    public String getIP() {
        String ip = getHandle().a.a.b().toString();

        return ip.substring(1, ip.lastIndexOf(":"));
    }

    public String getPreviousIP() {
        if (getMetaData() != null && getMetaData().containsKey("PreviousIP")) {
            return getMetaData().getString("PreviousIP");
        }
        return "UNKNOWN";
    }

    /** {@inheritDoc} */
    @Override
    public boolean executeCommand(String[] command) {
        try {
            boolean toRet = true;
            PlayerCommandHook hook = (PlayerCommandHook) new PlayerCommandHook(this, command).call();
            if (hook.isCanceled()) {
                return true;
            } // someone wants us not to execute the command. So lets do them the favor

            String cmdName = command[0];
            if (cmdName.startsWith("/")) {
                cmdName = cmdName.substring(1);
            }
            if (!Canary.commands().parseCommand(this, cmdName, command)) {
                if (Canary.ops().isOpped(getName())) {
                    toRet = ((CanaryServer) Canary.getServer()).getHandle().H().a(this.getHandle(), StringUtils.joinString(command, " ", 0)) > 0; // Vanilla Commands passed
                }
            }
            if (toRet) {
                log.info("Command used by " + getName() + ": " + StringUtils.joinString(command, " ", 0));
            }
            return toRet;
        }
        catch (Throwable ex) {
            log.error("Exception in command handler: ", ex);
            if (isAdmin()) {
                message(Colors.LIGHT_RED + "Exception occured. " + ex.getMessage());
            }
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sendPacket(Packet packet) {
        getHandle().a.a(((CanaryPacket) packet).getPacket());
    }

    /** {@inheritDoc} */
    @Override
    public Group getGroup() {
        return groups.get(0);
    }

    /** {@inheritDoc} */
    @Override
    public Group[] getPlayerGroups() {
        return groups.toArray(new Group[groups.size()]);
    }

    /** {@inheritDoc} */
    @Override
    public void setGroup(Group group) {
        groups.set(0, group);
        Canary.usersAndGroups().addOrUpdatePlayerData(this);
        defaultChatpattern.put("%prefix", getPrefix()); // Update Prefix
    }

    /** {@inheritDoc} */
    @Override
    public void addGroup(Group group) {
        if (!groups.contains(group)) {
            groups.add(group);
            Canary.usersAndGroups().addOrUpdatePlayerData(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasPermission(String permission) {
        if (Canary.ops().isOpped(getName())) {
            return true;
        }
        PermissionCheckHook hook = new PermissionCheckHook(permission, this, false);
        // If player has the permission set, use its personal permissions
        if (permissions.pathExists(permission)) {
            hook.setResult(permissions.queryPermission(permission));
            Canary.hooks().callHook(hook);
            return hook.getResult();
        }
        // Only main group is set
        else if (groups.size() == 1) {
            hook.setResult(groups.get(0).hasPermission(permission));
            Canary.hooks().callHook(hook);
            return hook.getResult();
        }

        // Check sub groups
        for (int i = 1; i < groups.size(); i++) {
            // First group that
            if (groups.get(i).getPermissionProvider().pathExists(permission)) {
                hook.setResult(groups.get(i).hasPermission(permission));
                Canary.hooks().callHook(hook);
                return hook.getResult();
            }
        }

        // No subgroup has permission defined, use what base group has to say
        hook.setResult(groups.get(0).hasPermission(permission));
        Canary.hooks().callHook(hook);
        return hook.getResult();
    }

    /** {@inheritDoc} */
    @Override
    public boolean safeHasPermission(String permission) {
        if (Canary.ops().isOpped(getName())) {
            return true;
        }
        // If player has the permission set, use its personal permissions
        if (permissions.pathExists(permission)) {
            return permissions.queryPermission(permission);
        }
        // Only main group is set
        else if (groups.size() == 1) {
            return groups.get(0).hasPermission(permission);
        }

        // Check sub groups
        for (int i = 1; i < groups.size(); i++) {
            // First group that
            if (groups.get(i).getPermissionProvider().pathExists(permission)) {
                return groups.get(i).hasPermission(permission);
            }
        }

        // No subgroup has permission defined, use what base group has to say
        return groups.get(0).hasPermission(permission);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAdmin() {
        return hasPermission("canary.super.administrator") || Canary.ops().isOpped(getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean canBuild() {
        return hasPermission("canary.world.build") || hasPermission("canary.vanilla.op") || isAdmin();
    }

    /** {@inheritDoc} */
    @Override
    public void setCanBuild(boolean canModify) {
        permissions.addPermission("canary.world.build", canModify);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canIgnoreRestrictions() {
        return hasPermission("canary.super.ignoreRestrictions");
    }

    /** {@inheritDoc} */
    @Override
    public void setCanIgnoreRestrictions(boolean canIgnore) {
        permissions.addPermission("canary.super.ignoreRestrictions", canIgnore, -1);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMuted() {
        return muted;
    }

    /** {@inheritDoc} */
    @Override
    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    /** {@inheritDoc} */
    @Override
    public PermissionProvider getPermissionProvider() {
        return permissions;
    }

    /** {@inheritDoc} */
    @Override
    public EnderChestInventory getEnderChestInventory() {
        return ((EntityPlayerMP) entity).getEnderChestInventory();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInGroup(Group group, boolean parents) {
        for (Group g : groups) {
            if (g.getName().equals(group.getName())) {
                return true;
            }
            if (parents) {
                for (Group parent : g.parentsToList()) {
                    if (parent.getName().equals(group.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void kick(String reason) {
        getHandle().a.c(reason);
    }

    public void kickNoHook(String reason) {
        getHandle().a.kickNoHook(reason);
    }

    /** {@inheritDoc} */
    @Override
    public NetServerHandler getNetServerHandler() {
        return getHandle().getServerHandler();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInGroup(String group, boolean parents) {
        for (Group g : groups) {
            if (g.getName().equals(group)) {
                return true;
            }
            if (parents) {
                for (Group parent : g.parentsToList()) {
                    if (parent.getName().equals(group)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getAllowedIPs() {
        return allowedIPs;
    }

    /** {@inheritDoc} */
    @Override
    public Direction getCardinalDirection() {
        double degrees = (getRotation() - 180) % 360;

        if (degrees < 0) {
            degrees += 360.0;
        }

        if (0 <= degrees && degrees < 22.5) {
            return Direction.NORTH;
        }
        else if (22.5 <= degrees && degrees < 67.5) {
            return Direction.NORTHEAST;
        }
        else if (67.5 <= degrees && degrees < 112.5) {
            return Direction.EAST;
        }
        else if (112.5 <= degrees && degrees < 157.5) {
            return Direction.SOUTHEAST;
        }
        else if (157.5 <= degrees && degrees < 202.5) {
            return Direction.SOUTH;
        }
        else if (202.5 <= degrees && degrees < 247.5) {
            return Direction.SOUTHWEST;
        }
        else if (247.5 <= degrees && degrees < 292.5) {
            return Direction.WEST;
        }
        else if (292.5 <= degrees && degrees < 337.5) {
            return Direction.NORTHWEST;
        }
        else if (337.5 <= degrees && degrees < 360.0) {
            return Direction.NORTH;
        }
        else {
            return Direction.ERROR;
        }

    }

    /** {@inheritDoc} */
    @Override
    public void initPlayerData() {
        String[] data = Canary.usersAndGroups().getPlayerData(getName());
        Group[] subs = Canary.usersAndGroups().getModuleGroupsForPlayer(getName());
        groups = new LinkedList<Group>();
        groups.add(Canary.usersAndGroups().getGroup(data[1]));
        for (Group g : subs) {
            if (g != null) {
                groups.add(g);
            }
        }

        permissions = Canary.permissionManager().getPlayerProvider(getName(), getWorld().getFqName());
        if (data[0] != null && (!data[0].isEmpty() && !data[0].equals(" "))) {
            prefix = ToolBox.stringToNull(data[0]);
        }

        if (data[2] != null && !data[2].isEmpty()) {
            muted = Boolean.valueOf(data[2]);
        }
        //defaultChatpattern.put("%name", getDisplayName()); // Display name not initialized at this time
        defaultChatpattern.put("%prefix", getPrefix());
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeGroup(Group group) {
        if (groups.get(0).equals(group)) {
            return false;
        }
        boolean success = groups.remove(group);
        if (success) {
            Canary.usersAndGroups().addOrUpdatePlayerData(this);
        }
        return success;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeGroup(String group) {
        Group g = Canary.usersAndGroups().getGroup(group);
        if (g == null) {
            return false;
        }
        return removeGroup(g);
    }

    /** {@inheritDoc} */
    @Override
    public int getPing() {
        return getHandle().i;
    }

    /** {@inheritDoc} */
    @Override
    public PlayerListEntry getPlayerListEntry(boolean shown) {
        return new PlayerListEntry(this, shown);
    }

    /** {@inheritDoc} */
    @Override
    public void sendPlayerListEntry(PlayerListEntry plentry) {
        if (Configuration.getServerConfig().isPlayerListEnabled()) {
            getHandle().a.a(new S38PacketPlayerListItem(plentry.getName(), plentry.isShown(), plentry.getPing()));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addExhaustion(float exhaustion) {
        getHandle().bO().a(exhaustion);
    }

    /** {@inheritDoc} */
    @Override
    public void setExhaustion(float exhaustion) {
        getHandle().bO().setExhaustionLevel(exhaustion);
    }

    /** {@inheritDoc} */
    @Override
    public float getExhaustionLevel() {
        return getHandle().bO().getExhaustionLevel();
    }

    /** {@inheritDoc} */
    @Override
    public void setHunger(int hunger) {
        getHandle().bO().setFoodLevel(hunger);
    }

    /** {@inheritDoc} */
    @Override
    public int getHunger() {
        return getHandle().bO().a();
    }

    /** {@inheritDoc} */
    @Override
    public void addExperience(int experience) {
        getHandle().addXP(experience);
    }

    /** {@inheritDoc} */
    @Override
    public void removeExperience(int experience) {
        getHandle().removeXP(experience);
    }

    /** {@inheritDoc} */
    @Override
    public int getExperience() {
        return getHandle().bH;
    }

    /** {@inheritDoc} */
    @Override
    public void setExperience(int xp) {
        if (xp < 0) {
            return;
        }
        getHandle().setXP(xp);
    }

    /** {@inheritDoc} */
    @Override
    public int getLevel() {
        return getHandle().bG;
    }

    /** {@inheritDoc} */
    @Override
    public void setLevel(int level) {
        this.setExperience(ToolBox.levelToExperience(level));
    }

    /** {@inheritDoc} */
    @Override
    public void addLevel(int level) {
        this.addExperience(ToolBox.levelToExperience(level));
    }

    /** {@inheritDoc} */
    @Override
    public void removeLevel(int level) {
        this.removeExperience(ToolBox.levelToExperience(level));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSleeping() {
        return getHandle().bm();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDeeplySleeping() {
        return getHandle().bJ();
    }

    /** {@inheritDoc} */
    @Override
    public int getModeId() {
        return getHandle().c.b().a();
    }

    /** {@inheritDoc} */
    @Override
    public GameMode getMode() {
        return GameMode.fromId(getHandle().c.b().a());
    }

    /** {@inheritDoc} */
    @Override
    public void setModeId(int mode) {
        // Adjust mode, make it null if number is invalid
        WorldSettings.GameType gt = WorldSettings.a(mode);
        EntityPlayerMP ent = getHandle();

        if (ent.c.b() != gt) {
            ent.c.a(gt);
            ent.a.a(new S2BPacketChangeGameState(3, mode));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setMode(GameMode mode) {
        this.setModeId(mode.getId());
    }

    /** {@inheritDoc} */
    @Override
    public void refreshCreativeMode() {
        if (getModeId() == 1 || Configuration.getWorldConfig(getWorld().getFqName()).getGameMode() == GameMode.CREATIVE) {
            getHandle().c.a(WorldSettings.a(1));
        }
        else {
            getHandle().c.a(WorldSettings.a(0));
        }
    }


    /** {@inheritDoc} */
    @Override
    public void teleportTo(double x, double y, double z, float pitch, float rotation) {
        this.teleportTo(x, y, z, pitch, rotation, getWorld());
    }

    /** {@inheritDoc} */
    @Override
    public void teleportTo(double x, double y, double z, float pitch, float rotation, World dim) {
        this.teleportTo(x, y, z, pitch, rotation, dim, TeleportHook.TeleportCause.PLUGIN);
    }

    /** {@inheritDoc} */
    @Override
    public void teleportTo(Location location, TeleportCause cause) {
        this.teleportTo(location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getRotation(), location.getWorld(), cause);
    }

    protected void teleportTo(double x, double y, double z, float pitch, float rotation, World world, TeleportHook.TeleportCause cause) {
        // If in a vehicle - eject before teleporting.
        if (isRiding()) {
            dismount();
        }
        if (!world.equals(this.getWorld())) {
            Canary.getServer().getConfigurationManager().switchDimension(this, world, false);
        }

        getHandle().a.a(x, y, z, rotation, pitch, getWorld().getType().getId(), getWorld().getName(), cause);
    }

    /** {@inheritDoc} */
    @Override
    public String getPrefix() {
        if (prefix != null) {
            return prefix;
        }
        else if (groups.get(0).getPrefix() != null) {
            return groups.get(0).getPrefix();
        }
        else {
            return Colors.WHITE;
        }
    }

    @Override
    public boolean isOnline() {
        return Canary.getServer().getPlayer(getName()) != null;
    }

    /** {@inheritDoc} */
    @Override
    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
        Canary.usersAndGroups().addOrUpdatePlayerData(this);
        this.defaultChatpattern.put("%prefix", getPrefix());
    }

    /** {@inheritDoc} */
    @Override
    public void updateCapabilities() {
        this.sendPacket(new CanaryPacket(new S39PacketPlayerAbilities(((CanaryHumanCapabilities) getCapabilities()).getHandle())));
    }

    /** {@inheritDoc} */
    @Override
    public void openInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }
        switch (inventory.getInventoryType()) {
            case CHEST:
                ContainerChest chest = new ContainerChest(getHandle().bn, ((CanaryBlockInventory) inventory).getInventoryHandle());
                chest.setInventory(inventory);
                getHandle().openContainer(chest, 0, inventory.getSize(), ((CanaryBlockInventory) inventory).getInventoryHandle().k_());
                return;
            case CUSTOM:
            case MINECART_CHEST:
                ContainerChest eChest = new ContainerChest(getHandle().bn, ((CanaryEntityInventory) inventory).getHandle());
                eChest.setInventory(inventory);
                getHandle().openContainer(eChest, 0, inventory.getSize(), ((CanaryEntityInventory) inventory).getHandle().k_());
                return;
            case ANVIL:
                getHandle().openContainer(((CanaryAnvil) inventory).getContainer(), 8, 9);
                return;
            case ANIMAL:
                getHandle().openContainer(new ContainerHorseInventory(getHandle().bn, ((CanaryAnimalInventory) inventory).getHandle(), ((CanaryHorse) ((CanaryAnimalInventory) inventory).getOwner()).getHandle()), 11, inventory.getSize(), ((CanaryEntityInventory) inventory).getHandle().k_());
                return;
            case BEACON:
                getHandle().openContainer(new ContainerBeacon(getHandle().bn, ((CanaryBeacon) inventory).getTileEntity()), 7, inventory.getSize(), ((CanaryBlockInventory) inventory).getInventoryHandle().k_());
                return;
            case BREWING:
                getHandle().openContainer(new ContainerBrewingStand(getHandle().bn, ((CanaryBrewingStand) inventory).getTileEntity()), 5, inventory.getSize(), ((CanaryBlockInventory) inventory).getInventoryHandle().k_());
                return;
            case DISPENSER:
                getHandle().openContainer(new ContainerDispenser(getHandle().bn, ((CanaryDispenser) inventory).getTileEntity()), inventory instanceof CanaryDropper ? 10 : 3, inventory.getSize(), ((CanaryBlockInventory) inventory).getInventoryHandle().k_());
                return;
            case ENCHANTMENT:
                getHandle().openContainer(((CanaryEnchantmentTable) inventory).getContainer(), 4, inventory.getSize());
                return;
            case FURNACE:
                getHandle().openContainer(new ContainerFurnace(getHandle().bn, ((CanaryFurnace) inventory).getTileEntity()), 2, inventory.getSize(), ((CanaryBlockInventory) inventory).getInventoryHandle().k_());
                return;
            case HOPPER:
                getHandle().openContainer(new ContainerHopper(getHandle().bn, ((CanaryHopperBlock) inventory).getTileEntity()), 9, inventory.getSize(), ((CanaryBlockInventory) inventory).getInventoryHandle().k_());
                return;
            case MINECART_HOPPER:
                getHandle().openContainer(new ContainerHopper(getHandle().bn, ((CanaryEntityInventory) inventory).getHandle()), 9, inventory.getSize(), ((CanaryEntityInventory) inventory).getHandle().k_());
                return;
            case WORKBENCH:
                getHandle().openContainer(((CanaryWorkbench) inventory).getContainer(), 1, 9);
                return;
            default:
                return;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void createAndOpenWorkbench() {

        Inventory bench_inv = new ContainerWorkbench(getHandle().bn, ((CanaryWorld) getWorld()).getHandle(), -1, -1, -1).getInventory();
        bench_inv.setCanOpenRemote(true);
        openInventory(bench_inv);
    }

    /** {@inheritDoc} */
    @Override
    public void createAndOpenAnvil() {
        Inventory anvil_inv = new ContainerRepair(getHandle().bn, ((CanaryWorld) getWorld()).getHandle(), -1, -1, -1, getHandle()).getInventory();
        anvil_inv.setCanOpenRemote(true);
        openInventory(anvil_inv);
    }

    /** {@inheritDoc} */
    @Override
    public void createAndOpenEnchantmentTable(int bookshelves) {
        CanaryEnchantmentTable ench_inv = (CanaryEnchantmentTable) new ContainerEnchantment(getHandle().bn, ((CanaryWorld) getWorld()).getHandle(), -1, -1, -1).getInventory();
        ench_inv.setCanOpenRemote(true);
        ench_inv.fakeCaseCount = MathHelp.setInRange(bookshelves, 0, 15);
        openInventory(ench_inv);
    }

    /** {@inheritDoc} */
    @Override
    public void closeWindow() {
        getHandle().k();
    }

    /** {@inheritDoc} */
    @Override
    public void openSignEditWindow(Sign sign) {
        getHandle().a(((CanarySign) sign).getTileEntity());
    }

    /** {@inheritDoc} */
    @Override
    public String getFirstJoined() {
        return getHandle().getFirstJoined();
    }

    @Override
    public String getLastJoined() {
        return getHandle().getLastJoined();
    }

    /** {@inheritDoc} */
    @Override
    public long getTimePlayed() {
        return getHandle().getTimePlayed();
    }

    /** {@inheritDoc} */
    @Override
    public String getLocale() {
        return getHandle().bN;
    }

    /** {@inheritDoc} */
    @Override
    public void sendChatComponent(ChatComponent chatComponent) {
        this.sendPacket(new CanaryPacket(new S02PacketChat(((CanaryChatComponent) chatComponent).getNative())));
    }

    public void resetNativeEntityReference(EntityPlayerMP entityPlayerMP) {
        this.entity = entityPlayerMP;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Player[name=%s]", getName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Player)) {
            return false;
        }
        final Player other = (Player) obj;

        return getName().equals(other.getName());
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int hash = 7;

        hash = 89 * hash + (this.getName() != null ? this.getName().hashCode() : 0);
        return hash;
    }

    /** {@inheritDoc} */
    @Override
    public EntityPlayerMP getHandle() {
        return (EntityPlayerMP) entity;
    }
}
