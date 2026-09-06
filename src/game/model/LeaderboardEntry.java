package game.model;

/** A leaderboard row returned by the score data-access layer. */
public record LeaderboardEntry(int rank, String name, int highScore) { }
