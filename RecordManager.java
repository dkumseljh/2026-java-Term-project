import java.io.*;
import java.util.*;

/**
 * "blackjack_records.txt" 기반 기록 로드 및 저장 클래스.
 * 저장 형식: "이름|점수|일시|승률|승수|총게임수" (줄 당 한 건).
 * 구형 "이름 점수" 및 4열 형식도 자동 인식.
 */
public class RecordManager {
    private static final String FILE_NAME = "blackjack_records.txt";
    private final ArrayList<ScoreRecord> records = new ArrayList<>();

    public RecordManager() { load(); }

    private void load() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    if (line.contains("|")) {
                        // 신형 형식: 이름|점수|일시|승률[|승수|총게임수]
                        String[] p = line.split("\\|", -1);
                        if (p.length < 2) continue;
                        String name  = p[0].trim();
                        int    score = Integer.parseInt(p[1].trim());
                        String dt    = p.length > 2 ? p[2].trim() : "";
                        double wr    = p.length > 3 ? Double.parseDouble(p[3].trim()) : 0.0;
                        int    wins  = p.length > 4 ? Integer.parseInt(p[4].trim()) : 0;
                        int    total = p.length > 5 ? Integer.parseInt(p[5].trim()) : 0;
                        records.add(new ScoreRecord(name, score, dt, wr, wins, total));
                    } else {
                        // 구형 형식: 이름 점수
                        int lastSp = line.lastIndexOf(' ');
                        if (lastSp < 1) continue;
                        String name  = line.substring(0, lastSp).trim();
                        int    score = Integer.parseInt(line.substring(lastSp+1).trim());
                        records.add(new ScoreRecord(name, score, "", 0.0, 0, 0));
                    }
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) { e.printStackTrace(); }
        Collections.sort(records); // 점수 내림차순 정렬
    }

    /** 현재 기록 전체 파일 덮어쓰기 */
    public void save() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (ScoreRecord r : records) { bw.write(r.toString()); bw.newLine(); }
        } catch (IOException e) { e.printStackTrace(); }
    }

    /** binarySearch로 정렬 위치 삽입. 이름+점수 완전 중복 시 false 반환 */
    public boolean addRecord(String name, int score, double winRate, int wins, int totalGames) {
        ScoreRecord nr = new ScoreRecord(name, score, winRate, wins, totalGames);
        if (records.contains(nr)) return false;
        int idx = Collections.binarySearch(records, nr);
        if (idx < 0) idx = -(idx + 1);
        records.add(idx, nr);
        return true;
    }

    /** 이어하기 가능 기록 탐색. 이름+점수 일치 및 점수 0 초과 조건 */
    public ScoreRecord findContinuable(String name, int score) {
        for (ScoreRecord r : records)
            if (r.getName().equals(name) && r.getScore() == score && score > 0) return r;
        return null;
    }

    public void remove(ScoreRecord r) { records.remove(r); }

    public ArrayList<ScoreRecord> getRecords() { return records; }
}
