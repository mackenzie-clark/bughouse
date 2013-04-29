package edu.brown.cs32.bughouse.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs32.bughouse.exceptions.GameNotReadyException;
import edu.brown.cs32.bughouse.exceptions.IllegalMoveException;
import edu.brown.cs32.bughouse.exceptions.RequestTimedOutException;
import edu.brown.cs32.bughouse.exceptions.TeamFullException;
import edu.brown.cs32.bughouse.exceptions.UnauthorizedException;
import edu.brown.cs32.bughouse.exceptions.WrongColorException;
import edu.brown.cs32.bughouse.interfaces.BackEnd;
import edu.brown.cs32.bughouse.interfaces.Client;
import edu.brown.cs32.bughouse.interfaces.FrontEnd;
import edu.brown.cs32.bughouse.models.ChessBoard;
import edu.brown.cs32.bughouse.models.ChessPiece;
import edu.brown.cs32.bughouse.models.Game;
import edu.brown.cs32.bughouse.models.Model;
import edu.brown.cs32.bughouse.models.Player;

public class BughouseBackEnd implements BackEnd {
	private Client client;
	private Player me;
	private FrontEnd frontEnd;
	private HashMap<Integer, List<ChessPiece>> prisoners;
	private Map<Integer, ChessBoard> currentBoards;
	public BughouseBackEnd(FrontEnd frontEnd,String host, int port) throws UnknownHostException, IllegalArgumentException, IOException {
		this.frontEnd = frontEnd;
		this.client = new BughouseClient(host,port,this);
		Model.setClient(client);
		this.prisoners = new HashMap<Integer, List<ChessPiece>>();
	}
	

	

	@Override
	public void quit() throws IOException, RequestTimedOutException {
		client.quit(me.getId());
	}

	@Override
	public Player joinServer(String name) throws UnknownHostException, IOException, RequestTimedOutException {
		int playerId = client.addNewPlayer(name);
		this.me = new Player(playerId);
		return me;
	}

	@Override
	public List<Game> getActiveGames() throws IOException, RequestTimedOutException {
		List<Game> games = new ArrayList<Game>();
		List<Integer> gameIds = client.getGames();
		for (int gameId: gameIds)
			games.add(new Game(gameId));
		return games;
	}
	

	@Override
	public void joinGame(int gameId, int team) throws IOException, RequestTimedOutException, TeamFullException {
		client.joinGame(me.getId(), gameId, team);
	}

	@Override
	public void createGame() throws IOException, RequestTimedOutException {
		client.createGame(me.getId());
	}

	@Override
	public void startGame() throws IOException, RequestTimedOutException, GameNotReadyException, UnauthorizedException {
		Game game = me.getCurrentGame();
		if (me.getId()==game.getOwnerId()) {
			client.startGame(game.getId());
		} else {
			throw new UnauthorizedException();
		}
	}
	

	@Override
	public Player me() {
		return me;
	}
	

	@Override
	public void shutdown() throws IOException {
		client.shutdown();
	}



	@Override
	public void notifyNewPrisoner(int playerId, int chessPieceType) throws IOException, RequestTimedOutException {
		boolean isWhite = (new Player(playerId)).isWhite();
		ChessPiece toAdd = new ChessPiece.Builder().setType(chessPieceType).setWhite(isWhite).build();
		if (prisoners.containsKey(playerId)) {
			prisoners.get(playerId).add(toAdd);
		} else {
			List<ChessPiece> pieces = new ArrayList<ChessPiece>();
			pieces.add(toAdd);
			prisoners.put(playerId, pieces);
		}
	}

	@Override
	public List<ChessPiece> getPrisoners(int playerId) {
		return prisoners.get(playerId);
	}

	@Override
	public void setBoards() throws IOException,	RequestTimedOutException {
		List<Integer> boardIds = client.getBoards(me.getCurrentGame().getId());
		Map<Integer,ChessBoard> toReturn = new HashMap<Integer,ChessBoard>();
		for (int boardId: boardIds) {
			ChessBoard board = new ChessBoard(boardId);
			if (boardId==me.getCurrentBoardId()) {
				me.setBoard(board);
			}
			toReturn.put(boardId,board);
		}
		System.out.println("Setting currentBoards to: "+toReturn);
		currentBoards= toReturn;
	}


	@Override
	public FrontEnd frontEnd() {
		return this.frontEnd;
	}




	@Override
	public void updateBoard(int boardId, int from_x, int from_y, int to_x,
			int to_y) {
		currentBoards.get(boardId).pieceMoved(from_x, from_y, to_x, to_y);
	}




	@Override
	public Collection<ChessBoard> getCurrentBoards() {
		
		return currentBoards.values();
	}
	
}
