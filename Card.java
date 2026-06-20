/** 플레이잉 카드 한 장 표현 클래스. 슈트: C=클럽, D=다이아, H=하트, S=스페이드 */
public class Card {
    private final String suit;
    private final String rank;

    public Card(String suit, String rank) { this.suit = suit; this.rank = rank; }

    public String  getSuit()  { return suit; }
    public String  getRank()  { return rank; }
    public boolean isAce()    { return "A".equals(rank); }

    /** A=11(기본), J/Q/K=10, 나머지는 숫자값 */
    public int getValue() {
        switch (rank) {
            case "A":  return 11;
            case "J": case "Q": case "K": return 10;
            default:   return Integer.parseInt(rank);
        }
    }

    /** 이미지 파일명 반환. 예: "JH.png", "10C.png" */
    public String getImageName() { return rank + suit + ".png"; }

    @Override
    public String toString() {
        String sym;
        switch (suit) {
            case "C": sym = "\u2663"; break;
            case "D": sym = "\u2666"; break;
            case "H": sym = "\u2665"; break;
            case "S": sym = "\u2660"; break;
            default:  sym = suit;
        }
        return rank + sym;
    }
}
