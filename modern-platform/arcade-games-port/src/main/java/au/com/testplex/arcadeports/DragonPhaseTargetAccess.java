package au.com.testplex.arcadeports;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;

/** Bridges Bukkit dragon phases to the native charge phase's moving position target. */
final class DragonPhaseTargetAccess {
    private static volatile Access access;
    private static volatile boolean accessFailureLogged;
    private static volatile boolean accessSuccessLogged;

    private DragonPhaseTargetAccess() { }

    static boolean setChargeTarget(EnderDragon dragon, Location target) {
        if (dragon == null || target == null || dragon.getWorld() != target.getWorld()
                || dragon.getPhase() != EnderDragon.Phase.CHARGE_PLAYER) return false;
        if (access == null && accessFailureLogged) return false;
        try {
            Access resolved = access;
            if (resolved == null) {
                synchronized (DragonPhaseTargetAccess.class) {
                    resolved = access;
                    if (resolved == null) access = resolved = resolve(dragon);
                }
            }
            Object dragonHandle = nativeHandle(dragon);
            Object phaseManager = resolved.getPhaseManager.invoke(dragonHandle);
            Object chargePhase = resolved.getCurrentPhase.invoke(phaseManager);
            Object vector = resolved.vectorConstructor.newInstance(target.getX(), target.getY(), target.getZ());
            resolved.setTarget.invoke(chargePhase, vector);
            if (!accessSuccessLogged) {
                accessSuccessLogged = true;
                Bukkit.getLogger().info("[Testplex] Native Ender Dragon charge-position targeting is active.");
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!accessFailureLogged) {
                accessFailureLogged = true;
                Bukkit.getLogger().severe("[Testplex] Could not set the Ender Dragon native charge target: "
                        + exception);
            }
            return false;
        }
    }

    static Object nativeHandle(Object bukkitEntity) throws ReflectiveOperationException {
        return bukkitEntity.getClass().getMethod("getHandle").invoke(bukkitEntity);
    }

    private static Access resolve(EnderDragon dragon) throws ReflectiveOperationException {
        Object dragonHandle = nativeHandle(dragon);
        Method getPhaseManager = dragonHandle.getClass().getMethod("getPhaseManager");
        Object phaseManager = getPhaseManager.invoke(dragonHandle);
        Method getCurrentPhase = phaseManager.getClass().getMethod("getCurrentPhase");
        Object chargePhase = getCurrentPhase.invoke(phaseManager);

        Method setTarget = null;
        Constructor<?> vectorConstructor = null;
        for (Method candidate : chargePhase.getClass().getMethods()) {
            if (!candidate.getName().equals("setTarget") || candidate.getParameterCount() != 1) continue;
            Class<?> vectorType = candidate.getParameterTypes()[0];
            try {
                vectorConstructor = vectorType.getConstructor(double.class, double.class, double.class);
                setTarget = candidate;
                break;
            } catch (NoSuchMethodException ignored) {
                // Other setTarget overloads may accept entities or unrelated values.
            }
        }
        if (setTarget == null || vectorConstructor == null) {
            throw new NoSuchMethodException("native charge phase has no vector position target setter");
        }
        return new Access(getPhaseManager, getCurrentPhase, setTarget, vectorConstructor);
    }

    private record Access(Method getPhaseManager, Method getCurrentPhase, Method setTarget,
            Constructor<?> vectorConstructor) { }
}
