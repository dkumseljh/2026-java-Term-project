/** 세션 내 게임 통계 관리 클래스 */
public class GameStatistics {
    private int totalGames, wins, losses, ties, blackjacks;
    private int currentStreak, maxStreak, maxBalance;

    public void reset() {
        totalGames = wins = losses = ties = blackjacks = 0;
        currentStreak = maxStreak = maxBalance = 0;
    }

    /** 이어하기 시 이전 기록의 승수·총게임수 복원 */
    public void restoreFromSave(int savedWins, int savedTotal) {
        this.wins       = savedWins;
        this.totalGames = savedTotal;
        this.losses     = savedTotal - savedWins; // 무승부 무시하고 근사값으로 복원
        this.ties = 0; this.blackjacks = 0;
        this.currentStreak = 0; this.maxStreak = 0; this.maxBalance = 0;
    }

    public void recordWin(boolean isBlackjack) {
        totalGames++; wins++;
        if (isBlackjack) blackjacks++;
        currentStreak++;
        if (currentStreak > maxStreak) maxStreak = currentStreak;
    }

    public void recordLoss() { totalGames++; losses++; currentStreak = 0; }
    public void recordTie()  { totalGames++; ties++; }

    public void updateMaxBalance(int b) { if (b > maxBalance) maxBalance = b; }

    public int    getTotalGames()    { return totalGames; }
    public int    getWins()          { return wins; }
    public int    getLosses()        { return losses; }
    public int    getTies()          { return ties; }
    public int    getBlackjacks()    { return blackjacks; }
    public int    getMaxStreak()     { return maxStreak; }
    public int    getCurrentStreak() { return currentStreak; }
    public int    getMaxBalance()    { return maxBalance; }
    public double getWinRate() {
        if (totalGames == 0) return 0.0;
        return wins * 100.0 / totalGames;
    }
}
