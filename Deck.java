import java.util.ArrayList;
import java.util.Collections;

/** 4덱 슈(총 208장). 50% 미만 시 재셔플 대상 */
public class Deck {
    private ArrayList<Card> cards;
    public static final int NUM_DECKS = 4;
    public static final int TOTAL     = 52 * NUM_DECKS;

    private static final String[] SUITS = {"C","D","H","S"};
    private static final String[] RANKS = {"A","2","3","4","5","6","7","8","9","10","J","Q","K"};

    public Deck() { initialize(); }

    /** 208장 전체 생성 및 셔플 */
    public void initialize() {
        cards = new ArrayList<>(TOTAL);
        for (int d = 0; d < NUM_DECKS; d++)
            for (String s : SUITS)
                for (String r : RANKS)
                    cards.add(new Card(s, r));
        Collections.shuffle(cards);
    }

    /** 맨 위 카드 한 장 딜 */
    public Card deal() {
        if (cards.isEmpty()) initialize();
        return cards.remove(cards.size() - 1);
    }

    /** 남은 카드 50% 미만 시 true 반환 */
    public boolean needsShuffle() { return cards.size() < TOTAL / 2; }

    public int size() { return cards.size(); }
}
