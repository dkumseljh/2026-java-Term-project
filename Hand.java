import java.util.ArrayList;

/**
 * 플레이어 또는 딜러의 핸드 클래스.
 * splitHand=true이면 스플릿 핸드로, isBlackjack()은 항상 false 반환.
 */
public class Hand {
    private final ArrayList<Card> cards;
    private final boolean splitHand;

    public Hand()                  { this(false); }
    public Hand(boolean splitHand) { cards = new ArrayList<>(); this.splitHand = splitHand; }

    public void addCard(Card c)    { cards.add(c); }
    public Card removeCard(int i)  { return cards.remove(i); }
    public Card getCard(int i)     { return cards.get(i); }
    public ArrayList<Card> getCards() { return cards; }
    public int  size()             { return cards.size(); }
    public void clear()            { cards.clear(); }
    public boolean isSplitHand()   { return splitHand; }

    /** 하드 점수: 에이스를 1로 계산 */
    public int getHardScore() {
        int s = 0;
        for (Card c : cards) s += c.isAce() ? 1 : c.getValue();
        return s;
    }

    /** 최적 점수: 버스트 미발생 시 에이스를 11로 계산 */
    public int getScore() {
        int score = 0, aces = 0;
        for (Card c : cards) {
            if (c.isAce()) { aces++; score += 11; }
            else score += c.getValue();
        }
        while (score > 21 && aces > 0) { score -= 10; aces--; }
        return score;
    }

    /** 표시용 점수 문자열. 에이스 11 사용 가능 시 "하드/소프트" 형태. 예: "6/16" */
    public String getDisplayScore() {
        int hard = getHardScore();
        boolean hasAce = false;
        for (Card c : cards) if (c.isAce()) { hasAce = true; break; }
        if (hasAce) {
            int soft = hard + 10;
            if (soft <= 21) return hard + "/" + soft;
        }
        return String.valueOf(getScore());
    }

    /** 내추럴 블랙잭 여부. 초기 2장 합계 21, 스플릿 핸드 제외 */
    public boolean isBlackjack() {
        return !splitHand && cards.size() == 2 && getScore() == 21;
    }

    /** 버스트 여부. 점수 21 초과 시 true */
    public boolean isBust() { return getScore() > 21; }

    /** 스플릿 가능 여부. 2장이고 값이 동일한 경우 true */
    public boolean canSplit() {
        return cards.size() == 2 && cards.get(0).getValue() == cards.get(1).getValue();
    }
}
