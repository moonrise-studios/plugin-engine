package games.negative.engine.paper.data;

import it.unimi.dsi.fastutil.Pair;
import org.bukkit.util.Vector;

/**
 * Represents an Axis-Aligned Bounding Box (AABB) defined by its minimum and maximum coordinates in 3D space.
 * This class provides methods to convert the AABB to different formats and to check if a point is inside the AABB.
 */
public record AABB(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
) {

    /**
     * Converts the AABB to a pair of vectors representing the minimum and maximum corners of the bounding box.
     *
     * @return a pair of vectors, where the first vector is the minimum corner and the second vector is the maximum corner
     */
    public Pair<Vector, Vector> toVectors() {
        return Pair.of(
                minVector(),
                maxVector()
        );
    }

    /**
     * Creates a vector representing the minimum corner of the AABB.
     *
     * @return a vector with the minimum x, y, and z coordinates
     */
    public Vector minVector() {
        return new Vector(minX, minY, minZ);
    }

    /**
     * Creates a vector representing the maximum corner of the AABB.
     *
     * @return a vector with the maximum x, y, and z coordinates
     */
    public Vector maxVector() {
        return new Vector(maxX, maxY, maxZ);
    }

    /**
     * Converts the AABB to an array of doubles containing the minimum and maximum coordinates.
     *
     * @return an array of doubles in the order: minX, minY, minZ, maxX, maxY, maxZ
     */
    public double[] toArrayPair() {
        return new double[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    /**
     * Converts the minimum coordinates of the AABB to an array of doubles.
     *
     * @return an array of doubles containing the minimum x, y, and z coordinates
     */
    public double[] minArray() {
        return new double[]{minX, minY, minZ};
    }

    /**
     * Converts the maximum coordinates of the AABB to an array of doubles.
     *
     * @return an array of doubles containing the maximum x, y, and z coordinates
     */
    public double[] maxArray() {
        return new double[]{maxX, maxY, maxZ};
    }

    /**
     * Checks if a point defined by its x, y, and z coordinates is inside the AABB.
     *
     * @param x the x coordinate of the point
     * @param y the y coordinate of the point
     * @param z the z coordinate of the point
     * @return true if the point is inside the AABB, false otherwise
     */
    public boolean isInside(double x, double y, double z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    /**
     * Checks if a point represented as a Vector is inside the AABB.
     *
     * @param point the vector representing the point to check
     * @return true if the point is inside the AABB, false otherwise
     */
    public boolean isInside(Vector point) {
        return isInside(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Creates an AABB from two vectors representing the minimum and maximum corners of the bounding box.
     *
     * @param min the vector representing the minimum corner of the AABB
     * @param max the vector representing the maximum corner of the AABB
     * @return a new AABB instance defined by the given minimum and maximum vectors
     */
    public static AABB fromVectors(Vector min, Vector max) {
        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ()
        );
    }

    /**
     * Creates an AABB from an array of doubles containing the minimum and maximum coordinates.
     *
     * @param array an array of doubles in the order: minX, minY, minZ, maxX, maxY, maxZ
     * @return a new AABB instance defined by the given array of coordinates
     * @throws IllegalArgumentException if the array does not contain exactly 6 elements
     */
    public static AABB fromArrayPair(double[] array) {
        if (array.length != 6) {
            throw new IllegalArgumentException("Array must have exactly 6 elements: [minX, minY, minZ, maxX, maxY, maxZ]");
        }
        return new AABB(
                array[0], array[1], array[2],
                array[3], array[4], array[5]
        );
    }

}
