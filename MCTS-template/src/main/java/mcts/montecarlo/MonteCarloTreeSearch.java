package mcts.montecarlo;

import mcts.game.Board;
import mcts.game.Field;
import mcts.game.Move;
import mcts.game.MoveType;
import mcts.tree.Node;
import mcts.tree.Tree;

import java.util.List;

public class MonteCarloTreeSearch {
    private static final int SIMULATION_TIME = 1000;
    private static final int SIMULATION_DEPTH = 6;
    private static final double COEF = 0.9;
    private double maxScore = 0;

    public MonteCarloTreeSearch() {
        for (int i = 0; i<=SIMULATION_DEPTH; i+=2) {
            maxScore += Math.pow(COEF, i);
        }
    }

    public Move findNextMove(Board board) throws InterruptedException {
        long start = System.currentTimeMillis();
        long end = start + SIMULATION_TIME;
        int myPlayerId = board.getCurrentPlayerIndex();

        if(board.getTurns() < 4){
            return board.getBestInitialMove();
        }

        Tree tree = new Tree();
        Node rootNode = tree.getRoot();
        rootNode.getState().setBoard(board);

        System.out.println("possible moves: " + board.getLegalMoves().size());

        while (System.currentTimeMillis() < end) {
            //TimeUnit.MILLISECONDS.sleep(100);
            // Phase 1 - Selection
            Node promisingNode = selectPromisingNode(rootNode);
            // Phase 2 - Expansion
            if (promisingNode.getState().getBoard().isRunning())
                expandNode(promisingNode);

            // Phase 3 - Simulation
            Node nodeToExplore = promisingNode;
            if (promisingNode.getChildArray().size() > 0) {
                nodeToExplore = promisingNode.getRandomChildNode();
            }
            double score = simulateRandomPlayout(nodeToExplore, myPlayerId);
            // Phase 4 - Update
            backPropagation(nodeToExplore, score);
        }
        Node winnerNode = rootNode.getChildWithMaxVisits();
        return winnerNode.getState().getInitialMove();
    }

    private Node selectPromisingNode(Node rootNode) {
        Node node = rootNode;
        while (node != null && node.getChildArray().size() != 0) {
            node = UCT.findBestNodeWithUCT(node);
        }
        return node;
    }

    private void expandNode(Node node) {
        List<State> possibleStates = node.getState().getAllPossibleStates();
        for (State state : possibleStates) {
            Node newNode = new Node(state);
            newNode.setParent(node);
            node.getChildArray().add(newNode);
        }
    }

    private double simulateRandomPlayout(Node node, int myPlayerId) {
        Node tempNode = node.copy();
        Board tempBoard = tempNode.getState().getBoard();
        double score = scoreFunctionPoints(tempBoard, myPlayerId);
        score += scoreFunctionMove(tempBoard, tempNode.getState().getInitialMove());

        for(int i = 1; i <= SIMULATION_DEPTH; i++) {
            Move randomMove = tempBoard.getRandomMove();
            int currentPlayerId = tempBoard.getCurrentPlayerIndex();
            tempBoard.playMove(randomMove);
            if(currentPlayerId == myPlayerId){
                score += scoreFunctionPoints(tempBoard, myPlayerId);
                score += scoreFunctionMove(tempBoard, randomMove);
            }
            if(!tempBoard.isRunning()){
                break;
            }
        }
        return score / SIMULATION_DEPTH;
    }

    private double scoreFunctionPoints(Board board, int playerId){
        return board.getPlayers()[playerId].getPoints();
    }

    private double scoreFunctionMove(Board board, Move randomMove) {
        double score = 0;
        if(randomMove.getType() == MoveType.BUILD_TOWN){
            score += buildTownScoreFunction(board, randomMove) / 100;
        } else if(randomMove.getType() == MoveType.UPGRADE_TOWN){
            score += upgradeTownScoreFunction(board, randomMove) / 100;
        } else if(randomMove.getType() == MoveType.BUILD_ROAD){
            score += buildRoadScoreFunction(board, randomMove) / 400;
        }
        return score;
    }

    private double upgradeTownScoreFunction(Board board, Move randomMove) {
        double score = 0;
        for(Field f : board.getIndexIntersections().get(randomMove.getIndex1()).getAdjacentFields()){
            score += f.getWeight();
        }
        return score;
    }

    private double buildRoadScoreFunction(Board board, Move randomMove) {
        double score = 0;
        for(Field f : board.getIndexIntersections().get(randomMove.getIndex1()).getAdjacentFields()){
            score += f.getWeight();
        }
        return score;
    }

    private double buildTownScoreFunction(Board board, Move randomMove) {
        double score = 0;
        for(Field f : board.getCurrentPlayer().getCurrentIntersection().getAdjacentFields()){
            score += f.getWeight();
        }
        return score;
    }

    private double initialMoveScoreFunction(Board board, Move randomMove){
        double score = 0;
        for(Field f : board.getIndexIntersections().get(randomMove.getIndex1()).getAdjacentFields()){
            switch (f.getResource()){
                case WOOD:
                case CLAY:
                    score += f.getWeight() * 4;
                    break;
                case WHEAT:
                case SHEEP:
                    score += f.getWeight() * 3;
            }
        }
        return score * 100;
    }

    private void backPropagation(Node nodeToExplore, double score) {
        Node tempNode = nodeToExplore;
        while (tempNode != null) {
            tempNode.getState().incrementVisit();
            tempNode.getState().addScore(score);
            tempNode = tempNode.getParent();
        }
    }
}
