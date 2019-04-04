package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLUE;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;

import ch.qos.logback.core.pattern.color.BlackCompositeConverter;
import javafx.scene.shape.MoveTo;
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
	Set<Colour> winners = new HashSet<>();

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

	}


	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}


	int count;
	int i =1;
	@Override
	public void startRotate() {
		count++;
		System.out.println(count);
		//Set<Move> Moves = new HashSet<>((validMove(getCurrentPlayer())));
		Set<Move> validMoves = new HashSet<>(validMove(BLACK));

		player(BLACK).makeMove(ScotlandYardModel.this, getPlayerLocation(BLACK).get(), validMoves, this);
		roundNumber++;
		while(i!=playerColour.size()){
			validMoves = validMove(playerColour.get(i));

			player(playerColour.get(i)).makeMove(ScotlandYardModel.this, getPlayerLocation(playerColour.get(i)).get(), validMoves, this);
			i++;
		}

		if (roundNumber > rounds.size()) {
			isGameOver();
		}
		i =1;
		//if(getCurrentPlayer().isMrX()){
		//	roundNumber++;
		//}
	     Move newMove = new PassMove(getCurrentPlayer());
		 //accept(newMove.visit());
		// TODO
	}



	public void accept(Move move){
		Move thisMove = requireNonNull(move);



	}
	class Consumer implements MoveVisitor{
		@Override
		public void visit(PassMove passMove){
			passMove.visit(this);

		}
		@Override
		public void visit(TicketMove ticketMove){
			ticketMove.visit(this);
		}

		@Override
		public void visit(DoubleMove doubleMove){
			doubleMove.visit(this);
		}
	}
	private Player player(Colour colour){
		Player playerID = players.get(1).player();
		for(ScotlandYardPlayer person: players){
			if(person.colour() == colour){
				return person.player();
			}
		}

		return playerID;
	}

	private Set<Move> validMove(Colour player){
		Set<TicketMove> Moves= new HashSet<>();
		Set<Move> validMoves = new HashSet<>();
		Set<Edge> Edges = new HashSet<>();
		Set<Edge> edgesOf = new HashSet<>();
		Set<DoubleMove> doubleMove = new HashSet<>();
		int x =0;
		Edges.addAll(graph.getEdgesFrom(graph.getNode(getPlayerLocation(player).get())));
		System.out.println(Edges);
		//Moves.add(new PassMove(getCurrentPlayer()));
		for(Edge currentEdge : Edges){
			Transport  currentticket =(Transport) currentEdge.data();
			if(getPlayerTickets(player, fromTransport(currentticket)).get() !=0){
				x =(Integer)currentEdge.destination().value();
				Moves.add(new TicketMove(player, fromTransport(currentticket), x));

			}

			if(getPlayerTickets(player, DOUBLE).get() != 0){
				int endlocation = 0;
				for(TicketMove firstMove : Moves){
					edgesOf.addAll(graph.getEdgesFrom(graph.getNode(x)));
					for (Edge edges : edgesOf){
						Transport secondTicket = (Transport) edges.data();
						endlocation = (Integer) edges.destination().value();
						TicketMove secondaryMove = new TicketMove(BLACK, fromTransport(secondTicket), endlocation);
						doubleMove.add(new DoubleMove(BLACK, firstMove, secondaryMove));
					}
				}
			}

		}

		System.out.println(Moves);
		System.out.println(doubleMove);
		//Moves.add()
		validMoves.addAll(Moves);
		validMoves.addAll(doubleMove);
		return validMoves;
	}


	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
        return Collections.unmodifiableList(playerColour);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		if(isGameOver() == true)
		return Collections.unmodifiableSet(winners);
		// TODOd
		return Collections.unmodifiableSet(winners);
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		Optional<Integer> playerLocation = Optional.empty();
		for (ScotlandYardPlayer person : players){
			if (person.colour() == colour){
				playerLocation = Optional.of(person.location());
				if ((rounds.get(roundNumber) == false) && colour.equals(BLACK))
					playerLocation = Optional.of(person.location());
				return playerLocation;
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

	@Override
	public boolean isGameOver() {
		int posX =0;
		for (ScotlandYardPlayer person : players) {
			if (person.colour() == BLACK)
				posX = person.location();
			if (person.location() == posX && person.isDetective()) {
				winners.addAll(playerColour);
				winners.remove(BLACK);
				return true;
			}
		}
		if (roundNumber > rounds.size()) {
			winners.add(BLACK);
			return true;
		}
		return false;
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
