package com.maple.quantum_chromodynamic_charge.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.mapleutillib.utils.task.TickableSubscription;

import static com.maple.quantum_chromodynamic_charge.common.QCCLevelTask.TASKS;
import static com.maple.quantum_chromodynamic_charge.explosion.ExplosionSupport.*;

/**
 * 渐进式球形爆炸清除：
 * <ol>
 * <li><b>0～50 实心</b>：整球 {@code r² ≤ min(50,R)²}（≤5 万格/tick）；</li>
 * <li><b>厚球壳批</b>：8/16 层合成一条环带，并向内重叠 1 层重扫已破坏边缘。</li>
 * </ol>
 * Y 扫描钳制在世界高度内，避免越界空转；{@code updateLight=false} 可关闭过程中光照以减轻 TPS 压力。
 */
public final class SphereExplosion {

    public static final int INITIAL_SOLID_RADIUS = 25;

    private static final int PHASE_SOLID = 0;
    private static final int PHASE_SHELL = 1;
    private static final int PHASE_DONE = 2;

    private final ServerLevel level;
    private final int cx, cy, cz;
    private final int targetRadius;
    private final int solidRadius;
    private final long solidRadiusSq;
    private final boolean updateHeightmap;
    private final boolean updateLight;
    /** 相对中心的合法 y 范围（世界高度钳制） */
    private final int yRelMin;
    private final int yRelMax;
    private final long[] buffer = new long[MAX_BLOCKS_PER_TICK];
    private final TickableSubscription<?> subscription;

    private int phase;
    private int batchEndL;
    private long batchStartSq;
    private long batchEndSq;

    private int relZ;
    private int relX = Integer.MIN_VALUE;
    private int relY = Integer.MIN_VALUE;
    private int ySeg;

    private SphereExplosion(BlockPos center, ServerLevel level, int radius,
                            boolean updateHeightmap, boolean updateLight,
                            boolean spawnParticles, boolean affectEntities) {
        this.level = level;
        this.cx = center.getX();
        this.cy = center.getY();
        this.cz = center.getZ();
        this.targetRadius = Math.max(0, radius);
        this.solidRadius = Math.min(INITIAL_SOLID_RADIUS, this.targetRadius);
        this.solidRadiusSq = (long) this.solidRadius * this.solidRadius;
        this.updateHeightmap = updateHeightmap;
        this.updateLight = updateLight;
        this.yRelMin = level.getMinY() - this.cy;
        this.yRelMax = level.getMaxY() - this.cy;

        this.phase = PHASE_SOLID;
        this.relZ = this.solidRadius;

        playExplosionEffects(level, this.cx + 0.5, this.cy + 0.5, this.cz + 0.5, spawnParticles);

        if (affectEntities) {
            double r = this.targetRadius + 1.0;
            killLivingIn(level, new AABB(
                    this.cx - r, this.cy - r, this.cz - r,
                    this.cx + r + 1.0, this.cy + r + 1.0, this.cz + r + 1.0));
        }

        subscription = TASKS.enqueueTick(level, this::tick, 0, 0);
    }

    private void tick() {
        if (phase == PHASE_DONE) {
            subscription.unsubscribe();
            return;
        }

        int count = 0;
        if (phase == PHASE_SOLID) {
            count = fillSolid(0);
        }
        if (phase == PHASE_SHELL && count < MAX_BLOCKS_PER_TICK) {
            count = fillThickShellBatches(count);
        }

        if (count > 0) {
            ILevel.setAirBlocksPacked(level, buffer, count, updateHeightmap, updateLight);
        }

        if (phase == PHASE_DONE) {
            subscription.unsubscribe();
        }
    }

    // -------------------------------------------------------------------------
    // 阶段 1：实心
    // -------------------------------------------------------------------------

    private int fillSolid(int count) {
        int z = relZ;
        int xResume = relX;
        int yResume = relY;

        for (; z >= -solidRadius; z--) {
            long remZ = solidRadiusSq - (long) z * z;
            if (remZ < 0L) {
                xResume = Integer.MIN_VALUE;
                yResume = Integer.MIN_VALUE;
                continue;
            }
            int xMax = floorSqrt(remZ);
            int x0 = (xResume != Integer.MIN_VALUE) ? xResume : -xMax;
            xResume = Integer.MIN_VALUE;
            int y0First = yResume;
            yResume = Integer.MIN_VALUE;

            for (int x = x0; x <= xMax; x++) {
                long remX = remZ - (long) x * x;
                if (remX < 0L) continue;
                int yMax = floorSqrt(remX);

                int y0 = (x == x0 && y0First != Integer.MIN_VALUE) ? y0First : -yMax;
                // 钳制到世界高度对应的相对 y，避免越界空转
                int yStart = Math.max(y0, yRelMin);
                int yEnd = Math.min(yMax, yRelMax);
                y0First = Integer.MIN_VALUE;
                if (yStart > yEnd) {
                    continue;
                }

                for (int y = yStart; y <= yEnd; y++) {
                    buffer[count++] = BlockPos.asLong(cx + x, cy + y, cz + z);
                    if (count >= MAX_BLOCKS_PER_TICK) {
                        relZ = z;
                        relX = x;
                        relY = y + 1;
                        return count;
                    }
                }
            }
        }

        beginShellPhase();
        return count;
    }

    private void beginShellPhase() {
        relX = Integer.MIN_VALUE;
        relY = Integer.MIN_VALUE;
        ySeg = 0;
        if (solidRadius >= targetRadius) {
            phase = PHASE_DONE;
            return;
        }
        phase = PHASE_SHELL;
        openBatch(solidRadius + 1);
    }

    /**
     * 打开厚壳批：逻辑推进 [fromL, end]，几何向内重叠 1 层已破坏边缘。
     */
    private void openBatch(int fromL) {
        if (fromL > targetRadius) {
            phase = PHASE_DONE;
            return;
        }
        int step = sphereShellBatchSize(fromL);
        batchEndL = Math.min(fromL + step - 1, targetRadius);

        int geoStartL = Math.max(0, fromL - 1);
        if (geoStartL == 0) {
            batchStartSq = 0L;
        } else {
            long inner = geoStartL - 1L;
            batchStartSq = inner * inner + 1L;
        }
        batchEndSq = (long) batchEndL * (long) batchEndL;

        relZ = batchEndL;
        relX = Integer.MIN_VALUE;
        relY = Integer.MIN_VALUE;
        ySeg = 0;
    }

    // -------------------------------------------------------------------------
    // 阶段 2：厚壳批
    // -------------------------------------------------------------------------

    private int fillThickShellBatches(int count) {
        final int baseX = cx, baseY = cy, baseZ = cz;
        final int yLoBound = yRelMin;
        final int yHiBound = yRelMax;

        while (phase == PHASE_SHELL) {
            final long startSq = batchStartSq;
            final long endSq = batchEndSq;
            final int outerR = batchEndL;

            int z = relZ;
            int xResume = relX;
            int yResume = relY;
            int segResume = ySeg;

            for (; z >= -outerR; z--) {
                long zSq = (long) z * z;
                long maxR2 = endSq - zSq;
                if (maxR2 < 0L) {
                    xResume = Integer.MIN_VALUE;
                    yResume = Integer.MIN_VALUE;
                    segResume = 0;
                    continue;
                }
                long minR2 = startSq - zSq;
                if (minR2 < 0L) minR2 = 0L;
                if (minR2 > maxR2) {
                    xResume = Integer.MIN_VALUE;
                    yResume = Integer.MIN_VALUE;
                    segResume = 0;
                    continue;
                }

                int xMax = floorSqrt(maxR2);
                int x0 = (xResume != Integer.MIN_VALUE) ? xResume : -xMax;
                xResume = Integer.MIN_VALUE;
                int y0First = yResume;
                int segFirst = segResume;
                yResume = Integer.MIN_VALUE;
                segResume = 0;

                for (int x = x0; x <= xMax; x++) {
                    long xSq = (long) x * x;
                    long maxY2 = maxR2 - xSq;
                    if (maxY2 < 0L) continue;
                    long minY2 = minR2 - xSq;
                    if (minY2 > maxY2) continue;

                    int yMax = floorSqrt(maxY2);
                    int yMinAbs;
                    if (minY2 <= 0L) {
                        yMinAbs = 0;
                    } else {
                        yMinAbs = ceilSqrt(minY2);
                        if (yMinAbs > yMax) continue;
                    }

                    boolean resumeHere = (x == x0 && y0First != Integer.MIN_VALUE);
                    int startSeg = resumeHere ? segFirst : 0;

                    if (yMinAbs == 0) {
                        int y0 = resumeHere ? y0First : -yMax;
                        int yStart = Math.max(y0, yLoBound);
                        int yEnd = Math.min(yMax, yHiBound);
                        if (yStart > yEnd) continue;

                        for (int y = yStart; y <= yEnd; y++) {
                            buffer[count++] = BlockPos.asLong(baseX + x, baseY + y, baseZ + z);
                            if (count >= MAX_BLOCKS_PER_TICK) {
                                saveResume(z, x, y + 1, 0);
                                return count;
                            }
                        }
                    } else {
                        for (int seg = startSeg; seg <= 1; seg++) {
                            int yFrom;
                            int yTo;
                            if (seg == 0) {
                                yFrom = resumeHere ? y0First : -yMax;
                                yTo = -yMinAbs;
                            } else {
                                yFrom = (resumeHere && startSeg == 1) ? y0First : yMinAbs;
                                yTo = yMax;
                            }
                            resumeHere = false;

                            int yStart = Math.max(yFrom, yLoBound);
                            int yEnd = Math.min(yTo, yHiBound);
                            if (yStart > yEnd) continue;

                            for (int y = yStart; y <= yEnd; y++) {
                                buffer[count++] = BlockPos.asLong(baseX + x, baseY + y, baseZ + z);
                                if (count >= MAX_BLOCKS_PER_TICK) {
                                    saveResume(z, x, y + 1, seg);
                                    return count;
                                }
                            }
                        }
                    }
                }
            }

            int nextFrom = batchEndL + 1;
            if (nextFrom > targetRadius) {
                phase = PHASE_DONE;
                return count;
            }
            openBatch(nextFrom);
        }

        return count;
    }

    private void saveResume(int z, int x, int nextY, int seg) {
        relZ = z;
        relX = x;
        relY = nextY;
        ySeg = seg;
    }

    // -------------------------------------------------------------------------
    // 公共入口
    // -------------------------------------------------------------------------

    /**
     * @param updateLight 过程中是否更新光照；大爆炸可传 {@code false} 显著减轻 TPS 压力
     */
    public static void explosion(BlockPos center, Level level, int radius,
                                 boolean updateHeightmap, boolean updateLight,
                                 boolean spawnParticles, boolean affectEntities) {
        if (level instanceof ServerLevel serverLevel) {
            new SphereExplosion(center, serverLevel, radius, updateHeightmap, updateLight,
                    spawnParticles, affectEntities);
        }
    }
}
