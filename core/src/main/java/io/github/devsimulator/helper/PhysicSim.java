package io.github.devsimulator.helper;

import io.github.devsimulator.elements.Element;
import io.github.devsimulator.elements.ElementType;
import io.github.devsimulator.elements.Smoke;
import io.github.devsimulator.controllers.SandManager;
import java.util.HashMap;

public class PhysicSim {
    //GRID
    private Element[][] matrix;
    private boolean[][] walls;
    private int width, height;

    //ELEMENT ALCHEMY DICTIONARY VIA HASHMAP
    private final HashMap<String, ElementType> alchemyRecipes = new HashMap<>();

    // OPTIMIZATION FOR EVERY PIXEL FRAME
    private final int CHUNK_SIZE = 32;
    private int chunksX, chunksY;
    private boolean[][] activeChunks;      // Chunks active THIS frame IF TRUE
    private boolean[][] nextActiveChunks;  // Chunks active NEXT frame

    public PhysicSim(int width, int height) {
        this.width = width;
        this.height = height;
        this.matrix = new Element[width][height];
        this.walls = new boolean[width][height];

        //Alchemy recipes or chemical reaction (MUST ALPHABETIZE)
        alchemyRecipes.put("LAVA_WATER", ElementType.OBSIDIAN);
        alchemyRecipes.put("DIRT_WATER", ElementType.MUD);
        alchemyRecipes.put("LAVA_SAND", ElementType.GLASS);
        alchemyRecipes.put("SAND_WATER", ElementType.MUD);

        // Initialize Chunks
        // use integer division + 1 to ensure we cover the edges if not divisible
        this.chunksX = (width / CHUNK_SIZE) + 1;
        this.chunksY = (height / CHUNK_SIZE) + 1;
        this.activeChunks = new boolean[chunksX][chunksY];
        this.nextActiveChunks = new boolean[chunksX][chunksY];
    }

    public void update() {
        // 1. SWAP PHASE: Prepare for this frame
        // Move 'Next' flags to 'Current' and clear 'Next'
        for (int x = 0; x < chunksX; x++) {
            for (int y = 0; y < chunksY; y++) {
                activeChunks[x][y] = nextActiveChunks[x][y];
                nextActiveChunks[x][y] = false; //reset next frame buffer
            }
        }

        //clear hasUpdated flag to move again THIS frame
        for (int cy = 0; cy < chunksY; cy++) {
            for (int cx = 0; cx < chunksX; cx++) {
                if (activeChunks[cx][cy]) {
                    resetChunkFlags(cx, cy);
                }
            }
        }

        //iterate through chunks to update the pixel physics
        for (int cy = 0; cy < chunksY; cy++) {
            for (int cx = 0; cx < chunksX; cx++) {
                if (!activeChunks[cx][cy]) continue;
                processChunk(cx, cy);
            }
        }
    }

    private void resetChunkFlags(int cx, int cy) {
        int startX = cx * CHUNK_SIZE;
        int startY = cy * CHUNK_SIZE;
        int endX = Math.min(startX + CHUNK_SIZE, width);
        int endY = Math.min(startY + CHUNK_SIZE, height);

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                if (matrix[x][y] != null) matrix[x][y].hasUpdated = false;
            }
        }
    }

    private void processChunk(int cx, int cy) {
        int startX = cx * CHUNK_SIZE;
        int startY = cy * CHUNK_SIZE;
        int endX = Math.min(startX + CHUNK_SIZE, width);
        int endY = Math.min(startY + CHUNK_SIZE, height);

        // Standard Random-X iteration for Sand
        boolean leftToRight = Math.random() > 0.5;
        int loopStart = leftToRight ? startX : endX - 1;
        int loopEnd = leftToRight ? endX : startX - 1;
        int step = leftToRight ? 1 : -1;

        for (int y = startY; y < endY; y++) {
            for (int x = loopStart; x != loopEnd; x += step) {
                if (matrix[x][y] != null) {
                    matrix[x][y].step(this);
                }
            }
        }
    }

    //
    public void wakeNeighbors(int x, int y) {
        int cx = x / CHUNK_SIZE;
        int cy = y / CHUNK_SIZE;

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int neighborX = cx + i;
                int neighborY = cy + j;

                if (neighborX >= 0 && neighborX < chunksX && neighborY >= 0 && neighborY < chunksY) {
                    nextActiveChunks[neighborX][neighborY] = true;
                }
            }
        }
    }

    public boolean processAlchemy(Element e1, Element e2) {
        if (e1 == null || e2 == null) return false;

        String n1 = e1.getClass().getSimpleName().toUpperCase();
        String n2 = e2.getClass().getSimpleName().toUpperCase();

        // Alphabetize the string so WATER_LAVA and LAVA_WATER both output "LAVA_WATER"
        String key = n1.compareTo(n2) < 0 ? n1 + "_" + n2 : n2 + "_" + n1;

        ElementType resultType = alchemyRecipes.get(key);
        if (resultType != null) {
            int e1x = e1.x, e1y = e1.y;
            int e2x = e2.x, e2y = e2.y;

            // clear the old elements first
            setElement(e1x, e1y, null);
            setElement(e2x, e2y, null);
            e1.freeToPool();
            e2.freeToPool();

            // spawn the reaction of the two elements
            setElement(e1x, e1y, resultType.create(e1x, e1y));

            // evaporate the second element into smoke/steam
            Smoke smoke = SandManager.smokePool.obtain();
            smoke.init(e2x, e2y);
            setElement(e2x, e2y, smoke);

            return true;
        }
        return false;
    }

    public ElementType getAlchemyRecipe(String key) {
        return alchemyRecipes.get(key);
    }

    public void setElement(int x, int y, Element e) {
        if (!isWithinBounds(x, y)) return;
        matrix[x][y] = e;
        if (e != null) {
            e.setX(x);
            e.setY(y);
        }
        // Changing a pixel must wake the chunk so the engine notices it
        wakeNeighbors(x, y);
    }

    public void moveElement(int oldX, int oldY, int newX, int newY) { //method for moving chunks (data) using array
        if (!isWithinBounds(newX, newY)) return;

        matrix[newX][newY] = matrix[oldX][oldY];
        matrix[oldX][oldY] = null;

        if (matrix[newX][newY] != null) {
            matrix[newX][newY].setX(newX);
            matrix[newX][newY].setY(newY);
        }
        wakeNeighbors(oldX, oldY);
        wakeNeighbors(newX, newY);
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Element getElement(int x, int y) {
        if (!isWithinBounds(x, y)) return null;
        return matrix[x][y];
    }

    public boolean isEmpty(int x, int y) {
        return isWithinBounds(x, y) && matrix[x][y] == null && !walls[x][y];
    }

    public void setWall(int x, int y, boolean isWall) {
        if (isWithinBounds(x, y)) walls[x][y] = isWall;
    }

    public boolean isWall(int x, int y) {
        if (!isWithinBounds(x, y)) return true;
        return walls[x][y];
    }

    public void fillArea(int startX, int startY, int w, int h, ElementType type) {
        for (int x = startX; x < startX + w; x++) {
            for (int y = startY; y < startY + h; y++) {
                if (isEmpty(x, y)) {
                    setElement(x, y, type.create(x, y));
                }
            }
        }
    }

    public void clearToPool() {//this method will prevent level load leaks
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix[x][y] != null && !(matrix[x][y] instanceof io.github.devsimulator.elements.EmptyCell)) {
                    matrix[x][y].freeToPool(); // Recycle everything!
                    matrix[x][y] = null;
                }
            }
        }
    }
}
