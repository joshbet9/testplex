package au.com.testplex.arcadeports;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Draw My Thing's non-repeating word pool, timed letter hints and action bars. */
final class DrawMyThingManager implements ArcadeMatchController {
    @Override public ArcadePortGame game() { return ArcadePortGame.DRAW_MY_THING; }

    String chooseWord(SessionState session) {
        List<String> words = new ArrayList<>(DrawMyThingContent.WORDS);
        words.removeAll(session.drawUsedWords);
        if (words.isEmpty()) {
            session.drawUsedWords.clear();
            words.addAll(DrawMyThingContent.WORDS);
        }
        String word = words.get(ThreadLocalRandom.current().nextInt(words.size()));
        session.drawUsedWords.add(word);
        return word;
    }

    void updateHintUi(SessionState session, Player drawer, long now) {
        if (session == null || session.drawWord == null) return;
        int letters = (int) session.drawWord.chars().filter(ch -> ch != ' ').count();
        boolean timedHints = GameDebugRuntimeOverrides.booleanValue("timed-hints", true);
        if (!timedHints) session.drawRevealedLetters.clear();
        int reveal = !timedHints ? 0 : ArcadePortRules.drawMyThingHintRevealCount(
                now - session.drawRoundStartedAt, session.drawRoundDurationMillis, letters);
        int revealedBefore = session.drawRevealedLetters.size();
        while (session.drawRevealedLetters.size() < reveal) {
            List<Integer> hidden = new ArrayList<>();
            for (int i = 0; i < session.drawWord.length(); i++) {
                if (session.drawWord.charAt(i) != ' ' && !session.drawRevealedLetters.contains(i)) hidden.add(i);
            }
            if (hidden.isEmpty()) break;
            session.drawRevealedLetters.add(hidden.get(ThreadLocalRandom.current().nextInt(hidden.size())));
        }
        if (session.drawRevealedLetters.size() > revealedBefore) {
            for (Player guesser : session.players.values()) {
                if (guesser != null && guesser.isOnline() && !guesser.getUniqueId().equals(session.drawCurrentDrawer)
                        && GameDebugRuntimeOverrides.booleanValue("sound-feedback", true)) {
                    guesser.playSound(guesser.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1.5F, 2.0F);
                }
            }
        }
        long seconds = Math.max(0, (session.drawRoundEndsAt - now + 999) / 1000);
        String hint = hintMask(session.drawWord, session.drawRevealedLetters);
        if (drawer != null && drawer.isOnline()) {
            drawer.sendActionBar(Component.text("DRAW: " + session.drawWord + " | " + seconds + "s"));
        }
        for (Player player : session.players.values()) {
            if (player == null || !player.isOnline() || player.getUniqueId().equals(session.drawCurrentDrawer)) continue;
            if (session.drawCorrectGuessers.contains(player.getUniqueId())) {
                player.sendActionBar(Component.text("Correct! Waiting for next round"));
            } else {
                player.sendActionBar(Component.text(hint + " | " + seconds + "s | Guesses: "
                        + Math.max(0, ArcadePortRules.drawMyThingMaxGuesses()
                                - session.drawGuessAttempts.getOrDefault(player.getUniqueId(), 0))
                        + "/" + ArcadePortRules.drawMyThingMaxGuesses()));
            }
        }
    }

    static String hintMask(String word, Set<Integer> revealed) {
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            hint.append(ch == ' ' ? "  " : revealed.contains(i) ? ch + " " : "_ ");
        }
        return hint.toString();
    }
}
