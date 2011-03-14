package com.fullwall.SkyPirates;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World;
import org.bukkit.block.Block;

public class Directions {
	public enum CompassDirection {
		NO_DIRECTION(-1), NORTH(0), NORTH_EAST(1), EAST(2), SOUTH_EAST(3), SOUTH(
				4), SOUTH_WEST(5), WEST(6), NORTH_WEST(7);
		@SuppressWarnings("unused")
		private int id;
		private static Map<Integer, CompassDirection> map;

		private CompassDirection(int id) {
			this.id = id;
			add(id, this);
		}

		private static void add(int type, CompassDirection name) {
			if (map == null) {
				map = new HashMap<Integer, CompassDirection>();
			}

			map.put(type, name);
		}

		private static boolean isFacingNorth(double degrees, double leeway) {
			return 0 <= degrees && degrees < 45 + leeway
					|| 315 - leeway <= degrees && degrees <= 360;
		}

		private static boolean isFacingEast(double degrees, double leeway) {
			return 45 - leeway <= degrees && degrees < 135 + leeway;
		}

		private static boolean isFacingSouth(double degrees, double leeway) {
			return 135 - leeway <= degrees && degrees < 225 + leeway;
		}

		private static boolean isFacingWest(double degrees, double leeway) {
			return 225 - leeway <= degrees && degrees < 315 + leeway;
		}

		public static CompassDirection getDirectionFromMinecartRotation(
				double degrees) {

			while (degrees < 0D) {
				degrees += 360D;
			}
			while (degrees > 360D) {
				degrees -= 360D;
			}

			CompassDirection direction = getDirectionFromRotation(degrees);

			double leeway = 15;
			if (direction.equals(CompassDirection.NORTH)
					|| direction.equals(CompassDirection.SOUTH)) {
				if (isFacingEast(degrees, leeway)) {
					return CompassDirection.EAST;
				}
				if (isFacingWest(degrees, leeway)) {
					return CompassDirection.WEST;
				}
			} else if (direction.equals(CompassDirection.EAST)
					|| direction.equals(CompassDirection.WEST)) {
				if (isFacingNorth(degrees, leeway)) {
					return CompassDirection.NORTH;
				}
				if (isFacingSouth(degrees, leeway)) {
					return CompassDirection.SOUTH;
				}
			}

			return direction;
		}

		public static Block getBlockTypeAhead(World w,
				CompassDirection efacingDir, int x, int y, int z, int below) {
			if (efacingDir == CompassDirection.NORTH)
				return w.getBlockAt(x - 1, y - below, z);
			if (efacingDir == CompassDirection.EAST)
				return w.getBlockAt(x, y - below, z - 1);
			if (efacingDir == CompassDirection.SOUTH)
				return w.getBlockAt(x + 1, y - below, z);
			if (efacingDir == CompassDirection.WEST)
				return w.getBlockAt(x, y - below, z + 1);
			return null;
		}

		public static CompassDirection getDirectionFromRotation(double degrees) {

			while (degrees < 0D) {
				degrees += 360D;
			}
			while (degrees > 360D) {
				degrees -= 360D;
			}

			if (isFacingNorth(degrees, 0)) {
				return CompassDirection.NORTH;
			} else if (isFacingEast(degrees, 0)) {
				return CompassDirection.EAST;
			} else if (isFacingSouth(degrees, 0)) {
				return CompassDirection.SOUTH;
			} else if (isFacingWest(degrees, 0)) {
				return CompassDirection.WEST;
			}

			return CompassDirection.NO_DIRECTION;
		}
	}
}
