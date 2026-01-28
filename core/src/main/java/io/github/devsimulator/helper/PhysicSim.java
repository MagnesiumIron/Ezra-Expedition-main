package io.github.devsimulator.helper;

import io.github.devsimulator.elements.Element;
import io.github.devsimulator.elements.Dirt;

public class PhysicSim {
    private Element[][] matrix;
    private boolean[][] walls;
    private int width, height;

    public PhysicSim(int width, int height) {
        this.width = width;
        this.height = height;
        this.matrix = new Element[width][height];
        this.walls = new boolean[width][height];
    }

    public void update() {
        // 1. RESET PHASE: Clear the 'hasUpdated' flag for every element
        // If we skip this, elements move once and then freeze forever!
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[x][y] != null) {
                    matrix[x][y].hasUpdated = false;
                }
            }
        }

        // 2. UPDATE PHASE: Run physics
        // We iterate Bottom-Up (0 to height) to process floor elements first
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[x][y] != null) {
                    matrix[x][y].step(this);
                }
            }
        }
    }

    // --- Core Helpers ---

    // Used by Element.java to check if it can move to a specific spot
    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public void setElement(int x, int y, Element e) {
        if (isWithinBounds(x, y)) {
            matrix[x][y] = e;
            // Update the element's internal coordinates to match the grid
            if (e != null) {
                e.setX(x);
                e.setY(y);
            }
        }
    }

    // --- Legacy Helpers (kept for compatibility) ---

    public boolean assimilateElement(int x, int y) {
        if (isWithinBounds(x, y)) {
            if (matrix[x][y] != null) {
                matrix[x][y] = null;
                return true;
            }
        }
        return false;
    }

    public void fillArea(int startX, int startY, int w, int h) {
        for (int x = startX; x < startX + w; x++) {
            for (int y = startY; y < startY + h; y++) {
                if (isWithinBounds(x, y)) {
                    // Only fill if empty and not a wall
                    if (matrix[x][y] == null && !walls[x][y]) {
                        matrix[x][y] = new Dirt(x, y);
                    }
                }
            }
        }
    }

    public boolean isEmpty(int x, int y) {
        if (!isWithinBounds(x, y)) return false;
        // It's empty if there is no element AND no wall
        return matrix[x][y] == null && !walls[x][y];
    }

    public void moveElement(int oldX, int oldY, int newX, int newY) {
        if (isWithinBounds(newX, newY)) {
            matrix[newX][newY] = matrix[oldX][oldY];
            matrix[oldX][oldY] = null;
            if (matrix[newX][newY] != null) {
                matrix[newX][newY].setX(newX);
                matrix[newX][newY].setY(newY);
            }
        }
    }

    public Element getElement(int x, int y) {
        if (isWithinBounds(x, y)) {
            return matrix[x][y];
        }
        return null;
    }

    public void setWall(int x, int y, boolean isWall) {
        if (isWithinBounds(x, y)) {
            walls[x][y] = isWall;
        }
    }

    public boolean isWall(int x, int y) {
        if (isWithinBounds(x, y)) {
            return walls[x][y];
        }
        return true; // Treat boundaries as walls
    }
}
