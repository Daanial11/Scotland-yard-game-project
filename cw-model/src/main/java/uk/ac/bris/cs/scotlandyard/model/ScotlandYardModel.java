package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import ch.qos.logback.core.pattern.color.BlackCompositeConverter;
import javafx.scene.shape.MoveTo;
import org.commonmark.node.Visitor;
import uk.ac.bris.cs.gamekit.graph.*;

import uk.ac.bris.cs.gamekit.graph.Graph;

import javax.security.auth.callback.Callback;


// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move> {
	List<Boolean> rounds;
	ImmutableGraph<Integer, Transport> graph;
	ArrayList<Colour> playerColour;
	ArrayList<ScotlandYardPlayer> players;
	int currentPlayer = 0;
	int roundNumber = 0;
	int MrxLastLocation =0;
	Set<Colour> winners = new HashSet<>();
	ArrayList<Spectator> spectatorSet;

	public ScotlandYardModel(List<Boolean> rounds, Graph<Integer, Transport> graph,
			PlayerConfiguration mrX, PlayerConfiguration firstDetective,
			PlayerConfiguration... restOfTheDetectives) {

		requireNonNull(rounds);
		requireNonNull(graph);
		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}
		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty graph");
		}
		if (mrX.colour != BLACK) { // or mr.colour.isDetective()
			throw new IllegalArgumentException("MrX should be Black");
		}

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		for (PlayerConfiguration configuration : restOfTheDetectives){
			configurations.add(requireNonNull(configuration));
		}

		configurations.add(0, firstDetective);
		configurations.add(0, mrX);




		// Checks if there are any duplicate colours or locations
		Set<Integer> set = new HashSet<>();
		Set<Colour> setColour = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set.contains(configuration.location)) {
				throw new IllegalArgumentException("Duplicate location");
			}
			if (setColour.contains(configuration.colour)) {
				throw new IllegalArgumentException("Duplicate colour");
			}
			set.add(configuration.location);
			setColour.add(configuration.colour);
		}
		//Checks if detectives have any secret or double tickets and if all ticket types are present even if they are 0.
		//Note tickets has the type Map<key, value>, using the 'get' method of Map with a given key e.g. double or
		//secret we can retrieve the value for the given key and compare it with 0. So no need for an array list!

		for (PlayerConfiguration configuration : configurations) {
			if (configuration.tickets.keySet().size() != 5){
				throw new IllegalArgumentException("Missing ticket type");
			}
			if (configuration.colour != BLACK) {
				if (configuration.tickets.get(DOUBLE) !=0) {
					throw new IllegalArgumentException("Detective shouldn't have double ticket");
				}
				if (configuration.tickets.get(SECRET) !=0) {
					throw new IllegalArgumentException("Detective shouldn't have secret ticket");
				}
			}
		}
		this.graph = new ImmutableGraph<>(graph);
		this.rounds = rounds;
		this.players = new ArrayList<>();
		for (PlayerConfiguration configuration : configurations) {
			players.add(new ScotlandYardPlayer(configuration.player, configuration.colour, configuration.location, configuration.tickets));
		}
		this.playerColour = new ArrayList<>();
		for (ScotlandYardPlayer player : players) {
			playerColour.add(player.colour());
		}
		this.spectatorSet = new ArrayList<>();

	}


	@Override
	public void registerSpectator(Spectator spectator) {
		Spectator thisSpectator = requireNonNull(spectator);
		if(spectatorSet.contains(thisSpectator)){
			throw new IllegalArgumentException("Already registered");

		} else spectatorSet.add(thisSpectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		Spectator thisSpectator = requireNonNull(spectator);

		if(!spectatorSet.contains(thisSpectator)){
			throw new IllegalArgumentException("Doesn't exist");

		} else spectatorSet.remove(thisSpectator);
	}


	private Player playerIDget(Colour colour){
		Player playerID = players.get(0).player();
		for(ScotlandYardPlayer person: players){
			if(person.colour() == colour){
				return person.player();
			}
		}

		return playerID;
	}
	// Checks if destination is already occupied by another player but returns false if detective wants to move
	// to location occupied by mrX to capture him.

	private Boolean isLocationOccupied(int destination){
		for (ScotlandYardPlayer currentPlayer : players){
			if(currentPlayer.location() == destination && !currentPlayer.isMrX()){
				return true;
			}
		}
		return false;
	}
	// Checks if player has enough tickets of the same type to do a double moving using only that transport
	// e.g. double move using two buses is not possible if player bus tickets=1
	private Boolean enoughTicketsForDouble(Colour playercolour, Transport firstTransport, Transport secondTransport){
		if(firstTransport == secondTransport){
			int amountOfTickets = getPlayerTickets(playercolour, fromTransport(firstTransport)).get();
			if(amountOfTickets>= 2){
				return true;
			} else return false;
		}
		return true;
	}
	private Set<Move> validMove(Colour player){
		ArrayList<Object> Edges = new ArrayList<>(graph.getEdgesFrom(graph.getNode(players.get(currentPlayer).location())));
		Set<Move> validMoves = new HashSet<>();
		ArrayList<Object> EdgesofMove = new ArrayList<>();

		for(Object possibleMove: Edges){
			Transport transportType = (Transport)((Edge)possibleMove).data();
			int destination = (int)((Edge)possibleMove).destination().value();
			if(getPlayerTickets(player, SECRET).get() != 0 && !isLocationOccupied(destination)){
				validMoves.add(new TicketMove(player, SECRET, destination));
			}
			if(getPlayerTickets(player, fromTransport(transportType)).get() != 0 && !isLocationOccupied(destination)) {
				validMoves.add(new TicketMove(player, fromTransport(transportType), destination));
				EdgesofMove.addAll(graph.getEdgesFrom(graph.getNode(destination)));
				if(getPlayerTickets(player, DOUBLE).get() !=0 && rounds.size()>=2){
					for (Object currentEdgeof : EdgesofMove){
						Transport secondTransport = (Transport)(((Edge)currentEdgeof).data());
						int secondDestination = (int)((Edge)currentEdgeof).destination().value();
						if(getPlayerTickets(player, fromTransport(secondTransport)).get() != 0 && !isLocationOccupied(secondDestination) && enoughTicketsForDouble(player, transportType, secondTransport)){
							validMoves.add(new DoubleMove(player, fromTransport(transportType), destination, fromTransport(secondTransport), secondDestination ));
							if(getPlayerTickets(player, SECRET).get() != 0){
								validMoves.add(new DoubleMove(player, SECRET, destination, fromTransport(secondTransport), secondDestination ));
								validMoves.add(new DoubleMove(player, fromTransport(transportType), destination, SECRET, secondDestination ));
								validMoves.add(new DoubleMove(player, SECRET, destination, SECRET, secondDestination ));
							}
						}

					}
				}
			}
			EdgesofMove.clear();
		}

		if(validMoves.isEmpty()){
			validMoves.add(new PassMove(player));
		}

		return validMoves;
	}
	@Override
	public void startRotate() {

		roundCompleted=false;
		if (isGameOver()){
			throw new IllegalStateException("Game is already over, don't start rotation");
		}
		if(checkifRevealRound()){
			MrxLastLocation = players.get(0).location();
		}

		playerIDget(getCurrentPlayer()).makeMove(ScotlandYardModel.this, players.get(currentPlayer).location(),
				validMove(getCurrentPlayer()), this);


	}


	public void accept(Move move){

		Move thisMove = requireNonNull(move);
		if(!validMove(getCurrentPlayer()).contains(thisMove)){
			throw new IllegalArgumentException("thisMove is not a valid move");
		}
		Consumer acceptor = new Consumer();
		thisMove.visit(acceptor);
		if(roundCompleted && !isGameOver()){
			spectatorSet.forEach((n)->n.onRotationComplete(ScotlandYardModel.this));
		}

		//System.out.println(getCurrentPlayer());

		if(!roundCompleted){
			playerIDget(getCurrentPlayer()).makeMove(ScotlandYardModel.this, players.get(currentPlayer).location(),
					validMove(getCurrentPlayer()), this);
		}
		System.out.println(getCurrentPlayer());
		//if(currentPlayer==3) currentPlayer=0;

		}



	Boolean currentMoveDouble = false;
	Colour recentplayer =BLACK;
	Boolean roundCompleted =false;
	class Consumer implements MoveVisitor{
		@Override
		public void visit(PassMove passMove) {

			currentPlayer++;
			if(currentPlayer==players.size()){
				currentPlayer=0;
				roundCompleted=true;
			}
			spectatorSet.forEach((n)->n.onMoveMade(ScotlandYardModel.this, passMove));



		}

		@Override
		public void visit(TicketMove ticketMove) {
			if(roundNumber!=rounds.size() && rounds.get(roundNumber)){
				lastLocation = ticketMove.destination();
			}
			System.out.println(getCurrentPlayer());
			recentplayer =getCurrentPlayer();

			players.get(currentPlayer).location(ticketMove.destination());
			players.get(currentPlayer).removeTicket(ticketMove.ticket());

			if (getCurrentPlayer().isDetective()) {
				players.get(0).addTicket(ticketMove.ticket());
			}

			if(getCurrentPlayer().isDetective()){
				currentPlayer++;
				System.out.println(currentPlayer);
			}
			if(currentPlayer==players.size()){
				currentPlayer=0;
				roundCompleted=true;
			}
			if (getCurrentPlayer().isMrX() && !currentMoveDouble && !roundCompleted) {
				roundNumber++;
				currentPlayer++;
				spectatorSet.forEach((n) -> n.onRoundStarted(ScotlandYardModel.this, roundNumber));

			}

			spectatorSet.forEach((n) -> n.onMoveMade(ScotlandYardModel.this, convertTicketMovetoHidden(ticketMove)));

			if(isGameOver() && roundCompleted){
				spectatorSet.forEach((n)->n.onGameOver(ScotlandYardModel.this, getWinningPlayers()));
			}

		}


		@Override
		public void visit(DoubleMove doubleMove) {
			currentMoveDouble = true;
			players.get(currentPlayer).removeTicket(DOUBLE);
			spectatorSet.forEach((n) -> n.onMoveMade(ScotlandYardModel.this, convert2xMovetoHidden(doubleMove)));
			roundNumber++;
			spectatorSet.forEach((n) -> n.onRoundStarted(ScotlandYardModel.this, roundNumber));
			doubleMove.firstMove().visit(this);
			roundNumber++;
			spectatorSet.forEach((n) -> n.onRoundStarted(ScotlandYardModel.this, roundNumber));
			doubleMove.secondMove().visit(this);
			currentPlayer++;
			currentMoveDouble = false;







		}
	}
	private Boolean checkifRevealRound(){
		if(roundNumber>0 && rounds.get(roundNumber-1)){
			return true;
		}
		return false;
	}
	private Boolean checkifFirstRoundReveal(){
		if(rounds.get(0) && roundNumber==1){
			return true;
		}
		return false;
	}
	int lastLocation =0;
	private DoubleMove convert2xMovetoHidden(DoubleMove move){
		if(rounds.get(roundNumber) &&!rounds.get(roundNumber+1) && !rounds.get(roundNumber+2)){
			lastLocation = move.firstMove().destination();
			return new DoubleMove(getCurrentPlayer(), move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), move.firstMove().destination());
		}

		if(rounds.get(roundNumber) && !rounds.get(roundNumber +1)){
			return new DoubleMove(getCurrentPlayer(), move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), 0);
		}
		else if (!rounds.get(roundNumber) && rounds.get(roundNumber +1)){
			return new DoubleMove(getCurrentPlayer(), move.firstMove().ticket(), 0, move.secondMove().ticket(), move.secondMove().destination());
		}

		else if(!rounds.get(roundNumber) && !rounds.get(roundNumber +1)) return new DoubleMove(getCurrentPlayer(), move.firstMove().ticket(), 0, move.secondMove().ticket(), 0);
		else return new DoubleMove(getCurrentPlayer(), move.firstMove().ticket(),move.firstMove().destination(), move.secondMove().ticket(), move.secondMove().destination());
	}
	private TicketMove convertTicketMovetoHidden(TicketMove move){
		System.out.println(roundNumber);
		System.out.println(recentplayer);

		if((roundNumber>1 &&roundNumber<rounds.size()) && !rounds.get(roundNumber) && !rounds.get(roundNumber-1) && rounds.get(roundNumber-2) &&recentplayer.isMrX()) {
			return new TicketMove(recentplayer, move.ticket(), lastLocation);
		}

		if(((rounds.get(0)&&roundNumber==1) ||(roundNumber>1 && rounds.get(roundNumber-1))) && recentplayer.isMrX()){
			return new TicketMove(recentplayer, move.ticket(), move.destination());
		} else if(recentplayer.isMrX()&& !rounds.get(roundNumber -1)) {
			return new TicketMove(recentplayer, move.ticket(), lastLocation);
		} else return new TicketMove(recentplayer, move.ticket(), move.destination());

	}

	@Override
	public Collection<Spectator> getSpectators() {
		return unmodifiableCollection(spectatorSet);
	}

	@Override
	public List<Colour> getPlayers() {
        return Collections.unmodifiableList(playerColour);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		if(isGameOver() == true) {
			return Collections.unmodifiableSet(winners);
		}
		// TODOd
		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> playerLocation = Optional.empty();
		for (ScotlandYardPlayer person : players){
			if (person.colour() == colour){
				playerLocation = Optional.of(person.location());
				if (roundNumber==0 &&colour.equals(BLACK)){
					playerLocation = Optional.of(0);
				} else if((roundNumber>=1 &&(rounds.get(roundNumber-1) == false) && colour.equals(BLACK))){
					return Optional.of(MrxLastLocation);
				}
			}
		}
		return playerLocation;
		// TODO
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		//go through players, if colour given player equals this.colour return player.ticket(ticket) (which is int).
		Optional<Integer> ticketCount = Optional.empty();
		for(ScotlandYardPlayer person : players){
			if(person.colour() == colour){
				ticketCount = Optional.of(person.tickets().get(ticket));
			}
		}
		return ticketCount;
	}
	private Boolean haveAnyTickets(Colour colour){
		if(getPlayerTickets(colour, BUS).get()==0 && getPlayerTickets(colour, DOUBLE).get()==0 && getPlayerTickets(colour, SECRET).get()==0 && getPlayerTickets(colour, UNDERGROUND).get()==0 && getPlayerTickets(colour, TAXI).get()==0){
			return false;
		}
		return true;
	}

	@Override
	public boolean isGameOver() {
		boolean gameOver = false;
		//int noTickets = 0;
		Set<Move> detectiveTickets = new HashSet<>();
		int posX =0;
		List<Boolean> detectivesHaveAnyTickets = new ArrayList<>();
		for (ScotlandYardPlayer person : players) {
			if(person.colour().isDetective()){
				detectivesHaveAnyTickets.add(haveAnyTickets(person.colour()));
				//noTickets =+ detectiveTickets.size();
			}

			if (person.colour().isMrX()) {
				posX = person.location();
				
				if((validMove(person.colour()).isEmpty()) && (gameOver != true)){
					gameOver = true;
					winners.clear();
					winners.addAll(playerColour);
					winners.remove(BLACK);
					//System.out.println("Line 427" + gameOver);
				}
			}
			if (person.isDetective() && (person.location() == posX) && (gameOver != true)) {
				winners.clear();
				winners.addAll(playerColour);
				winners.remove(BLACK);
				gameOver = true;
				//System.out.println("Line 435" + gameOver);
			}
			if (person.isMrX() && (validMove(person.colour()).isEmpty()) && (gameOver != true)) {
				winners.clear();
				gameOver = true;
				winners.addAll(playerColour);
				//System.out.println("Line 441" + gameOver);
			}
		}
		//System.out.println(detectiveTickets);
		if (roundNumber > rounds.size() - 1 && (gameOver != true)) {
			winners.clear();
			winners.add(BLACK);
			gameOver = true;
			//System.out.println("Line 449" + gameOver);
		}
		if (!detectivesHaveAnyTickets.contains(true)){
			gameOver = true;
			winners.clear();
			winners.add(BLACK);
		}

		//System.out.println("Line 345" + gameOver);
		//System.out.println("Total tickets " + noTickets);
		//System.out.println(winners);
		return gameOver;
		// TODO
	}
	@Override
	public Colour getCurrentPlayer() {

		return playerColour.get(currentPlayer);

		// TODO
	}

	@Override
	public int getCurrentRound() {

		return roundNumber;
		// TODO
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
		// TODO
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return graph;
		// TODO
	}

}
