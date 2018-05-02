
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.Timer;

import sun.audio.AudioPlayer;
import sun.audio.AudioData;
import sun.audio.AudioStream;
import sun.audio.ContinuousAudioDataStream;


/**
 * Created by Catrina on 5/14/16.
 *
 *  update 1.0 5/14/16
 *   create a playable version
 *   1. generate fruit & snake
 *   2. timer & timer task
 *   3. score calculation
 *
 *  update 1.1 5/16/16
 *   1. speedup problem
 *   2. hitting problem (tail disappear)
 *   3. quick key pressing
 *   4. fruit-jump (delay)
 *
 *  update 1.2 5/25/16
 *   1. resize the window to 800x600
 *   2. optimize the screen
 *   3. add info page
 *   4. < Q > to quit
 *   5. update function changelevel
 *
 *  update 1.3 5/26/16
 *   1. speed up/down
 *   2. more key features
 *   3. optimize fruit
 *   3. add trap function
 *   4. add sound
 *
 */

public class snake extends JComponent {

    private LinkedList<Point> sbody;
    private LinkedList<Point> trap = new LinkedList<Point>();
    private LinkedList<Point> draw_trap = new LinkedList<Point>();
    private Point fruit;
    private Image fruitI;


    private int score = 0;
    private int distance = 0; //
    private int moveGap = 0;
    private int fruit_count = 0;

    static private int framerate = 40;
    static private int level = 1;
    private int speed = 260 - level * 20;

    private Timer update = new Timer();
    private LevelUpdate levelUpd;
    private boolean hasMoved = false;


    private enum Direction {NO_DIRECTION, NORTH, SOUTH, EAST, WEST}
    private Direction direction = Direction.NO_DIRECTION;

    private enum State {SPLASH,MAIN_MENU, PAUSE, GAME, GAME_OVER}
    private State status = State.SPLASH;

    private enum Tip{GUIDE, INFO, NONE}
    private Tip tips = Tip.NONE;

    private int BOX_LENGTH = 12;
    private int GRID_WIDTH = 65;
    private int GRID_HEIGHT = 45;



    private Color fadewhite = new Color(255,255,255,230);
    private int splash_count = 0;


    private class ScreenUpdate extends TimerTask{
        @Override
        public void run(){
            repaint();
        }
    }
    private class LevelUpdate extends TimerTask{
        @Override
        public void run(){
            hasMoved = false;
            moveGap++;
            move();
        }
    }


    public static void main(String[] args){

        if(args.length == 2){
            framerate = Integer.parseInt(args[0]);
            level = Integer.parseInt(args[1]);
        }

        snake canvas = new snake();
        JFrame frame = new JFrame("SnakeGame");

        JPanel panel = new JPanel();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvas.setFocusable(true);
        frame.setSize(800,600);
        panel.setPreferredSize(new Dimension(800,600));
        panel.setBackground(Color.BLACK);


        panel.add(canvas);
        frame.add(panel,BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    public void paintComponent(Graphics g){

        if(status == State.SPLASH){
            if(splash_count < 10*framerate){
                splash_count++;
                DrawSplash(g);
            }else status = State.MAIN_MENU;

            return;
        }

        DrawGrid(g);

        if(status == State.MAIN_MENU){

            DrawMain(g);
        }else{

            g.setColor(fadewhite);
            Image background = Toolkit.getDefaultToolkit().getImage("background.jpg");
            g.drawImage(background,0,0,790,565,this);
            g.fillRect(0,0,790,565);


            g.setColor(Color.BLACK);
            g.fillRect(0,540,780,5);
            DrawSnake(g);
            DrawFruit(g);
            DrawScore(g);
            DrawLevel(g);
            DrawFrameRate(g);
            DrawTrap(g);
            if(status==State.PAUSE){
                DrawPause(g);
            }if(status==State.GAME_OVER){
                DrawDie(g);
            }
        }
        if(tips == Tip.GUIDE) DrawGuide(g);
        if(tips == Tip.INFO) DrawINFO(g);
    }
    private snake(){

        setPreferredSize(new Dimension(780,565));

        ScreenUpdate screenUpd = new ScreenUpdate();
        update.schedule(screenUpd, 0, framerate);

        playSound(0);

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e){
                switch (e.getKeyCode()){
                    case KeyEvent.VK_G:
                        if(tips == Tip.GUIDE){
                            tips = Tip.NONE;
                        }
                        break;
                    case KeyEvent.VK_I:
                        if(tips == Tip.INFO){
                            tips = Tip.NONE;
                        }
                        break;
                }
            }
            @Override
            public void keyPressed(KeyEvent e) {
//                super.keyPressed(e);
                switch (e.getKeyCode()){

                    case KeyEvent.VK_SPACE:
                        if(status == State.GAME){
                            levelUpd.cancel();
                            status = State.PAUSE;
                        }else if(status == State.PAUSE){
                            status = State.GAME;
                            levelUpd = new LevelUpdate();
                            update.schedule(levelUpd, 0, speed);
                        }
                        break;
                    case KeyEvent.VK_S:
                        if(status == State.MAIN_MENU){
                            score = 0;
                            GenerateSnake();
                            GenerateFruit();
                            status = State.GAME;
                            changeLevel(0);
                        }
                        break;

                    case KeyEvent.VK_R:
                        if(status == State.PAUSE || status == State.GAME_OVER){
                            score = 0;
                            GenerateSnake();
                            GenerateFruit();;
                            status = State.GAME;
                            // if(status == State.PAUSE) levelUpd.cancel();
                            level = 1;
                            changeLevel(0);
                        }
                        break;

                    case KeyEvent.VK_G:
                        tips = Tip.GUIDE;
                        break;

                    case KeyEvent.VK_I:
                        tips = Tip.INFO;
                        break;

                    case KeyEvent.VK_M:
                        if(status == State.PAUSE || status == State.GAME_OVER){
                            status = State.MAIN_MENU;
                            // levelUpd.cancel();
                            level = 1;
                        }
                        break;

                    case KeyEvent.VK_A:
                        if(status == State.GAME){
                            changeLevel(1);

                        }
                        break;

                    case KeyEvent.VK_Z:
                        if(status == State.GAME){
                            changeLevel(-1);
                        }
                        break;

                    case KeyEvent.VK_K:
                        if(status == State.SPLASH){
                            status = State.MAIN_MENU;
                        }
                        break;

                    case KeyEvent.VK_Q:
                        if(status == State.MAIN_MENU){
                            System.exit(0);
                        }
                        break;

                    case KeyEvent.VK_UP:

                        if (direction != Direction.SOUTH  && status == State.GAME && !hasMoved){
                            direction = Direction.NORTH;
                            hasMoved = true;
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        if (direction != Direction.NORTH && status == State.GAME  && !hasMoved){
                            direction = Direction.SOUTH;
                            hasMoved = true;
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if(direction != Direction.WEST && status == State.GAME  && !hasMoved){
                            direction = Direction.EAST;
                            hasMoved = true;
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        if(direction != Direction.EAST && status == State.GAME  && !hasMoved){
                            direction = Direction.WEST;
                            hasMoved = true;
                        }
                        break;
                }
            }});
    }

    private void GenerateSnake(){
        sbody = new LinkedList<Point>();
        sbody.add(new Point(20,2));
        sbody.add(new Point(20,1));
        sbody.add(new Point(20,0));
        direction = Direction.SOUTH;
    }
    private void GenerateFruit(){
        Random rand = new Random();
        int randX = rand.nextInt(GRID_WIDTH);
        int randY = rand.nextInt(GRID_HEIGHT);
        Point randPoint = new Point(randX,randY);
        while(sbody.contains(randPoint)){
            randX = rand.nextInt(GRID_WIDTH);
            randY = rand.nextInt(GRID_HEIGHT);
            randPoint = new Point(randX, randY);
        }
        fruit = randPoint;
        distance = (int)(Math.sqrt(Math.pow(fruit.x - sbody.peekFirst().x, 2)+
        Math.pow(fruit.y - sbody.peekFirst().y, 2)));

        int randfruit = rand.nextInt(24);
        if(randfruit >= 18) playSound(3);
        if(randfruit >= 4  && randfruit <=7) playSound(randfruit);
        fruitI = Toolkit.getDefaultToolkit().getImage("animal_" + randfruit + ".png");

    }
    private void GenerateTrap(){
        Random rand = new Random();
        LinkedList<Point> temp = new LinkedList<Point>();
        int randX = rand.nextInt(GRID_WIDTH);
        int randY = rand.nextInt(GRID_HEIGHT);
        Point seg;

        for(int i = 0; i < 3; ++i){
            for(int j = 0; j < 3; ++j){
                seg = new Point(randX+i,randY+j);
                if(sbody
                        .contains(seg) || seg.equals(fruit) || temp.contains(seg)
                        || seg.x > GRID_WIDTH -1 || seg.y > GRID_HEIGHT -1){
                    randX = rand.nextInt(GRID_WIDTH);
                    randY = rand.nextInt(GRID_HEIGHT);
                    i = 0;
                    temp = new LinkedList<Point>();
                    break;
                }
                temp.add(seg);
            }
        }
        draw_trap.add(new Point(randX,randY));
        for(Point p : temp){
            trap.add(p);
        }
    }

    private void DrawSplash(Graphics g){

        g.setColor(Color.WHITE);
        g.fillRect(0,0,790,565);


        Color splashcolor = new Color(98, 89, 65, 255);
        g.setColor(splashcolor);

        g.setFont(new Font("Cooper Black", Font.BOLD, 28));
        g.drawString("Name: Tianrui Ma", 200,150);
        g.drawString("UserID: t33ma (20550222)", 200,180);

        g.setFont(new Font("Cooper Black", Font.BOLD, 20));
        g.drawString("< ARROW > keys to Navigate", 200,240);
        g.drawString("< SPACE > to Pause/Resume", 200,270);
        g.drawString("< A/Z > to Speed Up/Down", 200,300);
        g.drawString("< G > to Instructions (cuter version)", 200,330);

        g.drawString(10 - splash_count/framerate + " seconds left... Press < K > to Skip", 370, 530);

    }

    private void DrawMain(Graphics g){

        Image main_background = Toolkit.getDefaultToolkit().getImage("main_background.jpg");
        g.drawImage(main_background,0,0,790,565,this);

        Color main_title = new Color(61,61,58,255);
        g.setColor(main_title);
        g.setFont(new Font("Cooper Black", Font.BOLD, 33));
        g.drawString("SNAKE IN ZOO", 38,150);

        Color main_start = new Color(79,79,74,255);
        g.setColor(main_start);
        g.setFont(new Font("Cooper Black", Font.BOLD, 26));
        g.drawString("Press < S > to Start", 50,250);


        g.setFont(new Font("Cooper Black", Font.BOLD, 23));
        Color main_guide = new Color(117,117,111,255);

        g.setColor(main_guide);
        g.drawString("Hold < G > to Guide", 50,290);
        g.setFont(new Font("Cooper Black", Font.BOLD, 21));
        Color main_info = new Color(102,102,96,255);
        g.setColor(main_info);
        g.drawString("Hold < I > to INFO", 50,330);

        g.setFont(new Font("Cooper Black", Font.BOLD, 18));

        Color main_quit = new Color(115,115,112,255);
        g.setColor(main_quit);
        g.drawString("Press < Q > to QUIT", 50,368);

        g.setFont(new JLabel().getFont());
    }
    private void DrawGuide(Graphics g){
        Color guide_back = new Color(216, 237, 201, 240);

        g.setColor(guide_back);
        g.fillRoundRect(200,120,360,282,35,35);


        g.setColor(Color.DARK_GRAY);
        drawDashedLine(g, 204,124,352,274);
        drawThickLine(g, 200,120,360,282);
        g.setFont(new Font("Cooper Black", Font.BOLD, 20));
        g.drawString("HoW to PlaY", 320,170);
        g.setFont(new Font("Cooper Black", Font.BOLD, 12));
        g.drawString("> Use < ARROW > keys to guide me :) ", 220,210);
        g.drawString("> Chase those doublicious ANIMALS .... Yummy! ", 220,230);
        g.drawString("> I grow BIGGER and FASTER!  YOHO~  ", 220,250);
        g.drawString("> I SPEED UP if you give me an < A > ", 220,270);
        g.drawString("> DON'T HIT the wall or BITE me", 220,290);

        g.drawString("> Oh watch out! SPIDER is EVIL   >_<", 220,310);
        g.drawString("> Too Fast? Press < Z > to slow down", 220,330);
        g.drawString("> BRB? press < SPACE > to PAUSE and RESUME", 220,350);
        g.drawString("> HOLD < G > at ANYTIME to see this TIP again", 220,370);
        g.setFont(new JLabel().getFont());
    }
    private void DrawINFO(Graphics g){
        Color guide_back = new Color(216, 237, 201, 240);

        g.setColor(guide_back);
        g.fillRoundRect(270,180,240,172,35,35);


        g.setColor(Color.DARK_GRAY);
        drawDashedLine(g, 274,184,232,164);
        drawThickLine(g, 270,180,240,172);
        g.setFont(new Font("Cooper Black", Font.BOLD, 14));
        g.drawString("NAME: Tianrui Ma", 315,240);
        g.drawString("USERID: t33ma (20550222)", 295,300);
        g.setFont(new JLabel().getFont());
    }

    private void DrawGrid(Graphics g){
        g.setColor(Color.WHITE);
        g.fillRect(0,0,790,565);
    }
    private void DrawSnake(Graphics g){
        for(Point p : sbody){

            g.setColor(Color.DARK_GRAY);
            if(p == sbody.getFirst()) g.setColor(Color.BLACK);
            g.fillRoundRect(p.x*BOX_LENGTH,p.y*BOX_LENGTH,BOX_LENGTH,BOX_LENGTH,4,4);
        }
    }
    private void DrawFruit(Graphics g){

        g.drawImage(fruitI,fruit.x * BOX_LENGTH-4, fruit.y * BOX_LENGTH-4, BOX_LENGTH+8,BOX_LENGTH+8,this);
    }
    private void DrawTrap(Graphics g){
        Image spider = Toolkit.getDefaultToolkit().getImage("spider.png");
        for(Point p : draw_trap){
            g.drawImage(spider,p.x * BOX_LENGTH, p.y * BOX_LENGTH, BOX_LENGTH*3,BOX_LENGTH*3,this);
        }
    }
    private void DrawScore(Graphics g){
        g.setColor(Color.BLACK);
        g.drawString("Score: " + score, 10, BOX_LENGTH*GRID_HEIGHT+20);
    }
    private void DrawFrameRate(Graphics g){
        g.setColor(Color.BLACK);
        g.drawString("Frame-rate: " + framerate, 630, BOX_LENGTH*GRID_HEIGHT+20);
    }
    private void DrawLevel(Graphics g){
        g.setColor(Color.BLACK);
        g.drawString("speed: " + level, 200, BOX_LENGTH*GRID_HEIGHT+20);
    }
    private void DrawPause(Graphics g){
        Color pause_back = new Color(255,255,255,150);
        g.setColor(pause_back);
        g.fillRect(0,0,790,565);

        g.setColor(Color.pink);
        g.setFont(new Font("Cooper Black", Font.BOLD, 33));
        g.drawString("PAUSE", 345,200);
        g.setColor(Color.GRAY);
        g.setFont(new Font("Cooper Black", Font.BOLD, 14));

        g.drawString("Press < SPACE > to Resume", 290,250);
        g.drawString("Press < R > to Restart", 290,270);
        g.drawString("Press < M > to Main Menu", 290,290);
        g.setFont(new JLabel().getFont());
    }
    private void DrawDie(Graphics g){
        g.setColor(Color.GRAY);
        g.setFont(new Font("Cooper Black", Font.BOLD, 33));
        g.drawString("GAME OVER", 290,200);
        g.setFont(new Font("Cooper Black", Font.BOLD, 22));
        g.drawString("Press < R > to Restart", 260,290);
        g.drawString("Press < M > to Main Menu", 260,330);
        g.setFont(new JLabel().getFont());
    }
    private void drawThickLine(Graphics g, int x, int y, int w, int l){
        //creates a copy of the Graphics instance
        Graphics2D g2 = (Graphics2D) g.create();

        //set the stroke of the copy, not the original
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x,y,w,l,35,35);

        //gets rid of the copy
        g2.dispose();
    }
    private void drawDashedLine(Graphics g, int x, int y, int w, int l){

        //creates a copy of the Graphics instance
        Graphics2D g2d = (Graphics2D) g.create();

        //set the stroke of the copy, not the original
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g2d.setStroke(dashed);
        g2d.drawRoundRect(x, y, w, l,35,35);

        //gets rid of the copy
        g2d.dispose();
    }
    private void changeLevel(int i){
        if(i==0){
            trap = new LinkedList<Point>();
            draw_trap = new LinkedList<Point>();
            fruit_count = 0;
        }else if((level == 10 && i == -1) ||
                (level == 1 && i == 1) ||
                (level < 10 && level > 1)){
            level+=i;
            levelUpd.cancel();
        }else{
            return;
        }
        speed = 260 - level * 20;
        System.out.println("speed : "+ speed);
        levelUpd = new LevelUpdate();
        update.schedule(levelUpd, speed, speed);
    }
    private void move(){
        Point head = sbody.peekFirst();
        Point nextPoint = head;

        switch(direction){
            case NORTH:
                nextPoint = new Point(head.x, head.y - 1);
                break;
            case SOUTH:
                nextPoint = new Point(head.x, head.y + 1);
                break;
            case WEST:
                nextPoint = new Point(head.x - 1, head.y);
                break;
            case EAST:
                nextPoint = new Point(head.x + 1, head.y);
                break;
            case NO_DIRECTION:
                return;
        }

        if(nextPoint.equals(fruit)){
            score += 15 * distance / moveGap + level * 10;
            moveGap = 0;
            sbody.push(nextPoint);
            GenerateFruit();
            fruit_count++;
            if(fruit_count%3 == 0) {
                GenerateTrap();
            }
            if(fruit_count%9 == 0) {
                changeLevel(1);
            }
            playSound(2);
        }else{
            Point last = sbody.peekLast();
            sbody.removeLast();
            // out of bounds or hit itself
            if (nextPoint.x < 0 || nextPoint.x > GRID_WIDTH - 1 ||
                    nextPoint.y < 0 || nextPoint.y > GRID_HEIGHT - 1 ||
                    sbody.contains(nextPoint) || trap.contains(nextPoint)) {
                if(trap.contains(nextPoint)) sbody.addFirst(nextPoint);
                else sbody.addLast(last);

                levelUpd.cancel();
                status = State.GAME_OVER;
                playSound(1);
                return;
            }
            sbody.addFirst(nextPoint);
        }
    }

    private void playSound(int a) {
        AudioStream BGM;
        AudioData MD;
        InputStream in;
        ContinuousAudioDataStream loop = null;
        try
        {
            in = new FileInputStream("sound_"+a+".wav");
            BGM = new AudioStream(in);

            if(a==0){
                MD = BGM.getData();
                loop = new ContinuousAudioDataStream(MD);
                AudioPlayer.player.start(loop);
            }else{
                // sound_1   -> game over
                // sound_2   -> eat
                // sound_3   -> lions
                // sound_4   -> cat
                // sound_5   -> dog
                // sound_6   -> pig
                AudioPlayer.player.start(BGM);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
