package com.fullwall.SkyPirates;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import net.minecraft.server.EntityBoat;
import net.minecraft.server.EntityTNTPrimed;

import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftBoat;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@SuppressWarnings("unused")
public class BoatHandler {
	public final Boat boat;
	private Vector previousLocation;
	private Vector previousMotion;
	private boolean wasMovingLastTick;
	public Calendar cal;

	// mode - 0 is normal, 1 is plane, 2 is submarine, 3 is hoverboat, 4 is
	// glider
	private int mode = 0;
	private int entityID = 0;
	public long delay = 0;
	public boolean isAttacking = false;
	private boolean throttleChanged = false;

	// 1E308 - max possible floating point value;
	private final double maxMomentum = 10D;
	private double rotationMultipler = 0D;
	private final double flyingMult = 1.0D;

	private double throttle = 1D;
	public double fromYaw = 0D;
	public double toYaw = 0D;
	private int hoverHeight = 0;

	private boolean goingDown = false;
	private boolean goingUp = false;
	private boolean firstRun = true;

	private final double DOWNWARD_DRIFT = -0.037999998673796664D;
	private final double COMPENSATION = 0.0379999999999999999999999999999999999999999999D;
	private final double MAX_BUOYANCY = 0.1D;
	private int MAX_HOVER_HEIGHT = 1;

	public BoatHandler(Boat newBoat, int newMode, int ID) {
		boat = newBoat;
		mode = newMode;
		entityID = ID;
		cal = Calendar.getInstance();
		previousMotion = boat.getVelocity().clone();
		previousLocation = getLocation().toVector().clone();
		if (isMoving())
			wasMovingLastTick = true;
		SkyPirates.boats.put(boat.getEntityId(), this);
	}

	private double getYaw() {
		return boat.getLocation().getYaw();
	}

	public void setYaw(double fromYaw, double toYaw) {
		this.fromYaw = fromYaw;
		this.toYaw = toYaw;
		return;
	}

	private void setMotion(double motionX, double motionY, double motionZ) {
		Vector newVelocity = new Vector();
		newVelocity.setX(motionX);
		newVelocity.setY(motionY);
		newVelocity.setZ(motionZ);
		boat.setVelocity(newVelocity);
	}

	public void setMotionX(double motionX) {
		motionX = RangeHandler.range(motionX, maxMomentum, -maxMomentum);
		setMotion(motionX, getMotionY(), getMotionZ());
	}

	public void setMotionY(double motionY) {
		motionY = RangeHandler.range(motionY, maxMomentum, -maxMomentum);
		setMotion(getMotionX(), motionY, getMotionZ());
	}

	public void setMotionZ(double motionZ) {
		motionZ = RangeHandler.range(motionZ, maxMomentum, -maxMomentum);
		setMotion(getMotionX(), getMotionY(), motionZ);
	}

	public double getMotionX() {
		return boat.getVelocity().getX();
	}

	public double getMotionY() {
		return boat.getVelocity().getY();
	}

	public int getX() {
		return boat.getLocation().getBlockX();
	}

	public int getY() {
		return boat.getLocation().getBlockY();
	}

	public int getZ() {
		return boat.getLocation().getBlockZ();
	}

	public double getMotionZ() {
		return boat.getVelocity().getZ();
	}

	public boolean getMovingLastTick() {
		return wasMovingLastTick;
	}

	public Block getBlockBeneath() {
		return boat.getWorld().getBlockAt(getX(), getY() - 1, getZ());
	}

	public int getBlockIdBeneath() {
		return boat.getWorld().getBlockAt(getX(), getY() - 1, getZ())
				.getTypeId();
	}

	public ItemStack getItemInHand() {
		return getPlayer().getItemInHand();
	}

	public int getItemInHandID() {
		return getPlayer().getItemInHand().getTypeId();
	}

	private int getHelmetID() {
		return getPlayer().getInventory().getHelmet().getTypeId();
	}

	private Player getPlayer() {
		final Player p = (Player) boat.getPassenger();
		return p;
	}

	public int getEntityId() {
		return entityID;
	}

	private Location getLocation() {
		return boat.getLocation();
	}

	private Vector getMaxSpeedVector() {
		return new Vector(0.4D, boat.getVelocity().getY(), 0.4D);
	}

	public boolean isMoving() {
		return getMotionX() != 0D || getMotionY() != 0D || getMotionZ() != 0D;
	}

	private boolean isGrounded() {
		EntityBoat be = (EntityBoat) ((CraftBoat) this.boat).getHandle();
		return be.onGround;
	}

	public void stopBoat() {
		setMotion(0D, 0D, 0D);
	}

	public void setMode(int newMode) {
		mode = newMode;
	}

	public void updateCalendar() {
		Calendar current = Calendar.getInstance();
		if (cal.get(Calendar.SECOND) != current.get(Calendar.SECOND)) {
			cal = current;
		}
	}

	public void resetValues() {
		goingDown = false;
		goingUp = false;
		delay = 0;
		return;
	}

	private void setThrottle(double change) {
		throttle = change;
		return;
	}

	private void changeThrottle(double change) {
		throttle += change;
		if (throttle >= 2.5D)
			throttle = 2.5D;
		else if (throttle <= 0D)
			throttle = 0D;
		if (throttle != 1) {
			throttleChanged = true;
		} else
			throttleChanged = false;

		return;
	}

	public void doRealisticFriction() {
		if (getPlayer() == null) {
			setMotion(getMotionX() * 0.53774, getMotionY(),
					getMotionZ() * 0.53774);
		}
	}

	public void speedUpBoat(double factor, Vector vel) {
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

	public void movementHandler(Vector vel) {

		Player p = getPlayer();
		Vector newvel = boat.getVelocity();
		if (throttle != 1) {
			speedUpBoat(throttle, newvel);
		}
		if (mode == 0) {
			doNormal(vel);
		} else if (mode == 1) {
			doFlying(vel);
		} else if (mode == 2) {
			doUnderwater(vel);
		} else if (mode == 3) {
			doHover(vel);
		} else if (mode == 4) {
			doGlider(vel);
		}
		previousMotion = boat.getVelocity();
	}

	public void movementHandler(double MotionY) {
		setMotionY(MotionY);
		// movementHandler(boat.getVelocity());
	}

	public void doArmSwing() {
		int i;
		if (isAttacking == true)
			i = 0;
		else if (mode != 4
				&& getItemInHandID() == 264
				&& Permission.genericCheck(getPlayer(),
						"skypirates.items.diamond")) {
			changeThrottle(0.25);
			getPlayer().sendMessage(
					ChatColor.YELLOW + "The boat " + ChatColor.DARK_RED
							+ "speeds up." + ChatColor.YELLOW
							+ " Your speed is now " + throttle
							+ "x of its original.");
		} else {
			// movementHandler(0.5D);
			if ((mode == 0) && delay == 0) {
				if (getItemInHandID() == 263
						&& Permission.genericCheck(getPlayer(),
								"skypirates.items.coal")) {
					movementHandler(0.75D);
					delay = cal.getTimeInMillis() + 750;
				} else {
					movementHandler(0.5D);
					delay = cal.getTimeInMillis();
				}
			} else if (mode == 1) {
				if (getItemInHandID() == 263
						&& Permission.genericCheck(getPlayer(),
								"skypirates.items.coal")) {
					goingUp = true;
					movementHandler(0.5D);
				} else {
					goingUp = true;
					movementHandler(0.5D);
				}
			} else if (mode == 2) {
				goingUp = true;
				movementHandler(0.1D);
			} else if (mode == 4
					&& (getBlockIdBeneath() != 8 && getBlockIdBeneath() != 9)) {
				speedUpBoat(10, boat.getVelocity());
			}
		}
		isAttacking = false;
	}

	public void doRightClick() {
		if (getItemInHandID() == 264
				&& Permission.genericCheck(getPlayer(),
						"skypirates.items.diamond")) {
			changeThrottle(-0.25D);
			getPlayer().sendMessage(
					ChatColor.BLUE + "The boat slows. Your speed is now "
							+ throttle + "x of its original.");
		} else if (getItemInHandID() == 262
				&& Permission.genericCheck(getPlayer(),
						"skypirates.items.arrow")) {
			getPlayer().shootArrow();
		} else if (mode == 1
				&& getItemInHandID() == 46
				&& Permission
						.genericCheck(getPlayer(), "skypirates.planes.tnt")) {
			Item item = getPlayer().getWorld().dropItemNaturally(
					getPlayer().getLocation(), new ItemStack(Material.TNT, 1));
			Timer t = new Timer();
			t.schedule(new DropTNT(item), 1000);
		} else if (getItemInHandID() == 80
				&& Permission.genericCheck(getPlayer(),
						"skypirates.items.snowblock")) {
			stopBoat();
			setThrottle(1D);
			getPlayer()
					.sendMessage(
							ChatColor.DARK_RED
									+ "The boat stops with a sudden jolt. Your speed is now only 1x original.");
		} else if (mode == 1) {
			goingDown = true;
			movementHandler(-0.65D);
		} else if (mode == 2) {
			goingDown = true;
			movementHandler(-0.2D);
		} else if (mode == 5) {
			doDrill();
		}

	}

	private void doNormal(Vector vel) {

		CraftEntity ce = (CraftEntity) this.boat.getPassenger();

		Vector playerVelocity = ce.getVelocity().clone();
		double playerVelocityX = playerVelocity.getX();
		double playerVelocityZ = playerVelocity.getZ();

		if ((playerVelocityX != 0D || playerVelocityZ != 0D) && isGrounded()) {
			getLocation().setYaw((float) (getYaw() * 2.5));
			// setMotion(playerVelocityX * 3.2, vel.getY(), playerVelocityZ *
			// 3.2);
			speedUpBoat(10, boat.getVelocity());
		}

		double currentX = vel.getX();
		double currentZ = vel.getZ();
		boolean boostSteering = false;

		if ((playerVelocityX < 0 && currentX > 0)
				|| (playerVelocityX > 0 && currentX < 0)) {
			boostSteering = true;
		}
		if (!boostSteering && (playerVelocityZ < 0 && currentZ > 0)
				|| (playerVelocityZ > 0 && currentZ < 0)) {
			boostSteering = true;
		}
		if (boostSteering) {
			currentX = currentX / 1.2D + playerVelocityX;
			currentZ = currentZ / 1.2D + playerVelocityZ;
			this.setMotion(currentX, vel.getY(), currentZ);
		}
		if (cal.getTimeInMillis() >= delay + 3000)
			delay = 0;
	}

	private void doGlider(Vector vel) {
		CraftEntity ce = (CraftEntity) this.boat.getPassenger();
		if (getBlockBeneath().getType() == Material.AIR)
			vel.setY(-0.075D);
		if (vel.getY() < -0.075D)
			vel.setY(-0.075D);
		setMotion(vel.getX(), vel.getY(), vel.getZ());
	}

	private void doFlying(Vector vel) {

		if (goingUp == true) {
			// vel.setY(vel.getY() - 0.02);
			if (vel.getY() <= 0D) {
				goingUp = false;
				vel.setY(0D);
			}
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		}
		if (goingDown == true) {
			if (vel.getY() <= 0D) {
				vel.setY(vel.getY() + 0.25);
				if (vel.getY() >= 0D)
					goingDown = false;
			}
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		} else if (vel.getY() <= 0D) {
			// workaround for bukkit glitch - setting motion to 0 still results
			// in downward moving. Not perfect, but it's the best it's going to
			// get without
			// more manipulation, like -(vel.getY())/2.5) etc.
			if (boat.getVelocity().getY() <= 0
					&& boat.getVelocity().getY() >= DOWNWARD_DRIFT)
				vel.setY(COMPENSATION);
			else
				vel.setY(0D);
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		} else {
			// see above.
			if (boat.getVelocity().getY() <= 0
					&& boat.getVelocity().getY() >= DOWNWARD_DRIFT)
				vel.setY(COMPENSATION);
			else
				vel.setY(0D);
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		}
	}

	private void doUnderwater(Vector vel) {
		// apply 'gravity' - aims to just be gentle downward motion
		Player p = getPlayer();

		if (goingUp != true)
			vel.setY(vel.getY() - 0.03);
		else
			vel.setY(vel.getY() - 0.03);

		// cap y velocity to combat buoyancy
		if (vel.getY() > MAX_BUOYANCY) {
			vel.setY(MAX_BUOYANCY);
		}
		if (goingUp == false && vel.getY() > 0)
			vel.setY(-0.15D);

		// raise rotation to stop slow turning
		getLocation().setYaw((float) (getYaw() * 2));

		// stop players from drowning underwater
		if (p.getRemainingAir() != p.getMaximumAir()
				&& ((Permission.genericCheck(p, "skypirates.player.air")) || (SkyPirates.helmets
						.contains("" + getHelmetID()) && Permission
						.genericCheck(p, "skypirates.items.helmets")))) {
			p.setRemainingAir(p.getMaximumAir());
			p.setMaximumAir(p.getMaximumAir());
		}
		if (goingUp == true) {
			vel.setY(vel.getY() - 0.009);
			if (vel.getY() <= 0.025D) {
				goingUp = false;
				vel.setY(0D);
			}
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		}
		if (goingDown == true) {
			if (vel.getY() <= -0.6D) {
				vel.setY(-0.6D);
				if (vel.getY() >= 0D)
					goingDown = false;
			}
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		} else {
			setMotion(vel.getX(), vel.getY(), vel.getZ());
			return;
		}
	}

	private void doHover(Vector vel) {
		if (getItemInHandID() == 263
				&& Permission
						.genericCheck(getPlayer(), "skypirates.items.coal")) {
			MAX_HOVER_HEIGHT = 2;
		} else
			MAX_HOVER_HEIGHT = 1;

		int x = boat.getLocation().getBlockX();
		int y = boat.getLocation().getBlockY();
		int z = boat.getLocation().getBlockZ();

		boolean goDown = false;
		int blockY = 0;
		Block block = null;

		getLocation().setYaw((float) (getYaw() * 6));

		for (int i = 0; i != MAX_HOVER_HEIGHT + 64; i++) {
			block = boat.getWorld().getBlockAt(x, y - blockY, z);
			if (block.getType() == Material.AIR)
				blockY += 1;
			else if (block.getType() == Material.WATER)
				break;
			else
				break;
			if (i > MAX_HOVER_HEIGHT + 1)
				goDown = true;
		}
		// if (stop == false)
		hoverHeight = block.getY() + (MAX_HOVER_HEIGHT * 2);

		if (boat.getLocation().getY() < hoverHeight + 0.6) {
			setMotionY(0.35D);
			return;
		} else if (goDown == true
				&& boat.getLocation().getY() > hoverHeight + 0.6) {
			setMotionY(-.25D);
			return;
		} else {
			setMotionY(0D);
			return;
		}
	}

	private void doDrill() {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				for (int y = 4; y >= 1; y--) {
					Block block = boat.getWorld().getBlockAt(
							boat.getLocation().getBlockX() - x,
							boat.getLocation().getBlockY() - y,
							boat.getLocation().getBlockZ() - z);
					if (!block.getType().equals(Material.AIR)
							&& (block.getTypeId() != 7)
							&& (block.getTypeId() != 8)
							&& (block.getTypeId() != 9)
							&& (block.getTypeId() != 10)
							&& (block.getTypeId() != 11)) {
						Material mat = block.getType();
						block.setType(Material.AIR);
						boat.getWorld().dropItemNaturally(block.getLocation(),
								new ItemStack(mat, 1));
					}
				}
			}
		}
	}

	public void doYaw(Location from, Location to) {
		fromYaw = (double) from.getYaw();
		toYaw = (double) to.getYaw();
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

	class DropTNT extends TimerTask {
		private Item i;

		public DropTNT(Item i) {
			this.i = i;
		}

		public void run() {
			Location loc = i.getLocation();
			int x = loc.getBlockX();
			int y = loc.getBlockY();
			int z = loc.getBlockZ();
			if (i.getWorld().getBlockAt(x, y - 1, z).getType() == Material.AIR) {
				Timer t = new Timer();
				t.schedule(this, 1000);
			} else {
				Block b = i.getWorld().getBlockAt(i.getLocation());
				i.remove();
				b.setType(Material.TNT);
				CraftWorld world = (CraftWorld) b.getWorld();
				EntityTNTPrimed tnt = new EntityTNTPrimed(world.getHandle(),
						b.getX() + 0.5F, b.getY() + 0.5F, b.getZ() + 0.5F);
				world.getHandle().a(tnt);
				b.setType(Material.AIR);
			}
		}
	}
}
