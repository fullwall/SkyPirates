package com.fullwall.skypirates;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftBoat;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_8_R3.EntityBoat;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityPlayer;

public class BoatHandler {
    public final Boat boat;

    public long delay = 0;

    private boolean goingDown = false;

    private boolean goingUp = false;
    private Mode mode;

    private double throttle = 1D;

    public BoatHandler(Boat newBoat, Mode newMode) {
        boat = newBoat;
        boat.setWorkOnLand(true);
        mode = newMode;
    }

    private void changeThrottle(double change) {
        throttle += change;
        if (throttle >= 2.5D) {
            throttle = 2.5D;
        } else if (throttle <= 0D) {
            throttle = 0D;
        }
    }

    public void doArmSwing() {
        if (mode != Mode.GLIDER && getInHandType() == Material.DIAMOND
                && Permission.has(getPlayer(), "skypirates.items.diamond")) {
            changeThrottle(0.25);
            getPlayer().sendMessage(ChatColor.YELLOW + "The boat " + ChatColor.DARK_RED + "speeds up."
                    + ChatColor.YELLOW + " Your speed is now " + throttle + "x of its original.");
        } else {
            if (mode == Mode.NORMAL && delay == 0 && isGrounded()) {
                if (getInHandType() == Material.COAL && Permission.has(getPlayer(), "skypirates.items.coal")) {
                    setMotionY(0.65D);
                    delay = System.currentTimeMillis() + 750;
                } else {
                    setMotionY(0.4D);
                    delay = System.currentTimeMillis();
                }
            } else if (mode == Mode.PLANE) {
                if (getInHandType() == Material.COAL && Permission.has(getPlayer(), "skypirates.items.coal")) {
                    goingUp = true;
                    setMotionY(0.25D);
                } else {
                    goingUp = true;
                    setMotionY(0.25D);
                }
                delay = System.currentTimeMillis() + 80;
            } else if (mode == Mode.SUBMARINE
                    && ((getBlockIdBeneath() == Material.WATER || getBlockIdBeneath() == Material.STATIONARY_WATER)
                            || (getBlockIn() == Material.WATER || getBlockIn() == Material.STATIONARY_WATER))) {
                goingUp = true;
                setMotionY(0.1D);
            } else if (mode == Mode.GLIDER && getBlockIdBeneath() != Material.WATER
                    && getBlockIdBeneath() != Material.STATIONARY_WATER) {
                speedUpBoat(10, boat.getVelocity());
            }
        }
    }

    private void doDrill() {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 4; y >= 1; y--) {
                    Block block = boat.getWorld().getBlockAt(boat.getLocation().getBlockX() - x,
                            boat.getLocation().getBlockY() - y, boat.getLocation().getBlockZ() - z);
                    if (block.getType() != Material.AIR && block.getType() != Material.LAVA
                            && block.getType() != Material.STATIONARY_WATER
                            && block.getType() != Material.STATIONARY_LAVA && block.getType() != Material.WATER) {
                        Material mat = block.getType();
                        block.setType(Material.AIR);
                        boat.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(mat, 1));
                    }
                }
            }
        }
    }

    private void doFlying(Vector vel) {
        if (goingUp) {
            // vel.setY(vel.getY() - 0.02);
            if (vel.getY() <= 0D) {
                goingUp = false;
                vel.setY(0D);
            }
            return;
        }
        if (goingDown) {
            if (vel.getY() <= 0D) {
                vel.setY(vel.getY() + 0.25);
                if (vel.getY() >= 0D)
                    goingDown = false;
            }
        } else if (vel.getY() <= 0D) {
            // workaround for minecraft glitch - setting motion to 0 still
            // results in downward moving. Not perfect.
            if (boat.getVelocity().getY() <= 0 && boat.getVelocity().getY() >= DOWNWARD_DRIFT) {
                vel.setY(COMPENSATION);
            } else {
                vel.setY(0D);
            }
        } else {
            // see above.
            if (boat.getVelocity().getY() <= 0 && boat.getVelocity().getY() >= DOWNWARD_DRIFT) {
                vel.setY(COMPENSATION);
            } else
                vel.setY(0D);
        }
    }

    private void doGlider(Vector vel) {
        if (getBlockIdBeneath() == Material.AIR)
            vel.setY(-0.075D);
        if (vel.getY() < -0.075D)
            vel.setY(-0.075D);
    }

    private void doHover(Vector vel) {
        int maxHeight = 1;
        if (getInHandType() == Material.COAL && Permission.has(getPlayer(), "skypirates.items.coal")) {
            maxHeight = 2;
        }

        int x = boat.getLocation().getBlockX();
        int y = boat.getLocation().getBlockY();
        int z = boat.getLocation().getBlockZ();

        boolean goDown = false;
        int blockY = 0;
        Block block = null;

        for (int i = 0; i < maxHeight + 100; i++) {
            block = boat.getWorld().getBlockAt(x, y - blockY, z);
            if (block.getType() == Material.AIR)
                blockY += 1;
            else if (block.getType() == Material.WATER)
                break;
            else
                break;
            if (i > maxHeight + 1)
                goDown = true;
        }
        // if (stop == false)
        int hoverHeight = block.getY() + (maxHeight * 2);

        if (boat.getLocation().getY() < hoverHeight + 0.6) {
            vel.setY(0.1D);
            return;
        } else if (goDown == true && boat.getLocation().getY() > hoverHeight + 0.6) {
            vel.setY(-0.1D);
            return;
        } else {
            if (boat.getVelocity().getY() <= 0 && boat.getVelocity().getY() >= DOWNWARD_DRIFT) {
                vel.setY(COMPENSATION);
            } else {
                vel.setY(0D);
            }
            return;
        }
    }

    private void doNormal(Vector vel) {
        boat.setOccupiedDeceleration(1);
        Vector playerVelocity = this.boat.getPassenger().getVelocity();
        double playerVelocityX = playerVelocity.getX();
        double playerVelocityZ = playerVelocity.getZ();

        if ((playerVelocityX != 0D || playerVelocityZ != 0D) && isGrounded()) {
            // setMotion(playerVelocityX * 3.2, vel.getY(), playerVelocityZ *
            // 3.2);
            speedUpBoat(10, vel);
        }

        double currentX = vel.getX();
        double currentZ = vel.getZ();
        boolean boostSteering = false;

        if ((playerVelocityX < 0 && currentX > 0) || (playerVelocityX > 0 && currentX < 0)) {
            boostSteering = true;
        }
        if (!boostSteering && (playerVelocityZ < 0 && currentZ > 0) || (playerVelocityZ > 0 && currentZ < 0)) {
            boostSteering = true;
        }
        if (boostSteering) {
            currentX = currentX / 1.2D + playerVelocityX;
            currentZ = currentZ / 1.2D + playerVelocityZ;
        }
    }

    public void doRealisticFriction() {
        if (getPlayer() == null) {
            setMotion(getMotionX() * 0.53774, getMotionY(), getMotionZ() * 0.53774);
        }
    }

    public void doRightClick() {
        if (getInHandType() == Material.DIAMOND && Permission.has(getPlayer(), "skypirates.items.diamond")) {
            changeThrottle(-0.25D);
            getPlayer().sendMessage(
                    ChatColor.BLUE + "The boat slows. Your speed is now " + throttle + "x of its original.");
        } else if (getInHandType() == Material.ARROW && Permission.has(getPlayer(), "skypirates.items.arrow")) {
            getPlayer().launchProjectile(Arrow.class);
        } else if (mode == Mode.PLANE && getInHandType() == Material.TNT
                && Permission.has(getPlayer(), "skypirates.planes.tnt")) {
            Item item = getPlayer().getWorld().dropItemNaturally(getPlayer().getLocation(),
                    new ItemStack(Material.TNT, 1));
            Bukkit.getScheduler().scheduleSyncDelayedTask(SkyPirates.plugin, new DropTNT(item), 20);
        } else
            if (getInHandType() == Material.SNOW_BLOCK && Permission.has(getPlayer(), "skypirates.items.snowblock")) {
            boat.setVelocity(new Vector(0, 0, 0));
            throttle = 1;
            getPlayer().sendMessage(
                    ChatColor.DARK_RED + "The boat stops with a sudden jolt. Your speed is now only 1x original.");
        } else if (mode == Mode.PLANE) {
            goingDown = true;
            setMotionY(-0.27D);
        } else if (mode == Mode.SUBMARINE) {
            goingDown = true;
            setMotionY(-0.2D);
        } else if (mode == Mode.DRILL) {
            doDrill();
        }

    }

    private void doUnderwater(Vector vel) {
        // apply 'gravity' - aims to just be gentle downward motion
        Player p = getPlayer();

        vel.setY(vel.getY() - 0.03);

        // cap y velocity to combat buoyancy
        if (vel.getY() > MAX_BUOYANCY) {
            vel.setY(MAX_BUOYANCY);
        }
        if (!goingUp && vel.getY() > 0)
            vel.setY(-0.15D);

        // stop players from drowning underwater
        if ((p.getRemainingAir() != p.getMaximumAir() && (Permission.has(p, "skypirates.player.air"))
                || (p.getInventory().getHelmet() != null && helmets.contains(p.getInventory().getHelmet().getType())
                        && Permission.has(p, "skypirates.items.helmets")))) {
            p.setRemainingAir(p.getMaximumAir());
            p.setMaximumAir(p.getMaximumAir());
            if (!p.hasPotionEffect(PotionEffectType.WATER_BREATHING)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 1));
            }
        }
        if (goingUp) {
            vel.setY(vel.getY() - 0.009);
            if (vel.getY() <= 0.025D) {
                goingUp = false;
                vel.setY(0D);
            }
        } else if (goingDown) {
            if (vel.getY() <= -0.6D) {
                vel.setY(-0.6D);
                if (vel.getY() >= 0D)
                    goingDown = false;
            }
        }
    }

    public void doYaw(Location from, Location to) {
        double fromYaw = from.getYaw();
        double toYaw = to.getYaw();
        // attempt to increase rotation on land
        if (toYaw >= fromYaw - .025 && toYaw <= fromYaw + .025) {
            to.setYaw((float) (fromYaw * 2.8));
        }
        // turning while high forward speed
        else if (toYaw >= fromYaw - .7 && toYaw <= fromYaw + .7) {
            to.setYaw((float) (fromYaw * 5.3));
        }
        // turning at low forward speed (catch-all)
        else if (toYaw >= fromYaw - 3 && toYaw <= fromYaw + 3) {
            to.setYaw((float) (fromYaw * 3.3));
        }
        return;
    }

    private Material getBlockIdBeneath() {
        return boat.getWorld().getBlockAt(getX(), getY() - 1, getZ()).getType();
    }

    private Material getBlockIn() {
        return boat.getWorld().getBlockAt(getX(), getY(), getZ()).getType();
    }

    private Material getInHandType() {
        return getPlayer().getItemInHand().getType();
    }

    private double getMotionX() {
        return boat.getVelocity().getX();
    }

    private double getMotionY() {
        return boat.getVelocity().getY();
    }

    private double getMotionZ() {
        return boat.getVelocity().getZ();
    }

    private Player getPlayer() {
        return (Player) boat.getPassenger();
    }

    private int getX() {
        return boat.getLocation().getBlockX();
    }

    private int getY() {
        return boat.getLocation().getBlockY();
    }

    private int getZ() {
        return boat.getLocation().getBlockZ();
    }

    private boolean isGrounded() {
        EntityBoat be = ((CraftBoat) this.boat).getHandle();
        return be.onGround;
    }

    public boolean isMoving() {
        return getMotionX() != 0D || getMotionY() != 0D || getMotionZ() != 0D;
    }

    public void movementHandler() {
        if (throttle != 1) {
            speedUpBoat(throttle, boat.getVelocity());
        }
        EntityPlayer passenger = (EntityPlayer) ((CraftBoat) boat).getHandle().passenger;
        try {
            if (delay == 0 && JUMP_FIELD != null && JUMP_FIELD.getBoolean(passenger)) {
                doArmSwing();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        if (((Player) boat.getPassenger()).isSneaking()) {
            doRightClick();
        }

        Vector vel = boat.getVelocity();
        switch (mode) {
            case NORMAL:
                doNormal(vel);
                break;
            case PLANE:
                doFlying(vel);
                break;
            case SUBMARINE:
                doUnderwater(vel);
                break;
            case HOVERBOAT:
                doHover(vel);
                break;
            case GLIDER:
                doGlider(vel);
                break;
            default:
                break;
        }
        boat.setVelocity(vel);
        if (System.currentTimeMillis() >= delay) {
            delay = 0;
        }
    }

    public void setMode(Mode newMode) {
        mode = newMode;
        goingDown = false;
        goingUp = false;
        delay = 0;
    }

    private void setMotion(double motionX, double motionY, double motionZ) {
        boat.setVelocity(new Vector(motionX, motionY, motionZ));
    }

    private void setMotionY(double motionY) {
        motionY = RangeHandler.range(motionY, MAX_MOMENTUM, -MAX_MOMENTUM);
        boat.setVelocity(boat.getVelocity().add(new Vector(0, motionY, 0)));
    }

    private void speedUpBoat(double factor, Vector vel) {
        double curX = vel.getX();
        double curZ = vel.getZ();
        double newX = curX * factor;
        if (Math.abs(newX) > 0.4D) {
            if (newX < 0) {
                newX = -0.4D;
            } else {
                newX = 0.4D;
            }
            double newZ = 0D;
            if (curZ != 0D) {
                newZ = 0.4D / Math.abs(curX / curZ);
                if (curZ < 0) {
                    newZ *= -1;
                }
            }
            this.setMotion(newX, vel.getY(), newZ);
            return;
        }
        double newZ = curZ * factor;
        if (Math.abs(newZ) > 0.4D) {
            if (newZ < 0) {
                newZ = -0.4D;
            } else {
                newZ = 0.4D;
            }
            newX = 0D;
            if (curX != 0D) {
                newX = 0.4D / (curZ / curX);
                if (curX < 0) {
                    newX *= -1;
                }
            }
            this.setMotion(newX, vel.getY(), newZ);
            return;
        }
        this.setMotion(newX, vel.getY(), newZ);
    }

    class DropTNT implements Runnable {
        private final Item i;

        public DropTNT(Item i) {
            this.i = i;
        }

        @Override
        public void run() {
            Block beneath = i.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (beneath == null || beneath.getType() == Material.AIR) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(SkyPirates.plugin, this, 20);
            } else {
                i.getWorld().createExplosion(i.getLocation(), 5);
                i.remove();
            }
        }
    }

    public enum Mode {
        DRILL,
        GLIDER,
        HOVERBOAT,
        NORMAL,
        PLANE,
        SUBMARINE;
    }

    private static final double COMPENSATION = 0.0379999999999999999999999999999999999999999999D;
    private static final double DOWNWARD_DRIFT = -0.037999998673796664D;
    private static final Set<Material> helmets = EnumSet.of(Material.PUMPKIN, Material.DIAMOND_HELMET,
            Material.CHAINMAIL_HELMET, Material.GOLD_HELMET, Material.LEATHER_HELMET, Material.IRON_HELMET);
    private static Field JUMP_FIELD;
    private static final double MAX_BUOYANCY = 0.1D;
    private static final double MAX_MOMENTUM = 10D;

    static {
        try {
            JUMP_FIELD = EntityLiving.class.getDeclaredField("aY");
            JUMP_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
