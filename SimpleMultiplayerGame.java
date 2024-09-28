import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleMultiplayerGame extends JFrame implements Runnable {
    private static final int WIDTH = 800, HEIGHT = 600;
    private static final String SERVER_ADDRESS = "localhost"; // Change as needed
    private static final int PORT = 12345;

    private volatile boolean running;
    private volatile Point playerPos = new Point(WIDTH / 2, HEIGHT / 2);
    private List<Point> otherPlayers = new ArrayList<>();
    private Socket socket;

    public SimpleMultiplayerGame() {
        setTitle("Simple 2D Multiplayer Game");
        setSize(WIDTH, HEIGHT);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_LEFT: playerPos.x -= 5; break;
                    case KeyEvent.VK_RIGHT: playerPos.x += 5; break;
                    case KeyEvent.VK_UP: playerPos.y -= 5; break;
                    case KeyEvent.VK_DOWN: playerPos.y += 5; break;
                }
            }
        });

        try {
            socket = new Socket(SERVER_ADDRESS, PORT);
            new Thread(this).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        running = true;
        while (running) {
            update();
            sendPlayerPosition();
            receivePlayerPositions();
            repaint();
            try {
                Thread.sleep(16); // Roughly 60 frames per second
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        // Update game state
    }

    private void sendPlayerPosition() {
        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(playerPos.x);
            out.writeInt(playerPos.y);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receivePlayerPositions() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            otherPlayers.clear();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                otherPlayers.add(new Point(in.readInt(), in.readInt()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(Color.RED);
        g.fillRect(playerPos.x, playerPos.y, 20, 20); // Draw player

        g.setColor(Color.BLUE);
        for (Point p : otherPlayers) {
            g.fillRect(p.x, p.y, 20, 20); // Draw other players
        }
    }

    public static void main(String[] args) {
        new SimpleMultiplayerGame();
        new Server().start(); // Start the server
    }

    // Simple server to handle player connections
    static class Server extends Thread {
        private List<Point> players = new ArrayList<>();
        private ServerSocket serverSocket;

        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new ClientHandler(clientSocket, players).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ClientHandler extends Thread {
        private Socket clientSocket;
        private List<Point> players;

        public ClientHandler(Socket socket, List<Point> players) {
            this.clientSocket = socket;
            this.players = players;
        }

        public void run() {
            try {
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                while (true) {
                    int x = in.readInt();
                    int y = in.readInt();
                    players.add(new Point(x, y));
                    sendPlayerPositions();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendPlayerPositions() throws IOException {
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            out.writeInt(players.size()); // Send number of players
            for (Point p : players) {
                out.writeInt(p.x);
                out.writeInt(p.y);
            }
        }
    }
}
