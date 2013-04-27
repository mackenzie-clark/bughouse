package edu.brown.cs32.bughouse.client;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import edu.brown.cs32.bughouse.exceptions.GameNotReadyException;
import edu.brown.cs32.bughouse.exceptions.RequestTimedOutException;
import edu.brown.cs32.bughouse.exceptions.TeamFullException;
import edu.brown.cs32.bughouse.interfaces.BackEnd;
import edu.brown.cs32.bughouse.interfaces.Client;

public class BughouseClient implements Client {
	private BackEnd backend;
	private ClientSocket socket;
	public BughouseClient(String host, int port, BackEnd backend) throws UnknownHostException, IllegalArgumentException, IOException {
		this.socket = new ClientSocket(host,port,this);
		this.socket.run();
		this.backend = backend;
	}
	@Override
	public int createGame(int userId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("CREATE_GAME:%s\n",userId));
		int gameId = Integer.parseInt(response.trim());
		return gameId;
		
	}

	@Override
	public List<Integer> getGames() throws IOException, RequestTimedOutException {
		
		List<Integer> toReturn = new ArrayList<Integer>();
		String response = socket.getResponse("GET_GAMES:\n");
		if (response.length()==0) return toReturn;
		String[] lines = response.split("\t");

		for (String line: lines) {
			int gameId = Integer.parseInt(line);
			toReturn.add(gameId);
		}
		return toReturn;
	}

	@Override
	public boolean gameIsActive(int gameId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GAME_IS_ACTIVE:%d\n",gameId));
		return (response.trim().equals("true"));
	}

	@Override
	public List<Integer> getPlayers(int gameId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_PLAYERS:%d\n",gameId));
		String[] lines = response.split("\t");
		List<Integer> toReturn = new ArrayList<Integer>();
		for (String line: lines) {
			int userId = Integer.parseInt(line);
			toReturn.add(userId);
		}
		return toReturn;
	}

	@Override
	public int getOwnerId(int gameId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_OWNER:%d\n",gameId));
		int ownerId = Integer.parseInt(response);
		return ownerId;
	}
	@Override
	public int getGame(int playerId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_CURRENT_GAME:%d\n",playerId));
		int gameId = Integer.parseInt(response);
		return gameId;
	}

	@Override
	public List<Integer> getBoards(int gameId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_BOARDS:%d\n",gameId));
		String[] lines = response.split("\t");
		List<Integer> toReturn = new ArrayList<Integer>();
		for (String line: lines) {
			int boardId = Integer.parseInt(line);
			toReturn.add(boardId);
		}
		return toReturn;
	}


	@Override
	public void startGame(int gameId) throws IOException, RequestTimedOutException, GameNotReadyException {
		String response = socket.getResponse(String.format("START_GAME:%d\n",gameId));
		if (response.split(":")[0].trim().equals("NOT_READY")) {
			throw new GameNotReadyException();
		}
		
	}

	@Override
	public int addNewPlayer(String name) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("ADD_PLAYER:%s\n",name));
		int playerId = Integer.parseInt(response.trim());
		return playerId;
	}

	@Override
	public String getName(int playerId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_NAME:%d\n",playerId));
		String name = response.trim();
		return name;
	}

	@Override
	public boolean isWhite(int playerId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("IS_WHITE:%d\n",playerId));
		return (response.trim().equals("true"));
	}

	@Override
	public int getCurrentTeam(int playerId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_TEAM:%d\n",playerId));
		int team = Integer.parseInt(response);
		return team;
	}

	@Override
	public void joinGame(int playerId, int gameId, int team) throws TeamFullException, IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("JOIN_GAME:%d\t%d\t%d\n",playerId,gameId,team));
		if (response.trim().equals("GAME_FULL")) {
			throw new TeamFullException();
		}
	}

	@Override
	public int getBoardId(int playerId) throws IOException, RequestTimedOutException {
		String response = socket.getResponse(String.format("GET_CURRENT_BOARD:%d\n",playerId));
		int boardId = Integer.parseInt(response);
		return boardId;
	}

	@Override
	public void move(int boardId, int from_x, int from_y, int to_x,
			int to_y) throws IOException, RequestTimedOutException {
		socket.getResponse(String.format("MOVE:%d\t%d\t%d\t%d\t%d\n", boardId,from_x,from_y,to_x,to_y));
		
	}
	@Override
	public void quit(int playerId) throws IOException, RequestTimedOutException {
		socket.getResponse(String.format("QUIT:%d\\n", playerId));
	}
	@Override
	public void receive(String message) throws NumberFormatException, IOException, RequestTimedOutException {
		String[] splitted = message.split("\t");
		switch (splitted[1]) {
			case "MOVE":
				broadcastMove(message);
				break;
			case "JOIN_GAME":
				broadcastJoinGame(message);
				break;
			case "QUIT_GAME":
				broadcastQuitGame(message);
				break;
			case "NEW_GAME":
				broadcastNewGame(message);
				break;
			case "GAME_STARTED":
				broadcastGameStarted(message);
				break;
			case "YOUR_TURN":
				backend.notifyTurn();
				break;
			case "ADD_PRISONER":
				addPrisoner(message);
				break;
			default:
				System.out.println("Unknown broadcast message: "+message);
				return;
		}
	}

	private void broadcastGameStarted(String message) {
		String body = message.split(":")[2];
		String[] splitted = body.split("\t");

		System.out.printf("Game #%d has started!\n",splitted[0]);
	}
	private void broadcastNewGame(String message) throws NumberFormatException, IOException, RequestTimedOutException {
		String body = message.split(":")[2];
		String[] splitted = body.split("\t");
		String name = getName(Integer.parseInt(splitted[0]));
		System.out.printf("%s created game #%d\n",name,splitted[1]);
	}
	private void broadcastQuitGame(String message) throws NumberFormatException, IOException, RequestTimedOutException {
		String body = message.split(":")[2];
		String[] splitted = body.split("\t");
		String name = getName(Integer.parseInt(splitted[0]));
		System.out.printf("%s quited game #%d\n",name,splitted[1]);
		
	}
	private void broadcastJoinGame(String message) throws NumberFormatException, IOException, RequestTimedOutException {
		String body = message.split(":")[2];
		String[] splitted = body.split("\t");
		String name = getName(Integer.parseInt(splitted[0]));
		System.out.printf("%s joined game #%d\n",name,splitted[1]);
		
	}
	private void broadcastMove(String message) {
		String body = message.split(":")[2];
		String[] splitted = body.split("\t");
		int boardId = Integer.parseInt(splitted[2]);
		int from_x = Integer.parseInt(splitted[3]);
		int from_y = Integer.parseInt(splitted[4]);
		int to_x = Integer.parseInt(splitted[5]);
		int to_y = Integer.parseInt(splitted[6]);
		//TODO
		//backend.updateBoard(boardId, from_x , from_y, to_x, to_y);
	}
	private void addPrisoner(String message) throws IOException, RequestTimedOutException {
		String body = message.split(":")[2];
		String[] splitted = body.split("\t");
		int playerId = Integer.parseInt(splitted[0]);
		int chessPieceType = Integer.parseInt(splitted[1]);
		backend.notifyNewPrisoner(playerId,chessPieceType);
	}
	
	@Override
	public void shutdown() throws IOException {
		socket.kill();
	}
	@Override
	public void gameOver(int gameId, int team) throws IOException, RequestTimedOutException {
		socket.getResponse(String.format("GAME_OVER:%d\t%d",gameId,team));
	}
	@Override
	public void pass(int fromId, int toId, int chessPieceType) throws IOException, RequestTimedOutException {
		socket.getResponse(String.format("PASS:%d\t%d\t%d", fromId, toId, chessPieceType));
	}
	@Override
	public void put(int chessPieceType, int color, int x, int y) throws IOException, RequestTimedOutException {
		socket.getResponse(String.format("PUT:%d\t%d\t%d\t%d", chessPieceType, color, x,y));
	}

	
}
