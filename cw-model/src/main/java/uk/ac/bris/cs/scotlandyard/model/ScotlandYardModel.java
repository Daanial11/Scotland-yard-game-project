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

import java.awt.image.CropImageFilter;
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

	// Adds spectator to spectatorSet
	@Override
	public void registerSpectator(Spectator spectator) {
		// Makes sure spectator parameter isn't NULL
		Spectator thisSpectator = requireNonNull(spectator);
		// Checks that spectator isn't already in the spectatorSet
		if(spectatorSet.contains(thisSpectator)){
			throw new IllegalArgumentException("Already registered");

		} else spectatorSet.add(thisSpectator);
	}


	// Removes Spectator from spectatorSet
	@Override
	public void unregisterSpectator(Spectator spectator) {
		// Makes sure spectator parameter isn't NULL
		Spectator thisSpectator = requireNonNull(spectator);
	// Checks that spectator actually exists in the set
		if(!spectatorSet.contains(thisSpectator)){
			throw new IllegalArgumentException("Doesn't exist");

		} else spectatorSet.remove(thisSpectator);
	}

	// Returns the ID of the current player when passed their colour
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
	// to location occupied by mrX to capture him.11
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


	// Returns set of ticketMoves that the player can do, regarding tickets, locations and other player's location.
	private Set<Move> validMove(Colour player){
		// Array list of the edges around the player's first location (to make set of valid TicketMoves)
		ArrayList<Object> Edges = new ArrayList<>(graph.getEdgesFrom(graph.getNode(players.get(currentPlayer).location())));
		// Final validMove set
		Set<Move> validMoves = new HashSet<>();
		// Array list of the edges around the edges of every initial move (to make a valid set of potential DoubleMoves)
		ArrayList<Object> EdgesofMove = new ArrayList<>();


		for(Object possibleMove: Edges){
			Transport transportType = (Transport)((Edge)possibleMove).data();
			int destination = (int)((Edge)possibleMove).destination().value();
			// Checks player has ticket to make Secret move
			if(getPlayerTickets(player, SECRET).get() != 0 && !isLocationOccupied(destination)){
				validMoves.add(new TicketMove(player, SECRET, destination));
			}
			// Checks player has enough tickets of a transport type and location is not taken to make the move
			if(getPlayerTickets(player, fromTransport(transportType)).get() != 0 && !isLocationOccupied(destination)) {
				validMoves.add(new TicketMove(player, fromTransport(transportType), destination));
				EdgesofMove.addAll(graph.getEdgesFrom(graph.getNode(destination)));
				//Checks is player has valid DoubleMove tickets to do a doubleMove
				if(getPlayerTickets(player, DOUBLE).get() !=0 && rounds.size()>=2){
					for (Object currentEdgeof : EdgesofMove){
						Transport secondTransport = (Transport)(((Edge)currentEdgeof).data());
						int secondDestination = (int)((Edge)currentEdgeof).destination().value();
						// Checks that player has enough tickets to execute DoubleMove and can also land on the final destination (not occupied) and rounds support doubleMove
						if(getPlayerTickets(player, SECRET).get() != 0 && getPlayerTickets(player, fromTransport(secondTransport)).get() != 0 && !isLocationOccupied(secondDestination) && (roundNumber<rounds.size()-2)){
							validMoves.add(new DoubleMove(player, SECRET, destination, fromTransport(secondTransport), secondDestination ));
							validMoves.add(new DoubleMove(player, fromTransport(transportType), destination, SECRET, secondDestination ));
							validMoves.add(new DoubleMove(player, SECRET, destination, SECRET, secondDestination ));
						}
						// DoubleMove checks but without the secret element
						if(getPlayerTickets(player, fromTransport(secondTransport)).get() != 0 && !isLocationOccupied(secondDestination) && enoughTicketsForDouble(player, transportType, secondTransport) && (roundNumber<rounds.size()-2) ){
							validMoves.add(new DoubleMove(player, fromTransport(transportType), destination, fromTransport(secondTransport), secondDestination ));
						}

					}
				}
			}
			// Empties EdgesofMove set to make it clear for next player
			EdgesofMove.clear();
		}
		// Adds Passmove if player is Detective
		if(validMoves.isEmpty() && player.isDetective()){
			validMoves.add(new PassMove(player));
		}

		return validMoves;
	}

	@Override
	public void startRotate() {
        // Checks if round is over
		if (isGameOver()){
			throw new IllegalStateException("Game is already over, don't start rotation");
		}
		roundCompleted=false;
        // Checks is round needs to be revealed
		if(checkifRevealRound()){
			MrxLastLocation = players.get(0).location();
		}
		// Notifies players that they need to make the move, 'Callback' calls the 'accept' method in the consumer class.
		playerIDget(getCurrentPlayer()).makeMove(ScotlandYardModel.this, players.get(currentPlayer).location(),
				validMove(getCurrentPlayer()), this);
	}


	public void accept(Move move){
		// Makes sure move parameter is not NULL
		Move thisMove = requireNonNull(move);
		// Checks if move is in validMove set
		if(!validMove(getCurrentPlayer()).contains(thisMove)){
			throw new IllegalArgumentException("thisMove is not a valid move");
		}
		// Instantiates consumer
		Consumer acceptor = new Consumer();
		// Uses the visit method in the move parameter and passes the acceptor (type: Consumer) which implements moveVisitor
		// Then when given a valid Move type (e.g. double/ ticket/ secret, goes into acceptor to implement move
		// i.e. remove ticket and update location
		thisMove.visit(acceptor);
		// Completes rotation at the end of the round
		if(roundCompleted && !isGameOver()){
			spectatorSet.forEach((n)->n.onRotationComplete(ScotlandYardModel.this));
		}
		// Calls make move again for the next detectives if they haven't won and round is not over (player iterated in
		// consumer class.
		if((!roundCompleted)&& !Dectwinners){
			playerIDget(getCurrentPlayer()).makeMove(ScotlandYardModel.this, players.get(currentPlayer).location(),
					validMove(getCurrentPlayer()), this);
			}
	}

	Boolean currentMoveDouble = false;
	Colour recentplayer =BLACK;
	Boolean roundCompleted =false;
	Boolean Dectwinners =false;
	class Consumer implements MoveVisitor{
		@Override
		public void visit(PassMove passMove) {
			// Implements interactions for when passMove is called
			currentPlayer++;
			if(currentPlayer==players.size()){
				currentPlayer=0;
				roundCompleted=true;
			}
			// Updates spectators with gamestate and move that just happened
			spectatorSet.forEach((n)->n.onMoveMade(ScotlandYardModel.this, passMove));
		}

		@Override
		// Implements interactions for when a ticketMove is used
		public void visit(TicketMove ticketMove) {

			//Checks if game is not over and if it's reveal round for MrX to either reveal location or not.
			if(roundNumber!=rounds.size() && rounds.get(roundNumber)){
				lastLocation = ticketMove.destination();
			}

			if(!currentMoveDouble) {
				recentplayer = getCurrentPlayer();
			}

			// Finds current player and updates location and ticket count when they execute a ticketMove
			for(ScotlandYardPlayer currentperson: players){
				if(currentperson.colour()==recentplayer){
					currentperson.location(ticketMove.destination());
				}

				if(currentperson.colour()==recentplayer && !currentperson.colour().isMrX()){
					currentperson.removeTicket(ticketMove.ticket());

				}

				if(currentperson.isMrX() &&!currentMoveDouble &&recentplayer.isMrX()){
					currentperson.removeTicket(ticketMove.ticket());
				}
			}

			// Adds ticket used by detective to MrX
			if (recentplayer.isDetective()) {
				players.get(0).addTicket(ticketMove.ticket());
			}
			// Iterates detective so next detective will be notified after
			if(recentplayer.isDetective()){
				currentPlayer++;
			}
			// Resets player counter to 0 when all detectives have made their move (back to MrX)
			if(currentPlayer==players.size()){
				currentPlayer=0;
				roundCompleted=true;
			}
			// Not double move and round not finished, iterate round number, move to next player and update start of
			// round for spectators
			if (recentplayer.isMrX() && !currentMoveDouble && !roundCompleted) {
				roundNumber++;
				currentPlayer++;
				spectatorSet.forEach((n) -> n.onRoundStarted(ScotlandYardModel.this, roundNumber));

			}
			// If not doubleMove, update MrX's move, but do NOT reveal
			if(!currentMoveDouble) {
				spectatorSet.forEach((n) -> n.onMoveMade(ScotlandYardModel.this,
						convertTicketMovetoHidden(ticketMove)));
			}
			// Spectator update for when game is over
			if(isGameOver() && winners.contains(BLACK) &&roundCompleted){
				spectatorSet.forEach((n)->n.onGameOver(ScotlandYardModel.this, getWinningPlayers()));
			}
			if(isGameOver() && !winners.contains(BLACK)){
				Dectwinners =true;
				spectatorSet.forEach((n)->n.onGameOver(ScotlandYardModel.this, getWinningPlayers()));
			}

		}

		@Override
		public void visit(DoubleMove doubleMove) {
			currentMoveDouble = true;
			players.get(currentPlayer).removeTicket(DOUBLE);
			// Keeps of who's doing the double
			recentplayer =getCurrentPlayer();

			// OnMoveMade wants next player to update
			currentPlayer++;
			spectatorSet.forEach((n) -> n.onMoveMade(ScotlandYardModel.this, convert2xMovetoHidden(doubleMove)));

			// Round increases before every move in double move
			roundNumber++;
			players.get(0).removeTicket(doubleMove.firstMove().ticket());
			doubleMove.firstMove().visit(this);

			// Updates MrX's last location depending on whether it's a hidden round or not
			MrxLastLocation= convertTicketMovetoHidden(doubleMove.firstMove()).destination();

			// Spectator update on first move that occurred (round interated after first move and move made)
			spectatorSet.forEach((n) -> n.onRoundStarted(ScotlandYardModel.this, roundNumber));
			spectatorSet.forEach((n) -> n.onMoveMade(ScotlandYardModel.this,
					convertTicketMovetoHidden(doubleMove.firstMove())));

			// Checks first move is done
			firstMoveDone=true;
			roundNumber++;
			players.get(0).removeTicket(doubleMove.secondMove().ticket());
			doubleMove.secondMove().visit(this);
			spectatorSet.forEach((n) -> n.onRoundStarted(ScotlandYardModel.this, roundNumber));

			// After secondMove done, updated location is hidden because Spectator only receives hiddenMove
			spectatorSet.forEach((n) -> n.onMoveMade(ScotlandYardModel.this,
					convertTicketMovetoHidden(doubleMove.secondMove())));
			secondMoveDone=true;

			// Resets state of the consumer
			currentMoveDouble = false;
			firstMoveDone=false;
			secondMoveDone=false;
			doubleMoveCount++;
		}
	}


	// Check if boolean for this round's index is true
	private Boolean checkifRevealRound(){
		if(roundNumber>0 && rounds.get(roundNumber-1)){
			return true;
		}
		return false;
	}

	int lastLocation =0;
	int firstMove =0;
	int secondMove =0;
	Boolean firstMoveDone =false;
	Boolean secondMoveDone =false;
	int doubleMoveCount = 0;

	// Goes through cases where the doubleMove's destinations must be revealed or not
	private DoubleMove convert2xMovetoHidden(DoubleMove move){
		if(rounds.get(roundNumber) &&!rounds.get(roundNumber+1) && !rounds.get(roundNumber+2)){
			lastLocation = move.firstMove().destination();
			firstMove=move.firstMove().destination();
			secondMove=move.firstMove().destination();
			return new DoubleMove(BLACK, move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), move.firstMove().destination());
		}

		if(rounds.get(roundNumber) && !rounds.get(roundNumber +1)){
			firstMove=move.firstMove().destination();
			secondMove=0;
			return new DoubleMove(BLACK, move.firstMove().ticket(), move.firstMove().destination(), move.secondMove().ticket(), 0);
		}
		else if (!rounds.get(roundNumber) && rounds.get(roundNumber +1)){
			firstMove=0;
			secondMove=move.secondMove().destination();
			return new DoubleMove(BLACK, move.firstMove().ticket(), 0, move.secondMove().ticket(), move.secondMove().destination());
		}

		else if(!rounds.get(roundNumber) && !rounds.get(roundNumber +1)){
			firstMove=MrxLastLocation;
			secondMove=MrxLastLocation;
			return new DoubleMove(BLACK, move.firstMove().ticket(),firstMove, move.secondMove().ticket(), secondMove);
		}
		else{
			firstMove=move.firstMove().destination();
			secondMove=move.secondMove().destination();
			return new DoubleMove(BLACK, move.firstMove().ticket(),move.firstMove().destination(), move.secondMove().ticket(), move.secondMove().destination());
		}
	}
	// Goes through cases where destinations of moves should be revealed or not
	private TicketMove convertTicketMovetoHidden(TicketMove move){

		if(recentplayer.isDetective()){
			return new TicketMove(recentplayer, move.ticket(), move.destination());
		}
		if(currentMoveDouble && !firstMoveDone){
			return new TicketMove(recentplayer, move.ticket(), firstMove);
		}
		if(currentMoveDouble && !secondMoveDone){
			return new TicketMove(recentplayer, move.ticket(), secondMove);
		}
		else return new TicketMove(recentplayer, move.ticket(), getPlayerLocation(BLACK).get());


	}

	// Get set of spectators
	@Override
	public Collection<Spectator> getSpectators() {
		return unmodifiableCollection(spectatorSet);
	}
	// Get list of colours (used for iterating through player rotations
	@Override
	public List<Colour> getPlayers() {
        return Collections.unmodifiableList(playerColour);
	}
	// Creates set of winning players
	@Override
	public Set<Colour> getWinningPlayers() {
		if(isGameOver() == true) {
			return Collections.unmodifiableSet(winners);
		}
		return Collections.unmodifiableSet(winners);
	}
	// Returns player location number, returns 0 if round is not a reveal round
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
	}
	// Gets ticket map for a given player
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
	// Boolean to see if every detective has run out of tickets, used for isGameOver()
	private Boolean haveAnyTickets(Colour colour){
		if(getPlayerTickets(colour, BUS).get()==0 && getPlayerTickets(colour, DOUBLE).get()==0 && getPlayerTickets(colour, SECRET).get()==0 && getPlayerTickets(colour, UNDERGROUND).get()==0 && getPlayerTickets(colour, TAXI).get()==0){
			return false;
		}
		return true;
	}

	// Endgame logic
	@Override
	public boolean isGameOver() {
		boolean gameOver = false;
		int posX =players.get(0).location();
		// Boolean list to see which detectives has tickets
		List<Boolean> detectivesHaveAnyTickets = new ArrayList<>();
		// ArrayList to see if MrX is stuck
		ArrayList<Object> EdgesMrx = new ArrayList<>(getGraph().getEdgesFrom(getGraph().getNode(players.get(0).location())));
		// ArrayList to see if MrX is stuck even with doubleMoves
		ArrayList<Object> toRemove = new ArrayList<>();
		Colour currentPlayer = getCurrentPlayer();
		for (ScotlandYardPlayer person : players) {
			if(person.colour().isDetective()){
				int CurrentDetectivelocation = person.location();
				// Adds true/false values if detectives have tickets to set
				detectivesHaveAnyTickets.add(haveAnyTickets(person.colour()));
				for(Object CurrentEdge: EdgesMrx){
					// Adds edges to MrX's location and removes them if they're occupied
					int destination = (int)((Edge)CurrentEdge).destination().value();
					if(CurrentDetectivelocation == destination){
						toRemove.add(CurrentEdge);
					}
				}
			}
			// GameOver if detective lands on MrX. D wins
			if (person.isDetective() && (person.location() == posX) && (gameOver != true)) {
				winners.clear();
				winners.addAll(playerColour);
				winners.remove(BLACK);
				gameOver = true;

			}
		}
		// Game over if MrX cannot move (stuck). D wins
		if(validMove(currentPlayer).isEmpty()){
			gameOver = true;
			winners.clear();
			winners.addAll(playerColour);
			winners.remove(BLACK);
		}
		//Game over if MrX is cornered. D wins
		EdgesMrx.removeAll(toRemove);
		if(EdgesMrx.isEmpty()){
			winners.clear();
			winners.addAll(playerColour);
			winners.remove(BLACK);
			gameOver =true;
		}

		//Game over if rounds are finished. X wins
		if (roundCompleted&&roundNumber > rounds.size()-1  && (gameOver != true)) {
			winners.clear();
			winners.add(BLACK);
			gameOver = true;

		}
		// Game over if detectives cannot move anymore. X wins
		if (!detectivesHaveAnyTickets.contains(true)){
			gameOver = true;
			winners.clear();
			winners.add(BLACK);
		}
		return gameOver;
	}

	@Override
	public Colour getCurrentPlayer() {
		return playerColour.get(currentPlayer);
	}

	@Override
	public int getCurrentRound() {
		return roundNumber;
	}

	@Override
	// Get immutable list for rounds
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return graph;
	}

}
