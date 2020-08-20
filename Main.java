import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JDialog;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class Main {
    public static void main(String[] argv) {
        new GameLauncher();
    }
}

// launcher menu to choose difficulty
class GameLauncher implements ActionListener{
    JFrame frame;
    public GameLauncher() {
        frame = new JFrame("New Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(null);

        // 3 possible difficulties
        int[] difInt = {3, 4, 5};
        String[] difStr = {"Easy", "Medium", "Hard"};

        frame.setLocation(200, 300);
        frame.setSize(335, 45 + 60 * 3);

        for(int i = 0; i < 3; i++) {
            JButton b = new JButton(difStr[i] + " " + difInt[i] + "x" + difInt[i]);
            b.setName(String.valueOf(difInt[i]));
            b.setSize(300, 50);
            b.setLocation(10, 10 + 60 * i);
            b.addActionListener(this);
            frame.add(b);
        }

        frame.setResizable(false);
        frame.setVisible(true);
    }

    @Override public void actionPerformed(ActionEvent e) {
        int gameSize = Integer.valueOf(((JButton) e.getSource()).getName());
        frame.dispose();
        new Game(gameSize, gameSize);
    }
}

// game class
class Game implements ActionListener {
    int score = 0;
    int gameWidth = 4;
    int gameHeight = 4;
    int buttonWidth = 130;
    int buttonHeight = 130;
    int offset = 5;

    JFrame frame;
    MyButton[][] buttons;

    public Game(int width, int height) {
        gameWidth = width;
        gameHeight = height;
        frame = new JFrame("Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(new Dimension(buttonWidth * gameWidth + 2 * offset + 5, buttonHeight * gameHeight + 2 * offset + 30));
        frame.setLocation(200, 300);
        frame.setResizable(false);
        frame.setLayout(null);

        buttons = getButtons(gameWidth, gameWidth);

        frame.setVisible(true);
    }

    // here button order is generated and button array is filled
    public MyButton[][] getButtons(int gameHeight, int gameWidth) {
        int n = gameHeight * gameWidth - 1;
        int[] arr = new int[n];
        for(int i = 0; i < n; i++)
            arr[i] = i + 1;

        boolean solvable;
        do {
            // mix order of tiles
            Random rand = new Random();
            for(int i = 0; i < n * 2; i++) {
                int l = rand.nextInt(n);
                int r = rand.nextInt(n);
                int t = arr[l];
                arr[l] = arr[r];
                arr[r] = t;
            }

            // check if game solvable
            int countInversions = 0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < i; j++) {
                    if (arr[j] > arr[i])
                        countInversions++;
                }
            }
            solvable = countInversions % 2 == 0;

        } while(!solvable);


        MyButton[][] buttons = new MyButton[gameHeight][gameWidth];
        for (int row = 0; row < gameHeight; row++) {
            for (int column = 0; column < gameWidth; column++) {
                if (row != gameHeight - 1 || column != gameWidth - 1) {
                    buttons[row][column] = new MyButton(buttonWidth - offset, buttonHeight - offset,
                            arr[row * gameWidth + column], row * gameWidth + column);
                    buttons[row][column].setLocation(offset + column * buttonHeight, offset + row * buttonWidth);
                    buttons[row][column].addActionListener(this);
                    frame.add(buttons[row][column]);
                }
                else {
                    buttons[row][column] = null;
                }
            }
        }
        return buttons;
    }

    // checks if game is finished
    public boolean isSolved() {
        boolean solved = true;
        int prev = 0;
        for(int i = 0; solved && i < gameHeight * gameWidth - 1; i++) {
            if (buttons[i / gameWidth][i % gameWidth] == null) return false;
            int curr = buttons[i / gameWidth][i % gameWidth].index;
            if (curr <= prev) solved = false;
            prev = curr;
        }
        return solved;
    }

    // respond to tile click
    @Override public void actionPerformed(ActionEvent e) {
        MyButton mb = (MyButton) e.getSource();
        int row = mb.pos / gameWidth, dRow = 0;
        int column = mb.pos % gameWidth, dColumn = 0;

        if      (row > 0                && buttons[row - 1][column] == null)    dRow = -1;
        else if (row < gameHeight - 1   && buttons[row + 1][column] == null)    dRow = 1;
        else if (column > 0             && buttons[row][column - 1] == null)    dColumn = -1;
        else if (column < gameWidth - 1 && buttons[row][column + 1] == null)    dColumn = 1;
        else return;

        score++;
        buttons[row][column].pos = (row + dRow) * gameWidth + (column + dColumn);
        MyButton button = buttons[row][column];

        if (button.animation != null && button.animation.isAlive()) {
            button.toRun = false;
            try {
                button.animation.join();
            } catch (Exception ee) {
                System.out.println(ee.toString());
            }
            button.animation = null;
        }

        button.toRun = true;
        button.animation = new Animation(buttons[row][column], this, dRow, dColumn);
        button.animation.start();

        buttons[row + dRow][column + dColumn] = buttons[row][column];
        buttons[row][column] = null;

        if (isSolved()) {
            frame.dispose();
            new GameFinished();
        }
    }
}

// dialog to notify that game is won
class GameFinished {
    JFrame frame;
    JDialog dialog;
    public GameFinished() {
        frame = new JFrame();
        dialog = new JDialog(frame, "You Won", true);
        dialog.setLayout(new FlowLayout());
        JButton b = new JButton ("Nice");
        b.addActionListener (new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });

        dialog.add(new JLabel("Congratulations!"));
        dialog.add(b);
        dialog.setSize(300,100);
        dialog.setLocation(200, 300);
        dialog.setVisible(true);
    }
}

// animation to move game tiles
class Animation extends Thread {
    MyButton button;
    Game game;
    int dy, dx;

    int animationTime = 250;
    public Animation(MyButton button, Game game, int dy, int dx) {
        this.button = button;
        this.game = game;
        this.dy = dy;
        this.dx = dx;
    }
    public void run() {
        if (dy == 0 && dx == 0) return;
        try {
            int y = button.getBounds().y;
            int x = button.getBounds().x;

            if (dy != 0) {  // vertical move
                int yFinal = button.pos / game.gameWidth * game.buttonHeight + (dy != 0 ? 1 : 0) * game.offset;
                int delay = animationTime / game.buttonHeight;

                while (button.toRun && y != yFinal) {
                    y += dy;
                    button.setLocation(x, y);
                    game.frame.repaint();
                    Thread.sleep(delay);
                }
            }
            if (dx != 0) {  // horizontal move
                int xFinal = button.pos % game.gameWidth * game.buttonWidth + (dx != 0 ? 1 : 0) * game.offset;
                int delay = animationTime / game.buttonWidth;

                while (button.toRun && x != xFinal) {
                    x += dx;
                    button.setLocation(x, y);
                    game.frame.repaint();
                    Thread.sleep(delay);
                }
            }

            button.toRun = false;
        }
        catch (Exception e) {
            System.out.println ("Exception is caught: " + e.toString());
        }
    }
}

// custom JButton representing game tile
class MyButton extends JButton {
    public int index, pos;      // index - number of tile
    public boolean toRun;       // used to stop animation
    public Animation animation; // moving animation thread

    public MyButton(int width, int height, int index, int pos) {
        super(String.valueOf(index));
        setName(String.valueOf(index));
        this.pos = pos;
        animation = null;
        this.setSize(width, height);
    }
}
