package engine;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

//TODO
//IMPLEMENT CYCLE BEHAVIOUR
//IMPLEMENT STARTING POSITION AND DIRECTION
//IMPLEMENT HEAD TO HEAD COLLISION
//IMPLEMENT MOCK(KHAZAR)
// DONT POST SCORES OF PLAYERS WHERE USERNAME IS lIKE AI%


public class GameMap {
    @SerializedName(value = "width")
    @Expose
    private int width;
    @SerializedName(value = "height")
    @Expose
    private int height;
    @Expose(serialize = false, deserialize = false)
    private String[][] mapMatrix;
    @SerializedName(value = "obstacles")
    @Expose
    private List<Point> obstacleLocations;
    @SerializedName(value = "power-ups")
    @Expose
    private List<Point> powerUpLocations;
    @Expose(serialize = false, deserialize = false)
    private ArrayList<Player> playerList;
    @SerializedName(value = "tracers")
    @Expose
    private List<Point> tracerLocations;
    @SerializedName(value = "allPointsOnMap")
    @Expose
    private List<Point> allPointsOnMap;
    @Expose(serialize = false, deserialize = false)
    private List<Player> deadPlayers;
    @Expose(serialize = false, deserialize = false)
    private Game game;
    private double turnInterval;
    private boolean cycleBehaviour;

    public GameMap(String[][] i_mapMatrix, List<Player> players, double turnInterval, boolean cycleBehaviour) {

        if (i_mapMatrix == null) {
            System.out.println("Unable to load the map.");
            return;
        }
        this.turnInterval = turnInterval;
        this.cycleBehaviour = cycleBehaviour;
        allPointsOnMap = new ArrayList<>();
        tracerLocations = new ArrayList<>();
        obstacleLocations = new ArrayList<>();
        this.playerList = new ArrayList<>(players);
        this.width = i_mapMatrix[0].length;
        this.height = i_mapMatrix.length;
        this.mapMatrix = new String[height][width];
        for (int i = 0; i < width; i++) {
            System.arraycopy(i_mapMatrix[i], 0, this.mapMatrix[i], 0, height); //handle null later
        }

        int playerCounter = 0;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                Point point = new Point(i, j, this.mapMatrix[i][j]);
                allPointsOnMap.add(point);
                if (this.mapMatrix[i][j].equals("SPAWN") && playerCounter < playerList.size()) {
                    Player player = this.playerList.get(playerCounter++);
                    player.setHeadPosition(point);
                    //player.setCurrentDirection(player.);
                    this.mapMatrix[i][j] = player.getID();
                }
            }
        }
        deadPlayers = new ArrayList<>();
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String[][] getMapMatrix() {
        return mapMatrix;
    }

    public void setMapMatrix(String[][] mapMatrix) {
        this.mapMatrix = mapMatrix;
    }

    public List<Point> getObstacleLocations() {
        return obstacleLocations;
    }

    public void setObstacleLocations(List<Point> obstacleLocations) {
        this.obstacleLocations = obstacleLocations;
    }

    public List<Point> getPowerUpLocations() {
        return powerUpLocations;
    }

    public void setPowerUpLocations(List<Point> powerUpLocations) {
        this.powerUpLocations = powerUpLocations;
    }

    public ArrayList<Player> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(ArrayList<Player> playerList) {
        this.playerList = playerList;
    }

    public List<Point> getTracerLocations() {
        return tracerLocations;
    }

    public void setTracerLocations(List<Point> tracerLocations) {
        this.tracerLocations = tracerLocations;
    }

    public List<Point> getAllPointsOnMap() {
        return allPointsOnMap;
    }

    public void setAllPointsOnMap(List<Point> allPointsOnMap) {
        this.allPointsOnMap = allPointsOnMap;
    }

    public List<Player> getDeadPlayers() {
        return deadPlayers;
    }

    public void setDeadPlayers(List<Player> deadPlayers) {
        this.deadPlayers = deadPlayers;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public double getTurnInterval() {
        return turnInterval;
    }

    public boolean isCycleBehaviour() {
        return cycleBehaviour;
    }

    public void move(Direction direction, Player player, boolean turboPressed) {

        boolean isAlive = false;
        for (Player p : playerList
        ) {
            if (p.getID().equals(player.getID())) {
                isAlive = true;
            }
        }
        if (!isAlive) {
            return;
        }

        if (!turboPressed) {
            move(direction, player);
            calculateScore(player);
        } else {
            for (int i = 0; i < 3; i++) {
                move(direction, player);
                calculateScore(player);
            }
            player.setTurboFlag(false);
        }
    }

    private void move(Direction direction, Player player) {
        tracerLocations.add(player.getHeadPosition());// check later
        if (!player.movePlayer(direction, this)) { //this function will move the player regardless of true or false
            die(player, false);
            return;
        }

        if (player.getHeadPosition().getState() == State.EMPTY) {
            mapMatrix[player.getHeadPosition().getX()][player.getHeadPosition().getY()] = player.getID();
            player.getHeadPosition().setState(State.TRACER);
            return;
        }
        if (player.getHeadPosition().getState() == State.POWERUP) {
            if (player.getTurboAmount() <= 3) {
                player.increaseTurbo();
            }
            mapMatrix[player.getHeadPosition().getX()][player.getHeadPosition().getY()] = player.getID();
            player.getHeadPosition().setState(State.TRACER);
            return;
        }
        if (player.getHeadPosition().getState() == State.OBSTACLE) {
            die(player, true);
            return;
        }
        if (player.getHeadPosition().getState() == State.TRACER) {
            die(player, true);
        }


    }

    public void die(Player player, boolean collision) { //Update it with turns so player will be out of the game
        List<Point> deadTracer = player.die();
        if (cycleBehaviour) {
            for (Point p : deadTracer
            ) {
                if (!collision) {
                    mapMatrix[p.getX()][p.getY()] = "EMPTY";
                    int index = allPointsOnMap.indexOf(p); // incorrect need to change
                    allPointsOnMap.get(index).setState(State.EMPTY);
                    tracerLocations.remove(p);
                } else {
                    if (player.getHeadPosition().getY() != p.getY() || player.getHeadPosition().getX() != p.getX()) {
                        mapMatrix[p.getX()][p.getY()] = "EMPTY";
                        int index = allPointsOnMap.indexOf(p); // incorrect need to change
                        allPointsOnMap.get(index).setState(State.EMPTY);
                        tracerLocations.remove(p);
                    }
                }
            }
        }

        calculateScore(player);
        playerList.remove(player);
        deadPlayers.add(player);
        game.setListOfPlayers(playerList);

        for (int i = 0; i < playerList.size(); i++) {
            Player checkPlayer = playerList.get(i);
            if (player.getHeadPosition().getX() == checkPlayer.getHeadPosition().getX() && player.getHeadPosition().getY() == checkPlayer.getHeadPosition().getY()) {
                checkPlayer.getTracer().add(checkPlayer.getHeadPosition());
                die(checkPlayer,false);
            }
        }

        for (int i = 0; i < playerList.size(); i++) {
            Player checkPlayer = playerList.get(i);
            for (Point p : checkPlayer.getTracer()
            ) {
                if (player.getHeadPosition().getX() == p.getX() && player.getHeadPosition().getY() == p.getY()) {
                    checkPlayer.kill();
                }
            }
        }


        if (playerList.size() <= 1) {
            game.setGameFinished(true);
            try {
                win(playerList.get(0));
            } catch (Exception ex) {

            }

        }
    }

    public void calculateScore(Player player) {

        double score = player.getScore();
        score = player.getNumberOfKills() * 500 + player.getLength() * 200 + player.getTimeElapsed() * 100;
        player.setScore(score);

    }

    public Point getPointOnXY(int x, int y) {
        for (Point p : allPointsOnMap
        ) {
            if (p.getX() == x && p.getY() == y) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        String map = "";
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                map += this.mapMatrix[i][j] + " ";
            }
            map += "\n";
        }
        return map;
    }


    public void win(Player player) throws IOException {
        double timeElapsed = (System.currentTimeMillis() - player.getStartTime()) / 1000.0;
        player.setTimeElapsed(timeElapsed);
        calculateScore(player);

        if(!player.getID().substring(0,2).equals("AI")) {
            String requestParameter = "userID=" + player.getID() + "&score=" + player.getScore() + "&wins=1" + "&kills=" + player.getNumberOfKills() + "&timePlayed" + player.getTimeElapsed();
            URL scoreBoardURL = new URL("https://scores-and-leaderboards-dot-trainingprojectlab2019.appspot.com/RegisterGame?userID=" + requestParameter);
            HttpURLConnection scoreBoardConnection = (HttpURLConnection) scoreBoardURL.openConnection();
            scoreBoardConnection.setRequestMethod("POST");
            scoreBoardConnection.connect();
        }
        for (Player p : deadPlayers
        ) {
            String aiName = p.getID().substring(0,2);
            if (!aiName.equals("AI")) {
                String   requestParameter = "userID=" + p.getID() + "&score=" + p.getScore() + "&wins=0" + "&kills=" + p.getNumberOfKills() + "&timePlayed" + p.getTimeElapsed();
                URL scoreBoardURL = new URL("https://scores-and-leaderboards-dot-trainingprojectlab2019.appspot.com/RegisterGame?userID=" + requestParameter);
                HttpURLConnection scoreBoardConnection = (HttpURLConnection) scoreBoardURL.openConnection();
                scoreBoardConnection.setRequestMethod("POST");
                scoreBoardConnection.connect();
            }

        }
    }

}







