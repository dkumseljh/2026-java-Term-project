import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 이름, 점수, 일시, 승률, 승수, 총게임수로 구성된 기록 항목. 점수 내림차순 정렬을 위해 Comparable 구현 */
public class ScoreRecord implements Comparable<ScoreRecord> {
    private final String name;
    private final int    score;
    private final String dateTime;   // "2026.05.31 17:30" 형식
    private final double winRate;    // 0~100
    private final int    wins;       // 이어하기 시 승수 복원용
    private final int    totalGames; // 이어하기 시 총게임수 복원용

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /** 파일 로드 시 사용하는 전체 생성자 */
    public ScoreRecord(String name, int score, String dateTime, double winRate, int wins, int totalGames) {
        this.name = name; this.score = score;
        this.dateTime = dateTime; this.winRate = winRate;
        this.wins = wins; this.totalGames = totalGames;
    }

    /** 현재 시각을 자동 기록하는 편의 생성자 */
    public ScoreRecord(String name, int score, double winRate, int wins, int totalGames) {
        this(name, score, LocalDateTime.now().format(FMT), winRate, wins, totalGames);
    }

    public String getName()      { return name; }
    public int    getScore()     { return score; }
    public String getDateTime()  { return dateTime; }
    public double getWinRate()   { return winRate; }
    public int    getWins()      { return wins; }
    public int    getTotalGames(){ return totalGames; }

    @Override public int compareTo(ScoreRecord o) { return Integer.compare(o.score, this.score); }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof ScoreRecord)) return false;
        ScoreRecord o = (ScoreRecord) obj;
        return name.equals(o.name) && score == o.score;
    }

    @Override public int hashCode() { return name.hashCode() * 31 + score; }

    /** 파일 저장 형식: "이름|점수|일시|승률|승수|총게임수" */
    @Override public String toString() {
        return name + "|" + score + "|" + dateTime + "|"
             + String.format("%.1f", winRate) + "|" + wins + "|" + totalGames;
    }
}
