import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * 블랙잭 메인 GUI 클래스. 검정 배경 / 흰색 인터페이스.
 * - 팬 카드 레이아웃: 카드가 겹쳐지며 오른쪽으로 이동할수록 위로 상승.
 * - 합계 라벨은 카드 영역 위 고정으로 카드에 가려지지 않음.
 * - 기록 화면: 각 행의 ▶ 버튼 클릭 시 이어하기/삭제 팝업 메뉴 표시.
 * 컴파일: javac -encoding EUC-KR *.java    실행: java BlackjackGUI
 */
public class BlackjackGUI extends JFrame {

    // ── 레이아웃 및 타이밍 상수                                                  
    private static final int CARD_W      = 100;
    private static final int CARD_H      = 145;
    private static final int FAN_OVERLAP = 34;  // pixels each card shifts right
    private static final int FAN_RISE    = 6;   // pixels each successive card rises
    private static final int DEAL_DELAY   = 320; // ms: 초기 딜 카드 간 딜레이
    private static final int DEALER_DELAY = 620; // ms: 딜러 드로우 카드 간 딜레이

    // ── 색상 팔레트                                                          
    private static final Color BG         = Color.BLACK;
    private static final Color FG         = Color.WHITE;
    private static final Color BORDER_CLR = Color.WHITE;
    private static final Color BTN_HOVER  = new Color(50, 50, 50);
    private static final Color RED_CLR    = new Color(220, 50, 50);

    // ── 폰트                                                             
    // "Malgun Gothic": 한국어 Windows 기본 폰트
    private static final Font FONT_HERO = new Font("Malgun Gothic", Font.PLAIN, 108);
    private static final Font FONT_LG   = new Font("Malgun Gothic", Font.PLAIN, 26);
    private static final Font FONT_MD   = new Font("Malgun Gothic", Font.PLAIN, 18);
    private static final Font FONT_SM   = new Font("Malgun Gothic", Font.PLAIN, 14);
    private static final Font FONT_XS   = new Font("Malgun Gothic", Font.PLAIN, 12);

    // ── CardLayout 패널 이름                                            
    private static final String SCREEN_MENU   = "MENU";
    private static final String SCREEN_GAME   = "GAME";
    private static final String SCREEN_RECORD = "RECORD";

    // ── 게임 엔진 및 이미지 캐시                                                    
    private final BlackjackGame         game     = new BlackjackGame();
    private final Map<String,ImageIcon> imgCache = new HashMap<>();

    // ── 최상위 화면 전환기                                         
    private JPanel     screens;
    private CardLayout screenLayout;

    // ── 게임 화면 컴포넌트                                            
    private JLabel     lblLeft1, lblLeft2;   // left side: deck count, balance
    private JLabel     lblRight1, lblRight2; // right side: win-rate, bet amount

    // 딜러 박스
    private JLabel     lblDealerSum;
    private FanPanel   dealerFan;

    // 플레이어 핸드 박스
    private JPanel     playerBoxesPanel;     // holds 1 or 2 hand boxes side by side
    private List<JLabel>    playerSumLabels = new ArrayList<>();
    private List<FanPanel>  playerFanPanels = new ArrayList<>();

    // 하단 액션 바 (CardLayout)
    private JPanel     actionBar;
    private CardLayout actionLayout;
    private static final String ACT_BLANK   = "BLANK";
    private static final String ACT_PLAY    = "PLAY";
    private static final String ACT_RESULT  = "RESULT";

    private JTextField fldBet;
    private JButton    btnHit, btnStay, btnDouble, btnSplit, btnSurrender;
    private JButton    btnNextRound; // kept as field to show/hide based on balance
    private JLabel     lblResult;

    // ── 애니메이션 상태                                                         
    private javax.swing.Timer animTimer;
    private int  snapDealerCount;
    private boolean snapDealerHidden;
    private int  snapActiveIdx;
    private List<Integer> snapPlayerCounts = new ArrayList<>();

    // ── 진입점                                                       
    public static void main(String[] args) {
        SwingUtilities.invokeLater(BlackjackGUI::new);
    }

    // ── 생성자                                                       
    public BlackjackGUI() {
        super("Black Jack");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { doExit(); }
        });
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        screenLayout = new CardLayout();
        screens      = new JPanel(screenLayout);
        screens.setBackground(BG);
        screens.add(buildMenuScreen(),   SCREEN_MENU);
        screens.add(buildGameScreen(),   SCREEN_GAME);
        screens.add(buildRecordScreen(), SCREEN_RECORD);
        add(screens, BorderLayout.CENTER);

        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    //                                                                     
    // ── 메인 메뉴 화면
    //                                                                     

    private JPanel buildMenuScreen() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(BG);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.insets = new Insets(0, 0, 40, 0);

        // "Black Jack" 제목 라벨
        JLabel title = new JLabel("Black Jack");
        title.setFont(FONT_HERO); title.setForeground(FG);
        g.gridy = 0; g.insets = new Insets(0, 0, 80, 0);
        p.add(title, g);

        // "새 게임" = "새 게임"
        JButton btnNew  = menuBtn("새 게임",    e -> doNewGame());
        // "기록/통계 확인" = "기록/통계 확인"
        JButton btnRec  = menuBtn("기록/통계 확인", e -> showRecordScreen());
        g.insets = new Insets(0, 0, 20, 0);
        g.gridy = 1; p.add(btnNew, g);
        g.gridy = 2; p.add(btnRec, g);
        return p;
    }

    private JButton menuBtn(String text, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_LG);
        btn.setForeground(FG); btn.setBackground(BG);
        btn.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 2));
        btn.setPreferredSize(new Dimension(420, 70));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(BTN_HOVER); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(BG); }
        });
        return btn;
    }

    //                                                                     
    // ── 게임 화면
    //

    private JPanel     midSwitch;     // 딜러 아래 영역: 배팅 UI 또는 플레이어 패
    private CardLayout midLayout;
    private static final String MID_BETTING = "MID_BETTING";
    private static final String MID_PLAYER  = "MID_PLAYER";

    private JPanel buildGameScreen() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        // 좌우 정보 패널
        // 좌측: 남은 카드 수 / 잔액
        JPanel leftPanel = new JPanel();
        leftPanel.setBackground(BG); leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(0, 20, 0, 0));
        lblLeft1 = sLabel("", FONT_SM, FG); lblLeft1.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblLeft2 = sLabel("", FONT_SM, FG); lblLeft2.setAlignmentX(Component.LEFT_ALIGNMENT);
        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(lblLeft1); leftPanel.add(Box.createVerticalStrut(10)); leftPanel.add(lblLeft2);
        leftPanel.add(Box.createVerticalGlue());

        // 우측: 승률 / 배팅금액
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(BG); rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(0, 0, 0, 20));
        lblRight1 = sLabel("", FONT_SM, FG); lblRight1.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblRight2 = sLabel("", FONT_SM, FG); lblRight2.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(lblRight1); rightPanel.add(Box.createVerticalStrut(10)); rightPanel.add(lblRight2);
        rightPanel.add(Box.createVerticalGlue());

        // 중앙 테이블
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(BG); center.setBorder(new EmptyBorder(20, 10, 10, 10));

        // 딜러 박스: 합계 라벨은 좌하단 고정, 팬 패널이 중앙을 채움
        JPanel dealerBox = new JPanel(new BorderLayout(0, 0));
        dealerBox.setBackground(BG);
        dealerBox.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 2));
        lblDealerSum = sLabel("합 : -", FONT_LG, FG);
        lblDealerSum.setBorder(new EmptyBorder(4, 10, 6, 10));
        dealerFan = new FanPanel();
        dealerBox.add(dealerFan,    BorderLayout.CENTER);
        dealerBox.add(lblDealerSum, BorderLayout.SOUTH);

        // 플레이어 박스 래퍼
        playerBoxesPanel = new JPanel();
        playerBoxesPanel.setBackground(BG);
        playerBoxesPanel.setLayout(new BoxLayout(playerBoxesPanel, BoxLayout.X_AXIS));

        // 중앙 스위처: 배팅 UI 또는 플레이어 패
        midLayout = new CardLayout();
        midSwitch = new JPanel(midLayout);
        midSwitch.setBackground(BG);
        midSwitch.add(buildBettingBar(), MID_BETTING);  // 배팅 시 이 영역에 표시
        midSwitch.add(playerBoxesPanel,  MID_PLAYER);   // 게임 중 플레이어 패 표시

        // 딜러 위, 플레이어/배팅 아래 (동일 높이 GridLayout)
        JPanel cardArea = new JPanel(new GridLayout(2, 1, 0, 10));
        cardArea.setBackground(BG);
        cardArea.add(dealerBox);
        cardArea.add(midSwitch);
        center.add(cardArea, BorderLayout.CENTER);

        // 액션 바: 배팅 시엔 빈 패널(BLANK) 표시, 플레이/결과 전환
        // 항상 표시 유지 → 배팅/플레이 간 딜러 박스 크기 일치
        actionLayout = new CardLayout();
        actionBar    = new JPanel(actionLayout);
        actionBar.setBackground(BG);
        actionBar.setPreferredSize(new Dimension(0, 120));
        JPanel blankPanel = new JPanel(); blankPanel.setBackground(BG);
        actionBar.add(blankPanel,        ACT_BLANK);
        actionBar.add(buildPlayBar(),    ACT_PLAY);
        actionBar.add(buildResultBar(),  ACT_RESULT);

        root.add(leftPanel,  BorderLayout.WEST);
        root.add(center,     BorderLayout.CENTER);
        root.add(rightPanel, BorderLayout.EAST);
        root.add(actionBar,  BorderLayout.SOUTH);
        return root;
    }

    // ── 배팅 바: 잔액/배팅 2행 테이블 + 베팅하기 버튼 + 빠른 선택 버튼
    private JLabel lblBetBalance; // 테이블 내 잔액 표시 라벨

    private JPanel buildBettingBar() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(BG);
        outer.setBorder(new EmptyBorder(10, 0, 10, 0));

        // 중앙: 2행 테이블 + 베팅하기 버튼
        JPanel middle = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        middle.setBackground(BG);

        // 잔액/베팅 2행 테이블
        JPanel table = new JPanel(new GridLayout(2, 2, 0, 0));
        table.setBackground(BG);
        table.setBorder(BorderFactory.createLineBorder(FG, 2));

        // 잔액 행 (읽기 전용 라벨)
        JLabel keyBal = sLabel("잔액", FONT_MD, FG);
        keyBal.setHorizontalAlignment(SwingConstants.CENTER);
        keyBal.setOpaque(true); keyBal.setBackground(BG);
        keyBal.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, FG));
        keyBal.setPreferredSize(new Dimension(90, 46));

        lblBetBalance = sLabel("", FONT_MD, FG);
        lblBetBalance.setHorizontalAlignment(SwingConstants.CENTER);
        lblBetBalance.setOpaque(true); lblBetBalance.setBackground(BG);
        lblBetBalance.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, FG));
        lblBetBalance.setPreferredSize(new Dimension(260, 46));

        // 베팅 행 (입력 필드)
        JLabel keyBet = sLabel("베팅", FONT_MD, FG);
        keyBet.setHorizontalAlignment(SwingConstants.CENTER);
        keyBet.setOpaque(true); keyBet.setBackground(BG);
        keyBet.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, FG));
        keyBet.setPreferredSize(new Dimension(90, 46));

        fldBet = new JTextField();
        fldBet.setFont(FONT_MD); fldBet.setBackground(BG); fldBet.setForeground(FG);
        fldBet.setCaretColor(FG); fldBet.setBorder(new EmptyBorder(2, 8, 2, 8));
        fldBet.setHorizontalAlignment(JTextField.CENTER);
        fldBet.setPreferredSize(new Dimension(260, 46));
        fldBet.addActionListener(e -> doPlaceBet());

        table.add(keyBal); table.add(lblBetBalance);
        table.add(keyBet); table.add(fldBet);

        // 베팅하기 버튼 (테이블과 같은 높이)
        JButton btnBet = actionBtn("베팅하기", e -> doPlaceBet());
        btnBet.setPreferredSize(new Dimension(150, 94));

        middle.add(table); middle.add(btnBet);

        // 하단: 빠른 선택 버튼
        JPanel quickBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        quickBtns.setBackground(BG);
        quickBtns.add(actionBtn("25%",  e -> fldBet.setText(String.valueOf(Math.max(1, game.getBalance()/4)))));
        quickBtns.add(actionBtn("50%",  e -> fldBet.setText(String.valueOf(Math.max(1, game.getBalance()/2)))));
        quickBtns.add(actionBtn("전액", e -> fldBet.setText(String.valueOf(game.getBalance()))));

        outer.add(middle,    BorderLayout.CENTER);
        outer.add(quickBtns, BorderLayout.SOUTH);
        return outer;
    }

    // ── 플레이 버튼 바                                                          
    private JPanel buildPlayBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 20));
        p.setBackground(BG);
        // "힛" = "힛"  "스테이" = "스테이"  etc.
        btnHit       = actionBtn("힛",        e -> doHit());
        btnStay      = actionBtn("스테이",   e -> doStay());
        btnSplit     = actionBtn("스플릿",    e -> doSplit());
        btnDouble    = actionBtn("더블다운", e -> doDouble());
        btnSurrender = actionBtn("서렌더",   e -> doSurrender());
        p.add(btnHit); p.add(btnStay); p.add(btnSplit); p.add(btnDouble); p.add(btnSurrender);
        return p;
    }

    // ── 라운드 결과 바
    private JPanel buildResultBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        lblResult = sLabel("", FONT_MD, FG);
        lblResult.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 10));
        btns.setBackground(BG);
        // "다음 라운드" = "다음 라운드"
        btnNextRound = actionBtn("다음 라운드", e -> doNextRound());
        // "게임 종료" = "게임 종료"
        btns.add(btnNextRound);
        btns.add(actionBtn("게임 종료", e -> doQuitGame()));
        p.add(lblResult, BorderLayout.CENTER);
        p.add(btns,      BorderLayout.SOUTH);
        return p;
    }

    private JButton actionBtn(String text, ActionListener al) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_MD); btn.setForeground(FG); btn.setBackground(BG);
        btn.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 2));
        btn.setPreferredSize(new Dimension(140, 48));
        btn.setFocusPainted(false); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(al);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if(btn.isEnabled()) btn.setBackground(BTN_HOVER); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(BG); }
        });
        return btn;
    }

    //                                                                     
    // ── 기록 확인 화면
    //                                                                     

    private JPanel buildRecordScreen() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.setName("RECORD_ROOT"); // tag so we can identify when rebuilding
        return root;
    }

    private void showRecordScreen() {
        rebuildRecordScreen();
        screenLayout.show(screens, SCREEN_RECORD);
    }

    private void rebuildRecordScreen() {
        // 기록 화면 패널 탐색 후 재구성
        JPanel root = null;
        for (Component c : screens.getComponents()) {
            if (c instanceof JPanel && "RECORD_ROOT".equals(((JPanel)c).getName())) {
                root = (JPanel) c; break;
            }
        }
        if (root == null) return;
        root.removeAll();

        // 제목 라벨
        // "기록확인" = "기록확인"
        JLabel title = sLabel("기록확인", FONT_LG, FG);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(24, 0, 16, 0));
        root.add(title, BorderLayout.NORTH);

        // 기록 테이블
        List<ScoreRecord> recs = game.getRecords().getRecords();
        // Columns: (blank), "일시", "이름", "점수", "승률", (  button)
        // "일시"=  , "이름"=  , "점수"=  , "승률"=  
        String[] colNames = {"", "일시", "이름", "점수", "승률", ""};
        int ROWS = Math.max(recs.size(), 10);
        Object[][] data = new Object[ROWS][6];
        for (int i = 0; i < ROWS; i++) {
            if (i < recs.size()) {
                ScoreRecord r = recs.get(i);
                data[i][0] = i + 1;
                data[i][1] = r.getDateTime();
                data[i][2] = r.getName();
                data[i][3] = r.getScore();
                data[i][4] = String.format("%.0f%%", r.getWinRate());
                data[i][5] = "\u25b6"; //  
            } else {
                data[i][0] = ""; data[i][1] = ""; data[i][2] = "";
                data[i][3] = ""; data[i][4] = ""; data[i][5] = "";
            }
        }

        // 기록 테이블 전용 폰트 (17pt)
        Font FONT_REC = new Font("Malgun Gothic", Font.PLAIN, 17);

        JTable table = new JTable(data, colNames) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setBackground(BG); table.setForeground(FG);
        table.setGridColor(BORDER_CLR); table.setFont(FONT_REC); table.setRowHeight(56);
        table.setShowGrid(true); table.setIntercellSpacing(new Dimension(1, 1));
        table.getTableHeader().setBackground(BG); table.getTableHeader().setForeground(FG);
        table.getTableHeader().setFont(FONT_REC);
        table.getTableHeader().setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));
        table.setSelectionBackground(new Color(30, 30, 30)); table.setSelectionForeground(FG);

        // 모든 셀 중앙 정렬 렌더러 적용
        javax.swing.table.DefaultTableCellRenderer centerRenderer =
            new javax.swing.table.DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                    super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                    setHorizontalAlignment(JLabel.CENTER);
                    setBackground(sel ? new Color(30,30,30) : BG);
                    if (col == 5 && val != null && !val.toString().isEmpty()) {
                        setForeground(FG);
                        setFont(FONT_MD); // ▶ 버튼 열은 약간 큰 폰트 적용
                    } else {
                        setForeground(FG);
                        setFont(FONT_REC);
                    }
                    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
                    return this;
                }
            };
        for (int i = 0; i < table.getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);

        // 열 너비 설정
        int[] widths = {50, 220, 160, 110, 110, 60};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // ▶ 클릭 시 팝업 메뉴 표시
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col != 5) return;
                if (row < 0 || row >= recs.size()) return;
                ScoreRecord rec = recs.get(row);
                showRecordPopup(table, e.getX(), e.getY(), rec);
            }
        });

        // 스크롤 패널을 래퍼로 감싸 좌우 여백에서 테이블 잘림 방지
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG); scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setBackground(BG);
        scrollWrapper.setBorder(new EmptyBorder(0, 40, 0, 40));
        scrollWrapper.add(scroll, BorderLayout.CENTER);

        // 하단 버튼 패널
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
        bottom.setBackground(BG);
        // "뒤로" = "뒤로"
        JButton btnBack = menuBtn("뒤로", e -> {
            screenLayout.show(screens, SCREEN_MENU);
        });
        btnBack.setPreferredSize(new Dimension(200, 52));
        // "통계 초기화" = "통계 초기화"
        JButton btnStat = menuBtn("통계 보기", e -> showStatsDialog());
        btnStat.setPreferredSize(new Dimension(200, 52));
        bottom.add(btnBack); bottom.add(Box.createHorizontalStrut(20)); bottom.add(btnStat);

        root.add(scrollWrapper, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);
        root.revalidate(); root.repaint();
    }

    /** 기록 행의 ▶ 클릭 시 표시되는 이어하기/삭제 팝업 메뉴 */
    private void showRecordPopup(Component parent, int x, int y, ScoreRecord rec) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(BG);
        popup.setBorder(BorderFactory.createLineBorder(BORDER_CLR, 1));

        // "이어하기" = "이어하기"
        JMenuItem itemContinue = new JMenuItem("이어하기");
        itemContinue.setBackground(BG); itemContinue.setForeground(FG); itemContinue.setFont(FONT_MD);
        if (rec.getScore() <= 0) itemContinue.setEnabled(false);

        // "삭제" = "삭제"
        JMenuItem itemDelete = new JMenuItem("삭제");
        itemDelete.setBackground(BG); itemDelete.setForeground(RED_CLR); itemDelete.setFont(FONT_MD);

        itemContinue.addActionListener(e -> {
            boolean ok = game.continueGame(rec.getName(), rec.getScore());
            if (ok) {
                game.persistRecords();
                screenLayout.show(screens, SCREEN_GAME);
                updateGameUI();
            } else {
                // "기록을 찾을 수 없습니다."
                JOptionPane.showMessageDialog(this, "기록을 찾을 수 없습니다.", "", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        itemDelete.addActionListener(e -> {
            // "삭제하시게습니까?" = "삭제하시겠습니까?"
            int opt = JOptionPane.showConfirmDialog(this,
                rec.getName() + "  (" + rec.getScore() + "원)  삭제하시게습니까?",
                "삭제", JOptionPane.YES_NO_OPTION);
            if (opt == JOptionPane.YES_OPTION) {
                game.getRecords().remove(rec);
                game.persistRecords();
                rebuildRecordScreen();
            }
        });

        popup.add(itemContinue);
        popup.addSeparator();
        popup.add(itemDelete);
        popup.show(parent, x, y);
    }

    //                                                                     
    // ── 게임 UI 갱신
    //                                                                     

    private void updateGameUI() {
        BlackjackGame.Phase phase = game.getPhase();

        // 좌우 정보 라벨 갱신
        // "남은 카드" = "남은 카드"  "장" = "장"
        lblLeft1.setText("남은 카드 " + game.getDeckSize() + "장");
        // "잔액 " = "잔액 "  "원" = "원"
        lblLeft2.setText("잔액 " + game.getBalance() + "원");


        GameStatistics st = game.getStats();
        String wrText = "승률 " + String.format("%.0f%%", st.getWinRate())
            + " (" + st.getWins() + "승 " + st.getLosses() + "패)";
        lblRight1.setText(wrText);

        int totalBet = 0;
        for (int b : game.getBets()) totalBet += b;

        if (totalBet > 0) lblRight2.setText("배팅금액 " + totalBet + "원");
        else              lblRight2.setText("");

        // 카드 영역 재구성
        rebuildCardAreas();

        // 액션 바 전환
        switch (phase) {
            case MENU:
            case GAME_OVER:
                screenLayout.show(screens, SCREEN_MENU);
                return;
            case BETTING:
                lblDealerSum.setVisible(false);   // 배팅 시 합계 미표시
                fldBet.setText("");
                lblBetBalance.setText(game.getBalance() + "원");
                midLayout.show(midSwitch, MID_BETTING);
                actionLayout.show(actionBar, ACT_BLANK);   // 크기 유지용 빈 패널
                SwingUtilities.invokeLater(() -> fldBet.requestFocusInWindow());
                break;
            case PLAYER_TURN:
                lblDealerSum.setVisible(true);
                midLayout.show(midSwitch, MID_PLAYER);
                updatePlayButtons();
                actionLayout.show(actionBar, ACT_PLAY);
                break;
            case DEALER_TURN:
                lblDealerSum.setVisible(true);
                midLayout.show(midSwitch, MID_PLAYER);
                disablePlay();
                actionLayout.show(actionBar, ACT_PLAY);
                break;
            case ROUND_END:
                lblDealerSum.setVisible(true);
                midLayout.show(midSwitch, MID_PLAYER);
                buildResultLabel();
                actionLayout.show(actionBar, ACT_RESULT);
                break;
        }
        screens.revalidate(); screens.repaint();
    }

    /** 딜러 팬 패널 및 플레이어 핸드 박스 전체 재구성 */
    private void rebuildCardAreas() {
        // 배팅 단계(라운드 미진행): 카드 화면 전체 초기화
        if (game.getPhase() == BlackjackGame.Phase.BETTING) {
            dealerFan.setCards(new ArrayList<>(), false);
            lblDealerSum.setText("합 : -");
            playerBoxesPanel.removeAll();
            playerSumLabels.clear();
            playerFanPanels.clear();
            playerBoxesPanel.revalidate();
            playerBoxesPanel.repaint();
            return;
        }
        dealerFan.setCards(game.getDealerHand().getCards(), game.isDealerHidden());
        String dealerSumStr;
        if (game.isDealerHidden()) {
            String v = game.getDealerHand().size() > 0
                ? String.valueOf(game.getDealerHand().getCard(0).getValue()) : "?";
            dealerSumStr = "합 : " + v + " + ?";
        } else {
            dealerSumStr = "합 : " + game.getDealerHand().getDisplayScore();
            if (game.getDealerHand().isBust()) dealerSumStr += "  버스트";
        }
        lblDealerSum.setText(dealerSumStr);


        int dealerH = CARD_H + 60;
        dealerFan.setPreferredSize(new Dimension(10, dealerH));

        // 플레이어 핸드 박스 재구성
        playerBoxesPanel.removeAll();
        playerSumLabels.clear();
        playerFanPanels.clear();

        int n = game.getPlayerHands().size();
        for (int i = 0; i < n; i++) {
            boolean active = (i == game.getActiveIdx())
                          && game.getPhase() == BlackjackGame.Phase.PLAYER_TURN;
            JPanel box = buildPlayerBox(i, active || n == 1);
            playerBoxesPanel.add(box);
            if (n > 1 && i < n-1)
                playerBoxesPanel.add(Box.createHorizontalStrut(10));
        }
        playerBoxesPanel.revalidate();
        playerBoxesPanel.repaint();
    }

    private JPanel buildPlayerBox(int idx, boolean active) {
        Hand hand = game.getPlayerHands().get(idx);
        int  bet  = game.getBets().get(idx);

        JPanel box = new JPanel(new BorderLayout(0, 4));
        box.setBackground(BG);
        Color borderColor = active ? FG : new Color(80,80,80);
        box.setBorder(BorderFactory.createLineBorder(borderColor, active ? 2 : 1));

        // 합계 라벨: 카드 위 고정 표시 (배팅금액은 우측 패널에서 표시하므로 생략)
        String sumText = "합 : " + hand.getDisplayScore();
        if (game.hasSplit()) sumText = "핸드" + (idx+1) + "  " + sumText;
        if (hand.isBust())      sumText += "  버스트";
        if (hand.isBlackjack()) sumText += "  BJ";

        JLabel sumLbl = sLabel(sumText, FONT_LG, active ? FG : new Color(160,160,160));
        sumLbl.setBorder(new EmptyBorder(6,10,4,10));
        playerSumLabels.add(sumLbl);

        // 팬 패널 생성
        FanPanel fan = new FanPanel();
        fan.setCards(hand.getCards(), false);
        int fanH = CARD_H + 40;
        fan.setPreferredSize(new Dimension(10, fanH));
        playerFanPanels.add(fan);

        box.add(sumLbl, BorderLayout.NORTH);
        box.add(fan,    BorderLayout.CENTER);
        return box;
    }

    private void updatePlayButtons() {
        boolean sp    = game.isSpecialAvail();
        int     bal   = game.getBalance();
        int     bet   = game.getActiveHandBet();
        boolean noAce = !game.getDealerHand().getCards().isEmpty()
                     && !game.getDealerHand().getCard(0).isAce();
        btnHit.setEnabled(true);
        btnStay.setEnabled(true);
        btnDouble.setEnabled(sp && !game.hasSplit() && bal >= bet);
        btnSplit.setEnabled(sp && !game.hasSplit()
                         && !game.getPlayerHands().isEmpty()
                         && game.getPlayerHands().get(0).canSplit() && bal >= bet);
        btnSurrender.setEnabled(sp && !game.hasSplit() && noAce);
    }

    private void disablePlay() {
        btnHit.setEnabled(false); btnStay.setEnabled(false);
        btnDouble.setEnabled(false); btnSplit.setEnabled(false);
        btnSurrender.setEnabled(false);
    }

    private void buildResultLabel() {
        List<BlackjackGame.Result> results = game.getResults();
        List<Integer>              bets    = game.getBets();
        StringBuilder sb = new StringBuilder();
        int totalDelta = 0;
        for (int i = 0; i < results.size(); i++) {
            BlackjackGame.Result res = results.get(i);
            int bet = bets.get(i);
            int delta;
            switch (res) {
                case BLACKJACK_WIN: delta=(int)(bet*1.5); sb.append("블랙잭! "); break;
                case WIN:           delta=bet;            sb.append("승리 "); break;
                case PUSH:          delta=0;              sb.append("무승부 "); break;
                case LOSS:          delta=-bet;           sb.append("패배 "); break;
                case BUST:          delta=-bet;           sb.append("버스트 "); break;
                case SURRENDER:     delta=-(bet/2);       sb.append("서렌더 "); break;
                default:            delta=0;
            }
            totalDelta += delta;
            if (delta > 0)      sb.append("+").append(delta).append("원  ");
            else if (delta < 0) sb.append(delta).append("원  ");
            else                sb.append("\u00b10원  ");
        }
        sb.append("  \u2192  잔액: ").append(game.getBalance()).append("원");
        lblResult.setText(sb.toString());
        if      (totalDelta > 0) lblResult.setForeground(FG);
        else if (totalDelta < 0) lblResult.setForeground(new Color(160,160,160));
        else                     lblResult.setForeground(FG);

        // 잔액 0원 시 다음 라운드 버튼 숨김
        if (btnNextRound != null) btnNextRound.setVisible(game.getBalance() > 0);
    }

    //                                                                     
    // ── 팬 카드 패널
    //                                                                     

    /** 카드를 팬 형태로 표시. 뒤에 놓인 카드일수록 오른쪽으로 이동하고 위로 상승 */
    class FanPanel extends JPanel {
        private List<Card>    cards  = new ArrayList<>();
        private boolean       hideSecond = false;
        private List<ImageIcon> icons = new ArrayList<>();

        FanPanel() { setBackground(BG); setOpaque(true); }

        void setCards(List<Card> cards, boolean hideSecond) {
            this.cards      = cards;
            this.hideSecond = hideSecond;
            icons.clear();
            for (int i = 0; i < cards.size(); i++) {
                boolean hidden = hideSecond && i == 1;
                icons.add(getCardIcon(cards.get(i), hidden));
            }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (icons.isEmpty()) return;
            int n = icons.size();
            // 팬 펼침 시 전체 폭 계산
            int totalW = CARD_W + (n - 1) * FAN_OVERLAP;
            int startX = Math.max(8, (getWidth() - totalW) / 2);
            // 수직 중앙 정렬: 팬 전체 높이를 계산해 패널 중앙에 배치
            int totalH = CARD_H + (n - 1) * FAN_RISE;
            int topY   = (getHeight() - totalH) / 2;
            int startY = topY + (n - 1) * FAN_RISE; // 가장 아래 카드의 y좌표

            for (int i = 0; i < n; i++) {
                int x = startX + i * FAN_OVERLAP;
                int y = startY - i * FAN_RISE; // 뒤 카드일수록 위로 상승
                icons.get(i).paintIcon(this, g, x, y);
            }
        }

        @Override public Dimension getPreferredSize() {
            int n = Math.max(1, icons.size());
            int w = CARD_W + (n - 1) * FAN_OVERLAP + 20;
            return new Dimension(w, CARD_H + 20);
        }
    }

    //                                                                     
    // ── 애니메이션 시스템
    //                                                                     

    private void snapshotState() {
        snapDealerCount  = game.getDealerHand().size();
        snapDealerHidden = game.isDealerHidden();
        snapActiveIdx    = game.getActiveIdx();
        snapPlayerCounts.clear();
        for (Hand h : game.getPlayerHands()) snapPlayerCounts.add(h.size());
    }

    /** 초기 4장 딜 애니메이션 실행 후 done 콜백 호출 */
    private void animateInitialDeal(Runnable done) {
        if (game.getPlayerHands().isEmpty() || game.getDealerHand().size() < 2) { done.run(); return; }

        Hand ph  = game.getPlayerHands().get(0);
        if (ph.size() < 2) { done.run(); return; }

        // 애니메이션 시작 전 화면 초기화
        dealerFan.setCards(new ArrayList<>(), false);
        if (!playerFanPanels.isEmpty()) playerFanPanels.get(0).setCards(new ArrayList<>(), false);

        List<Card> pCards = new ArrayList<>(ph.getCards());
        List<Card> dCards = new ArrayList<>(game.getDealerHand().getCards());
        final boolean dHidden = game.isDealerHidden();

        // 딜 순서: 플레이어0, 딜러0, 플레이어1, 딜러1
        List<Runnable> steps = new ArrayList<>();

        steps.add(() -> {
            List<Card> cur = new ArrayList<>(); cur.add(pCards.get(0));
            if (!playerFanPanels.isEmpty()) {
                playerFanPanels.get(0).setCards(cur, false);
                refreshPlayerLabelPartial(0, cur);   // 1장 기준 합계
            }
        });
        steps.add(() -> {
            List<Card> cur = new ArrayList<>(); cur.add(dCards.get(0));
            dealerFan.setCards(cur, false);
            refreshDealerLabelPartial(cur, false);   // 딜러 1장만 공개
        });
        steps.add(() -> {
            List<Card> cur = new ArrayList<>(); cur.add(pCards.get(0)); cur.add(pCards.get(1));
            if (!playerFanPanels.isEmpty()) {
                playerFanPanels.get(0).setCards(cur, false);
                refreshPlayerLabelPartial(0, cur);   // 2장 기준 합계
            }
        });
        steps.add(() -> {
            // 딜러 두 번째 카드(숨김): 첫 카드 값 + ? 표시
            dealerFan.setCards(dCards, dHidden);
            List<Card> visible = new ArrayList<>(); visible.add(dCards.get(0));
            refreshDealerLabelPartial(visible, dHidden);
        });

        runAnimSteps(steps, done);
    }

    /** 힛/스테이/딜러 드로우 등 액션 후 변경된 카드 애니메이션 실행 */
    private void animateAction(Runnable done) {
        List<Runnable> steps = new ArrayList<>();

        // 현재 핸드에 추가된 플레이어 카드 애니메이션
        if (snapActiveIdx < game.getPlayerHands().size()) {
            Hand h   = game.getPlayerHands().get(snapActiveIdx);
            int prev = snapActiveIdx < snapPlayerCounts.size() ? snapPlayerCounts.get(snapActiveIdx) : 0;
            for (int i = prev; i < h.size(); i++) {
                final List<Card> slice = new ArrayList<>(h.getCards().subList(0, i+1));
                final int hi = snapActiveIdx;
                steps.add(() -> {
                    if (hi < playerFanPanels.size()) {
                        playerFanPanels.get(hi).setCards(slice, false);
                        refreshPlayerLabelPartial(hi, slice);  // 현재 보이는 카드만으로 점수 계산
                    }
                });
            }
        }

        // 딜러 홀 카드 공개 및 추가 드로우 카드 애니메이션
        boolean revealed = snapDealerHidden && !game.isDealerHidden();
        int dSize = game.getDealerHand().size();

        // 플레이어 스텝이 있으면 먼저 DEAL_DELAY로 실행, 딜러 스텝은 DEALER_DELAY로 별도 실행
        List<Runnable> dealerSteps = new ArrayList<>();
        if (revealed) {
            // 홀카드 공개: 2장 보이는 상태에서 partialScore (2장 합계만 표시)
            final List<Card> twoCards = new ArrayList<>(game.getDealerHand().getCards().subList(0, Math.min(snapDealerCount, dSize)));
            dealerSteps.add(() -> {
                dealerFan.setCards(twoCards, false);
                refreshDealerLabelPartial(twoCards, false);  // 2장 기준 합계만
            });
        }
        for (int i = snapDealerCount; i < dSize; i++) {
            final List<Card> slice = new ArrayList<>(game.getDealerHand().getCards().subList(0, i+1));
            dealerSteps.add(() -> {
                dealerFan.setCards(slice, false);
                refreshDealerLabelPartial(slice, false);   // 보이는 카드 기준 합계
            });
        }

        if (steps.isEmpty()) {
            // 플레이어 스텝 없음: 딜러 스텝만 DEALER_DELAY로 실행
            runAnimSteps(dealerSteps, DEALER_DELAY, done);
        } else {
            // 플레이어 스텝 먼저 → 완료 후 딜러 스텝
            runAnimSteps(steps, DEAL_DELAY, () -> runAnimSteps(dealerSteps, DEALER_DELAY, done));
        }
    }

    private void runAnimSteps(List<Runnable> steps, Runnable done) {
        runAnimSteps(steps, DEAL_DELAY, done);
    }

    private void runAnimSteps(List<Runnable> steps, int delayMs, Runnable done) {
        if (steps.isEmpty()) { done.run(); return; }
        disablePlay();
        int[] idx = {0};
        if (animTimer != null && animTimer.isRunning()) animTimer.stop();
        animTimer = new javax.swing.Timer(delayMs, null);
        animTimer.addActionListener(e -> {
            if (idx[0] < steps.size()) { steps.get(idx[0]++).run(); }
            else { animTimer.stop(); done.run(); }
        });
        animTimer.start();
    }

    /** 애니메이션 중 핸드별 합계 라벨 갱신 */
    private void refreshSumLabels() {
        for (int i = 0; i < playerSumLabels.size() && i < game.getPlayerHands().size(); i++) {
            Hand h   = game.getPlayerHands().get(i);
            String t = "합 : " + h.getDisplayScore();
            if (game.hasSplit()) t = "핸드" + (i+1) + "  " + t;
            if (h.isBust())      t += "  버스트";
            if (h.isBlackjack()) t += "  BJ";
            playerSumLabels.get(i).setText(t);
        }
    }

    private void refreshDealerLabel() {
        if (game.isDealerHidden()) {
            String v = game.getDealerHand().size() > 0
                ? String.valueOf(game.getDealerHand().getCard(0).getValue()) : "?";
            lblDealerSum.setText("합 : " + v + " + ?");
        } else {
            String t = "합 : " + game.getDealerHand().getDisplayScore();
            if (game.getDealerHand().isBust()) t += "  버스트";
            lblDealerSum.setText(t);
        }
    }

    /** 현재 보이는 카드 목록만으로 점수 문자열 계산 (애니메이션 중 부분 표시용) */
    private String partialScore(List<Card> visible) {
        if (visible.isEmpty()) return "-";
        int score = 0, aces = 0;
        for (Card c : visible) {
            if (c.isAce()) { aces++; score += 11; }
            else score += c.getValue();
        }
        while (score > 21 && aces > 0) { score -= 10; aces--; }
        // 에이스 있으면 "하드/소프트" 표기
        boolean hasAce = false;
        for (Card c : visible) if (c.isAce()) { hasAce = true; break; }
        if (hasAce) {
            int hard = 0;
            for (Card c : visible) hard += c.isAce() ? 1 : c.getValue();
            int soft = hard + 10;
            if (soft <= 21) return hard + "/" + soft;
        }
        return String.valueOf(score);
    }

    /** 딜러: 현재 보이는 카드 기준 라벨 갱신. hidden=true면 "X + ?" 표시 */
    private void refreshDealerLabelPartial(List<Card> visible, boolean hidden) {
        if (visible.isEmpty()) { lblDealerSum.setText("합 : -"); return; }
        if (hidden) {
            lblDealerSum.setText("합 : " + visible.get(0).getValue() + " + ?");
        } else {
            String t = "합 : " + partialScore(visible);
            // 버스트 표시는 모든 카드가 공개됐을 때만
            if (!hidden && visible.size() == game.getDealerHand().size()
                    && game.getDealerHand().isBust())
                t += "  버스트";
            lblDealerSum.setText(t);
        }
    }

    /** 플레이어: 현재 보이는 카드 기준 라벨 갱신 */
    private void refreshPlayerLabelPartial(int handIdx, List<Card> visible) {
        if (handIdx >= playerSumLabels.size()) return;
        String t = "합 : " + partialScore(visible);
        if (game.hasSplit()) t = "핸드" + (handIdx+1) + "  " + t;
        // 버스트/BJ는 모든 카드가 공개됐을 때만
        if (handIdx < game.getPlayerHands().size()) {
            Hand h = game.getPlayerHands().get(handIdx);
            if (visible.size() == h.size()) {
                if (h.isBust())      t += "  버스트";
                if (h.isBlackjack()) t += "  BJ";
            }
        }
        playerSumLabels.get(handIdx).setText(t);
    }

    //                                                                     
    // ── 액션 핸들러
    //                                                                     

    private void doNewGame() {
        // 이전 게임 카드 잔류 방지: 화면 전환 전 초기화
        dealerFan.setCards(new ArrayList<>(), false);
        playerBoxesPanel.removeAll();
        playerSumLabels.clear();
        playerFanPanels.clear();
        game.startNewGame();
        screenLayout.show(screens, SCREEN_GAME);
        updateGameUI();
    }

    private void doPlaceBet() {
        int amount;
        try { amount = Integer.parseInt(fldBet.getText().trim()); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "유효한 금액을 입력해주세요."); return;
        }
        // 배팅 가능 여부 먼저 확인
        if (amount <= 0 || amount > game.getBalance()) {
            JOptionPane.showMessageDialog(this,
                "유효하지 않은 배팅입니다. (1 ~ " + game.getBalance() + "원)");
            return;
        }
        // 배팅 UI → 게임 뷰로 즉시 전환 (딜 애니메이션 전)
        lblDealerSum.setVisible(true);
        midLayout.show(midSwitch, MID_PLAYER);
        disablePlay();
        actionLayout.show(actionBar, ACT_PLAY);

        // 베팅 실행 (phase → PLAYER_TURN)
        game.placeBet(amount);

        // 빈 팬 패널 구성 후 애니메이션
        rebuildCardAreas();
        dealerFan.setCards(new ArrayList<>(), false);
        lblDealerSum.setText("합 : -");                    // 카드 등장 전 초기화
        if (!playerFanPanels.isEmpty()) {
            playerFanPanels.get(0).setCards(new ArrayList<>(), false);
            if (!playerSumLabels.isEmpty())
                playerSumLabels.get(0).setText("합 : -");  // 카드 등장 전 초기화
        }

        animateInitialDeal(() -> updateGameUI());
    }

    private void doHit() {
        snapshotState();
        game.hit();
        animateAction(() -> updateGameUI());
    }

    private void doStay() {
        snapshotState();
        game.stay();
        animateAction(() -> updateGameUI());
    }

    private void doDouble() {
        snapshotState();
        if (!game.doubleDown()) { JOptionPane.showMessageDialog(this, "더블다운이 불가능합니다."); return; }
        animateAction(() -> updateGameUI());
    }

    private void doSplit() {
        if (!game.split()) { JOptionPane.showMessageDialog(this, "스플릿이 불가능합니다."); return; }
        rebuildCardAreas();
        // 각 핸드의 첫 번째 카드 표시 후 두 번째 카드 애니메이션
        for (int i = 0; i < 2 && i < playerFanPanels.size(); i++) {
            Hand h = game.getPlayerHands().get(i);
            if (h.size() >= 1) {
                List<Card> one = new ArrayList<>(); one.add(h.getCard(0));
                playerFanPanels.get(i).setCards(one, false);
            }
        }
        List<Runnable> steps = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            final int hi = i;
            steps.add(() -> {
                Hand h = game.getPlayerHands().get(hi);
                if (hi < playerFanPanels.size())
                    playerFanPanels.get(hi).setCards(new ArrayList<>(h.getCards()), false);
                refreshSumLabels();
            });
        }
        runAnimSteps(steps, () -> updateGameUI());
    }

    private void doSurrender() {
        if (!game.surrender()) { JOptionPane.showMessageDialog(this, "서렌더가 불가능합니다."); return; }
        updateGameUI();
    }

    private void doNextRound() { game.nextRound(); updateGameUI(); }

    private void doQuitGame() {
        askSaveRecord();
        game.setPhase(BlackjackGame.Phase.MENU);
        screenLayout.show(screens, SCREEN_MENU);
    }

    private void doExit() {
        int opt = JOptionPane.showConfirmDialog(this,
            "프로그램을 종료하시게습니까?",
            "종료", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) { game.persistRecords(); System.exit(0); }
    }

    private void askSaveRecord() {
        int opt = JOptionPane.showConfirmDialog(this,
            "잔액(" + game.getBalance() + "원)을 기록으로 저장하시게습니까?",
            "기록 저장", JOptionPane.YES_NO_OPTION);
        if (opt != JOptionPane.YES_OPTION) return;
        while (true) {
            String name = JOptionPane.showInputDialog(this, "이름:");
            if (name == null) return;
            name = name.trim();
            if (name.isEmpty()) continue;
            if (game.saveRecord(name)) { game.persistRecords(); return; }
            JOptionPane.showMessageDialog(this, "중복된 기록입니다. 다른 이름을 입력하세요.");
        }
    }

    //                                                                     
    // ── 통계 다이얼로그
    //                                                                     

    private void showStatsDialog() {
        GameStatistics st = game.getStats();
        JDialog dlg = new JDialog(this, "통계", true);
        dlg.setSize(380, 460); dlg.setLocationRelativeTo(this);
        dlg.getContentPane().setBackground(BG);
        dlg.setLayout(new BorderLayout());

        JPanel content = new JPanel(new GridLayout(0, 2, 12, 18));
        content.setBackground(BG); content.setBorder(new EmptyBorder(20, 36, 20, 36));
        String[][] rows = {
            {"총 게임 수",            String.valueOf(st.getTotalGames())},
            {"승리",                   String.valueOf(st.getWins())},
            {"패배",                   String.valueOf(st.getLosses())},
            {"무승부",             String.valueOf(st.getTies())},
            {"블랙잭 횟수", String.valueOf(st.getBlackjacks())},
            {"승률",                   String.format("%.1f%%", st.getWinRate())},
            {"최고 잔액",       st.getMaxBalance() + "원"},
            {"최대 연승",       String.valueOf(st.getMaxStreak())},
        };
        for (String[] r : rows) {
            JLabel lKey = sLabel(r[0], FONT_SM, new Color(180,180,180));
            JLabel lVal = sLabel(r[1], FONT_SM, FG);
            lVal.setHorizontalAlignment(SwingConstants.RIGHT);
            content.add(lKey);
            content.add(lVal);
        }

        JLabel title = sLabel("통계", FONT_LG, FG);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(18, 0, 6, 0));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 14));
        btns.setBackground(BG);
        // "통계 초기화" = "통계 초기화" (      )
        JButton btnReset = menuBtn("통계 초기화", e -> {
            game.getStats().reset();
            dlg.dispose();
        });
        btnReset.setPreferredSize(new Dimension(190, 50));
        // "닫기" = "닫기" (  )
        JButton btnClose = menuBtn("닫기", e -> dlg.dispose());
        btnClose.setPreferredSize(new Dimension(120, 50));
        btns.add(btnReset); btns.add(btnClose);

        dlg.add(title,   BorderLayout.NORTH);
        dlg.add(content, BorderLayout.CENTER);
        dlg.add(btns,    BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    //                                                                     
    // ── 카드 이미지 생성
    //                                                                     

    private ImageIcon getCardIcon(Card card, boolean hidden) {
        String key = hidden ? "back" : card.getImageName();
        if (!imgCache.containsKey(key)) {
            File f = new File("images/" + (hidden ? "back.png" : card.getImageName()));
            if (f.exists()) {
                Image scaled = new ImageIcon(f.getAbsolutePath()).getImage()
                               .getScaledInstance(CARD_W, CARD_H, Image.SCALE_SMOOTH);
                imgCache.put(key, new ImageIcon(scaled));
            } else {
                imgCache.put(key, new ImageIcon(hidden ? drawBack() : drawFront(card)));
            }
        }
        return imgCache.get(key);
    }

    private BufferedImage drawFront(Card card) {
        BufferedImage bi = new BufferedImage(CARD_W, CARD_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(FG); g.fillRoundRect(0, 0, CARD_W-1, CARD_H-1, 12, 12);
        g.setColor(new Color(220,220,220)); g.drawRoundRect(0,0,CARD_W-2,CARD_H-2,12,12);

        boolean red = "H".equals(card.getSuit()) || "D".equals(card.getSuit());
        Color clr = red ? new Color(190,20,20) : BG;
        String sym = suitSym(card.getSuit());
        g.setColor(clr);

        // 좌상단: 랭크 및 슈트 기호
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        g.drawString(card.getRank(), 6, 18);
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        g.drawString(sym, 6, 34);

        // 중앙: 큰 슈트 기호
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 42));
        FontMetrics fm = g.getFontMetrics();
        int sw = fm.stringWidth(sym);
        g.drawString(sym, (CARD_W - sw)/2, CARD_H/2 + 15);

        // 우하단: 반전 배치
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        fm = g.getFontMetrics();
        g.drawString(card.getRank(), CARD_W - 6 - fm.stringWidth(card.getRank()), CARD_H - 18);
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        fm = g.getFontMetrics();
        g.drawString(sym, CARD_W - 6 - fm.stringWidth(sym), CARD_H - 5);

        g.dispose(); return bi;
    }

    private BufferedImage drawBack() {
        BufferedImage bi = new BufferedImage(CARD_W, CARD_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 검정 바탕 + 흰 테두리 카드 뒷면
        g.setColor(BG); g.fillRoundRect(0,0,CARD_W-1,CARD_H-1,12,12);
        g.setColor(FG); g.setStroke(new BasicStroke(2));
        g.drawRoundRect(2,2,CARD_W-5,CARD_H-5,10,10);
        g.drawRoundRect(6,6,CARD_W-13,CARD_H-13,7,7);
        // 중앙 장식 기호
        g.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        FontMetrics fm = g.getFontMetrics();
        String s = "\u2666\u2663";
        g.drawString(s, (CARD_W - fm.stringWidth(s))/2, CARD_H/2 + 8);
        g.dispose(); return bi;
    }

    private String suitSym(String suit) {
        if ("C".equals(suit)) return "\u2663";
        if ("D".equals(suit)) return "\u2666";
        if ("H".equals(suit)) return "\u2665";
        if ("S".equals(suit)) return "\u2660";
        return suit;
    }

    //                                                                     
    // ── 유틸리티
    //                                                                     

    private JLabel sLabel(String t, Font f, Color c) {
        JLabel l = new JLabel(t); l.setFont(f); l.setForeground(c); l.setOpaque(false); return l;
    }
}
