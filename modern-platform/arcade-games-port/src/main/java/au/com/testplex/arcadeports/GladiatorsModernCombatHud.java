package au.com.testplex.arcadeports;

/** Text shown on the Gladiators scoreboard for modern-combat weapon and shield state. */
final class GladiatorsModernCombatHud {
    private GladiatorsModernCombatHud() { }

    static View view(boolean blocking, float attackStrength, double attackSpeed) {
        double strength = Double.isFinite(attackStrength)
                ? Math.max(0.0D, Math.min(1.0D, attackStrength)) : 0.0D;
        double speed = Double.isFinite(attackSpeed) && attackSpeed > 0.0D ? attackSpeed : 4.0D;
        long tenthsRemaining = (long) Math.ceil(Math.max(0.0D, 1.0D - strength) * 10.0D / speed);
        String cooldown = tenthsRemaining <= 0 ? "Ready"
                : (tenthsRemaining / 10L) + "." + (tenthsRemaining % 10L) + "s";
        return new View(blocking ? "Raised" : "Lowered", cooldown);
    }

    record View(String shieldStatus, String cooldownStatus) { }
}
