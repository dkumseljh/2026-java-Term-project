import java.util.ArrayList;

/**
 * 블랙잭 게임 엔진 클래스.
 * GUI에서 이 클래스의 메서드를 호출하고 phase/results를 읽어 화면 갱신.
 */
public class BlackjackGame {

    public enum Phase  { MENU, BETTING, PLAYER_TURN, DEALER_TURN, ROUND_END, GAME_OVER }
    public enum Result { BLACKJACK_WIN, WIN, LOSS, PUSH, SURRENDER, BUST }

    private final Deck           deck        = new Deck();
    private final Hand           dealerHand  = new Hand();
    private ArrayList<Hand>      playerHands = new ArrayList<>();
    private ArrayList<Integer>   bets        = new ArrayList<>();
    private ArrayList<Result>    results     = new ArrayList<>();
    private final GameStatistics stats       = new GameStatistics();
    private final RecordManager  records     = new RecordManager();

    private Phase   phase         = Phase.MENU;
    private int     balance       = 0;
    private int     activeHandIdx = 0;     // 현재 플레이 중인 핸드 인덱스
    private boolean dealerHidden  = true;  // 딜러 홀 카드 은닉 여부
    private boolean specialAvail  = true;  // 더블다운·스플릿·서렌더 가능 여부
    private boolean hasSplit      = false;

    // ── 게임 시작 ──────────────────────────────────────────────────────

    /** 잔액 100원으로 새 게임 시작 및 통계 초기화 */
    /** 잔액 100원으로 새 게임 시작, 덱 완전 초기화 및 통계 리셋 */
    public void startNewGame() {
        balance = 100;
        stats.reset();
        phase = Phase.BETTING;
        deck.initialize();  // 새 게임 시작 시 항상 덱 전체 초기화
    }

    /** 저장 기록 이어하기. 이름+점수 불일치 시 false 반환 */
    public boolean continueGame(String name, int score) {
        ScoreRecord r = records.findContinuable(name, score);
        if (r == null) return false;
        records.remove(r);
        balance = score;
        // 이전 기록의 승수·총게임수 복원으로 승률 이어하기
        stats.restoreFromSave(r.getWins(), r.getTotalGames());
        phase = Phase.BETTING;
        reshuffleIfNeeded();
        return true;
    }

    /** 배팅 후 라운드 시작. 금액 유효하지 않으면 false 반환 */
    public boolean placeBet(int amount) {
        if (amount <= 0 || amount > balance) return false;
        balance -= amount;
        startRound(amount);
        return true;
    }

    private void startRound(int betAmount) {
        playerHands.clear(); bets.clear(); results.clear();
        dealerHand.clear();
        activeHandIdx = 0; dealerHidden = true;
        specialAvail = true; hasSplit = false;

        Hand hand = new Hand();
        playerHands.add(hand);
        bets.add(betAmount);

        // 딜 순서: 플레이어→딜러→플레이어→딜러
        hand.addCard(deck.deal());
        dealerHand.addCard(deck.deal());
        hand.addCard(deck.deal());
        dealerHand.addCard(deck.deal());

        // 플레이어 블랙잭이면 딜러 카드 즉시 공개 후 결과 계산
        if (hand.isBlackjack()) {
            specialAvail = false;
            dealerHidden = false;
            phase = Phase.DEALER_TURN;
            calculateResults();
        } else {
            phase = Phase.PLAYER_TURN;
        }
    }

    // ── 플레이어 행동 ──────────────────────────────────────────────────

    /** 힛: 카드 한 장 추가. 21 달성 또는 버스트 시 자동 진행 */
    public void hit() {
        specialAvail = false;
        getActiveHand().addCard(deck.deal());
        if (getActiveHand().getScore() >= 21) advanceHand();
    }

    /** 스테이: 현재 핸드 종료 */
    public void stay() {
        specialAvail = false;
        advanceHand();
    }

    /** 더블다운: 배팅 두 배, 카드 한 장, 자동 스테이. 불가 시 false 반환 */
    public boolean doubleDown() {
        if (!specialAvail || balance < bets.get(0)) return false;
        balance -= bets.get(0);
        bets.set(0, bets.get(0) * 2);
        specialAvail = false;
        getActiveHand().addCard(deck.deal());
        advanceHand();
        return true;
    }

    /** 스플릿: 동값 2장을 두 핸드로 분리. 불가 시 false 반환 */
    public boolean split() {
        if (!specialAvail || playerHands.size() > 1) return false;
        Hand hand = playerHands.get(0);
        if (!hand.canSplit() || balance < bets.get(0)) return false;

        int splitBet = bets.get(0);
        balance -= splitBet;
        specialAvail = false;
        hasSplit = true;

        Card c1 = hand.getCard(0);
        Card c2 = hand.removeCard(1);
        Hand h1 = new Hand(true); h1.addCard(c1);
        Hand h2 = new Hand(true); h2.addCard(c2);
        playerHands.set(0, h1); playerHands.add(h2);
        bets.set(0, splitBet);  bets.add(splitBet);

        h1.addCard(deck.deal());
        h2.addCard(deck.deal());

        activeHandIdx = 0;
        if (h1.getScore() == 21) {
            activeHandIdx = 1;
            if (h2.getScore() == 21) { runDealer(); return true; }
        }
        phase = Phase.PLAYER_TURN;
        return true;
    }

    /** 서렌더: 배팅 절반 반환 후 패배 처리. 딜러 에이스 공개 시 불가 */
    public boolean surrender() {
        if (!specialAvail) return false;
        if (dealerHand.getCard(0).isAce()) return false;
        balance += bets.get(0) / 2;
        specialAvail = false;
        results.add(Result.SURRENDER);
        stats.recordLoss();
        stats.updateMaxBalance(balance);
        phase = Phase.ROUND_END;
        reshuffleIfNeeded();
        return true;
    }

    // ── 내부 흐름 ──────────────────────────────────────────────────────

    /** 현재 핸드 종료 후 다음 핸드 또는 딜러 차례로 진행 */
    private void advanceHand() {
        activeHandIdx++;
        if (activeHandIdx < playerHands.size()) {
            if (playerHands.get(activeHandIdx).getScore() == 21) advanceHand();
            else phase = Phase.PLAYER_TURN;
            return;
        }
        boolean anyAlive = false;
        for (Hand h : playerHands) if (!h.isBust()) { anyAlive = true; break; }

        if (!anyAlive) {
            // 전부 버스트: 딜러 드로우 없이 패배 처리
            for (int i = 0; i < playerHands.size(); i++) results.add(Result.BUST);
            stats.recordLoss();
            stats.updateMaxBalance(balance);
            phase = Phase.ROUND_END;
            reshuffleIfNeeded();
        } else {
            runDealer();
        }
    }

    /** 딜러 17 이상이 될 때까지 카드 드로우 */
    private void runDealer() {
        dealerHidden = false;
        phase = Phase.DEALER_TURN;
        while (dealerHand.getScore() <= 16) dealerHand.addCard(deck.deal());
        calculateResults();
    }

    /** 각 핸드별 승패 계산 및 잔액 처리 */
    private void calculateResults() {
        int     ds    = dealerHand.getScore();
        boolean dBust = dealerHand.isBust();
        boolean dBJ   = dealerHand.isBlackjack();

        for (int i = 0; i < playerHands.size(); i++) {
            Hand h   = playerHands.get(i);
            int  bet = bets.get(i);
            Result res;
            if (h.isBust()) {
                res = Result.BUST;
            } else if (h.isBlackjack()) {
                if (dBJ) { res = Result.PUSH;          balance += bet; }
                else      { res = Result.BLACKJACK_WIN; balance += (int)(bet * 2.5); }
            } else {
                int ps = h.getScore();
                if      (dBust || ps > ds) { res = Result.WIN;  balance += bet * 2; }
                else if (ps == ds)         { res = Result.PUSH; balance += bet; }
                else                       { res = Result.LOSS; }
            }
            results.add(res);
        }
        // 첫 번째 핸드 기준으로 통계 갱신
        if (!results.isEmpty()) {
            switch (results.get(0)) {
                case BLACKJACK_WIN: stats.recordWin(true);  break;
                case WIN:           stats.recordWin(false); break;
                case BUST: case LOSS: stats.recordLoss();   break;
                case PUSH:          stats.recordTie();      break;
                default: break;
            }
        }
        stats.updateMaxBalance(balance);
        phase = Phase.ROUND_END;
        reshuffleIfNeeded();
    }

    /** 다음 라운드 이동 또는 잔액 0원 시 게임 오버 처리 */
    public void nextRound() {
        if (balance <= 0) phase = Phase.GAME_OVER;
        else { phase = Phase.BETTING; reshuffleIfNeeded(); }
    }

    /** 이름으로 현재 잔액 저장. 완전 중복 기록 존재 시 false 반환 */
    public boolean saveRecord(String name) {
        return records.addRecord(name, balance, stats.getWinRate(), stats.getWins(), stats.getTotalGames());
    }
    public void persistRecords() { records.save(); }

    private void reshuffleIfNeeded() { if (deck.needsShuffle()) deck.initialize(); }

    public Hand getActiveHand() {
        if (playerHands.isEmpty()) return new Hand();
        return playerHands.get(Math.min(activeHandIdx, playerHands.size() - 1));
    }

    public int getActiveHandBet() {
        if (bets.isEmpty() || activeHandIdx >= bets.size()) return 0;
        return bets.get(activeHandIdx);
    }

    public Phase              getPhase()       { return phase; }
    public int                getBalance()     { return balance; }
    public Hand               getDealerHand()  { return dealerHand; }
    public ArrayList<Hand>    getPlayerHands() { return playerHands; }
    public ArrayList<Integer> getBets()        { return bets; }
    public ArrayList<Result>  getResults()     { return results; }
    public int                getActiveIdx()   { return activeHandIdx; }
    public boolean            isDealerHidden() { return dealerHidden; }
    public boolean            isSpecialAvail() { return specialAvail; }
    public boolean            hasSplit()       { return hasSplit; }
    public GameStatistics     getStats()       { return stats; }
    public RecordManager      getRecords()     { return records; }
    public int                getDeckSize()    { return deck.size(); }
    public void               setPhase(Phase p){ this.phase = p; }
}
