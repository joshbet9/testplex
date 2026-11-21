package au.com.testplex.arcadeports;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.LivingEntity;

/** Bridges Bukkit's Mob target API to the separate target held by the dragon's native flight phase. */
final class DragonPhaseTargetAccess {
    private static volatile Access access;
    private static volatile boolean accessFailureLogged;
    private static volatile boolean accessSuccessLogged;

    private DragonPhaseTargetAccess() { }

    static boolean setStrafingTarget(EnderDragon dragon, LivingEntity target) {
        if (dragon == null || target == null || dragon.getWorld() != target.getWorld()
                || dragon.getPhase() != EnderDragon.Phase.STRAFING) return false;
        if (access == null && accessFailureLogged) return false;
        try {
            Access resolved = access;
            if (resolved == null) {
                synchronized (DragonPhaseTargetAccess.class) {
                    resolved = access;
                    if (resolved == null) access = resolved = resolve(dragon, target);
                }
            }
            Object dragonHandle = resolved.bukkitHandle.invoke(dragon);
            Object phaseManager = resolved.getPhaseManager.invoke(dragonHandle);
            Object strafePhase = resolved.getCurrentPhase.invoke(phaseManager);
            Object targetHandle = resolved.bukkitHandle.invoke(target);
            resolved.setTargetEntity.invoke(strafePhase, targetHandle);
            if (!accessSuccessLogged) {
                accessSuccessLogged = true;
                Bukkit.getLogger().info("[Testplex] Native Ender Dragon STRAFE_PLAYER target access is active.");
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!accessFailureLogged) {
                accessFailureLogged = true;
                Bukkit.getLogger().severe("[Testplex] Could not set the Ender Dragon native pursuit target; "
                        + "Bukkit's general Mob target does not drive the STRAFING phase: " + exception);
            }
            return false;
        }
    }

    private static Access resolve(EnderDragon dragon, LivingEntity target) throws ReflectiveOperationException {
        Method bukkitHandle = dragon.getClass().getMethod("getHandle");
        Object dragonHandle = bukkitHandle.invoke(dragon);
        Method getPhaseManager = dragonHandle.getClass().getMethod("getPhaseManager");
        Object phaseManager = getPhaseManager.invoke(dragonHandle);
        Method getCurrentPhase = phaseManager.getClass().getMethod("getCurrentPhase");
        Object strafePhase = getCurrentPhase.invoke(phaseManager);
        Object targetHandle = target.getClass().getMethod("getHandle").invoke(target);
        Method setTargetEntity = null;
        for (String methodName : new String[] {"setTargetEntity", "setTarget"}) {
            for (Method candidate : strafePhase.getClass().getMethods()) {
                if (candidate.getName().equals(methodName)
                        && candidate.getParameterCount() == 1
                        && candidate.getParameterTypes()[0].isInstance(targetHandle)) {
                    setTargetEntity = candidate;
                    break;
                }
            }
            if (setTargetEntity != null) break;
        }
        if (setTargetEntity == null) {
            throw new NoSuchMethodException("native dragon flight phase has no LivingEntity target setter");
        }
        return new Access(bukkitHandle, getPhaseManager, getCurrentPhase, setTargetEntity);
    }

    private record Access(Method bukkitHandle, Method getPhaseManager, Method getCurrentPhase,
            Method setTargetEntity) { }
}
